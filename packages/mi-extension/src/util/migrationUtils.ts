/**
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// VS Code adapter layer — delegates all migration logic to @wso2/mi-migration-core
// and handles VS Code-specific concerns (UI, workspace management, resource paths).

import * as fs from 'fs';
import * as path from 'path';
import { ImportProjectRequest, ImportProjectResponse } from '@wso2/mi-core';
import { commands, Uri, window, workspace } from 'vscode';
import { extension } from '../MIExtensionContext';
import {
    importProjects as coreImportProjects,
    MigrationHost,
    MigrationConfigKey,
    LATEST_MI_VERSION,
    getProjectDetails,
    containsMultiModuleNatureInProjectFile,
    containsMultiModuleNatureInPomFile,
    findMultiModuleProjectsInWorkspaceDir,
} from '@wso2/mi-migration-core';
import { consolidatedProjectPomContent } from './templates';
import { updatePomWithParent } from './fileOperations';

// Re-export utility functions used by other extension modules
export { containsMultiModuleNatureInProjectFile, containsMultiModuleNatureInPomFile, findMultiModuleProjectsInWorkspaceDir };

// ---------------------------------------------------------------------------
// Logging
// ---------------------------------------------------------------------------

const BACKUP_DIR = '.backup';
const MIGRATION_LOG_FILE = 'migration.log';

let migrationLogFilePath: string | undefined;
const logQueue: string[] = [];
let isFlushing = false;

function setMigrationLogFilePath(projectRoot: string) {
    migrationLogFilePath = path.join(projectRoot, BACKUP_DIR, MIGRATION_LOG_FILE);
}

async function flushLogs() {
    if (isFlushing || logQueue.length === 0 || !migrationLogFilePath) return;
    isFlushing = true;
    try {
        const logsToWrite = logQueue.splice(0, logQueue.length).join('\n') + '\n';
        await fs.promises.appendFile(migrationLogFilePath, logsToWrite, 'utf-8');
    } catch (error) {
        console.error(`Failed to write migration log to ${migrationLogFilePath}:`, error);
    } finally {
        isFlushing = false;
        if (logQueue.length > 0) flushLogs();
    }
}

function appendLog(level: 'log' | 'warn' | 'error', args: unknown[]) {
    if (!migrationLogFilePath) return;
    const timestamp = new Date().toISOString();
    const line = `[${timestamp}] [${level.toUpperCase()}] ${args.map(a => a instanceof Error ? (a.stack || a.message) : typeof a === 'string' ? a : JSON.stringify(a)).join(' ')}`;
    try {
        const dir = path.dirname(migrationLogFilePath);
        if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
        logQueue.push(line);
        flushLogs();
    } catch (error) {
        console.error('Failed to queue migration log:', error);
    }
}

function logInfo(...args: unknown[]) { console.log(...args); appendLog('log', args); }
function logError(...args: unknown[]) { console.error(...args); appendLog('error', args); }

// ---------------------------------------------------------------------------
// VS Code MigrationHost implementation
// ---------------------------------------------------------------------------

class VsCodeMigrationHost implements MigrationHost {
    notify(level: 'info' | 'warn' | 'error', message: string): void {
        if (level === 'error') void window.showErrorMessage(message);
        else if (level === 'warn') void window.showWarningMessage(message);
        else void window.showInformationMessage(message);
    }

    getResourcePath(relative: string): string {
        return extension.context.asAbsolutePath(path.join('resources', relative));
    }

    getConfig(key: MigrationConfigKey): boolean {
        const config = workspace.getConfiguration('MI');
        switch (key) {
            case 'useLocalMaven': return config.get<boolean>('useLocalMaven') ?? false;
            case 'useDefaultMavenForMigration': return config.get<boolean>('useDefaultMavenForMigration') ?? false;
            case 'convertToConsolidated': return config.get<boolean>('convertToConsolidatedProjectAfterMigration') ?? false;
            case 'runAnalysis': return false;
        }
    }

    getTargetVersion(sourceVersion?: string): string {
        return sourceVersion ?? LATEST_MI_VERSION;
    }

    async confirmProceed(message: string): Promise<boolean> {
        const choice = await window.showWarningMessage(message, { modal: true }, 'Proceed', 'Cancel');
        return choice === 'Proceed';
    }
}

// ---------------------------------------------------------------------------
// Public entry points
// ---------------------------------------------------------------------------

export async function importProject(params: ImportProjectRequest): Promise<ImportProjectResponse[]> {
    const { source } = params;

    const projectUri = workspace.getWorkspaceFolder(Uri.file(source))?.uri?.fsPath;
    if (!projectUri) {
        void window.showErrorMessage('Please select a valid project directory');
        throw new Error('Invalid project directory');
    }

    setMigrationLogFilePath(source);
    const host = new VsCodeMigrationHost();
    const result = await coreImportProjects([{ source, directory: source }], host);
    commands.executeCommand('setContext', 'MI.migrationStatus', 'done');
    await flushLogs();
    return result.filePaths.map(fp => ({ filePath: fp }));
}

export async function importProjects(params: ImportProjectRequest[]): Promise<ImportProjectResponse[]> {
    const responses: ImportProjectResponse[] = [];
    const allCreatedUris: Uri[] = [];
    logInfo(`Starting migration for ${params.length} project(s).`);

    for (const param of params) {
        try {
            const created = await importProject(param);
            for (const r of created) {
                responses.push(r);
                if (r.filePath) allCreatedUris.push(Uri.file(r.filePath));
            }
        } catch (err) {
            logError(`Migration failed for ${param.source}:`, err);
            responses.push({ filePath: '' });
        }
    }

    const closeWindow = await handleWorkspaceAfterMigration(params[0].directory, allCreatedUris);
    await closeWindowIfRequired(closeWindow);
    logInfo('All migrations finished.');
    await flushLogs();
    return responses;
}

// ---------------------------------------------------------------------------
// Workspace management (VS Code-specific)
// ---------------------------------------------------------------------------

async function handleWorkspaceAfterMigration(projectUri: string, createdFolderUris: Uri[]): Promise<boolean> {
    if (createdFolderUris.length <= 0) return false;
    logInfo('Updating workspace after migration.');
    const createdProjectCount = createdFolderUris.length;
    logInfo(`Handling workspace after migration. projectUri=${projectUri}, createdProjectCount=${createdProjectCount}, workspaceFolderCount=${workspace.workspaceFolders?.length ?? 0}`);
    if (!workspace.workspaceFolders || workspace.workspaceFolders.length <= 1) {
        if (createdProjectCount === 1) {
            logInfo(`Opening single created project in new window. uri=${createdFolderUris[0].fsPath}`);
            await commands.executeCommand('vscode.openFolder', createdFolderUris[0], true);
            return true;
        } else {
            await commands.executeCommand('workbench.action.closeActiveEditor');
            await updateWorkspaceWithNewFolders(projectUri, createdFolderUris);
        }
    } else {
        await commands.executeCommand('workbench.action.closeActiveEditor');
        await updateWorkspaceWithNewFolders(projectUri, createdFolderUris);
    }
    return false;
}

async function closeWindowIfRequired(shouldCloseWindow: boolean): Promise<void> {
    if (shouldCloseWindow) {
        await commands.executeCommand('workbench.action.closeWindow');
    }
}

async function updateWorkspaceWithNewFolders(projectUri: string, createdFolderUris: Uri[]) {
    const workspaceFolders = workspace.workspaceFolders || [];
    const urisToAdd = createdFolderUris.filter(
        folderUri => !workspaceFolders.some(folder => folder.uri.fsPath === folderUri.fsPath)
    );
    logInfo(`Updating workspace. existingCount=${workspaceFolders.length}, toAddCount=${urisToAdd.length}`);

    if (urisToAdd.length > 0) {
        const currentProjectIndex = workspaceFolders.findIndex(folder => folder.uri.fsPath === projectUri);
        const foldersToAdd = urisToAdd.map(uri => ({ uri }));
        const foldersToRemove = currentProjectIndex !== -1 ? [currentProjectIndex] : [];

        const config = workspace.getConfiguration('MI', Uri.file(projectUri));
        const convertToConsolidated = config.get('convertToConsolidatedProjectAfterMigration');

        if (convertToConsolidated) {
            const runtimeVersion = getProjectDetails(foldersToAdd[0].uri.fsPath).runtimeVersion;
            fs.writeFileSync(
                path.join(path.dirname(foldersToAdd[0].uri.fsPath), 'pom.xml'),
                consolidatedProjectPomContent(
                    'consolidated-project',
                    'com.microintegrator.projects',
                    'consolidated-project',
                    '1.0.0',
                    runtimeVersion ?? LATEST_MI_VERSION,
                    foldersToAdd.map(folder => path.basename(folder.uri.fsPath))
                )
            );
            for (const folder of foldersToAdd) {
                const pomPath = path.join(folder.uri.fsPath, 'pom.xml');
                updatePomWithParent(pomPath, { groupId: 'com.microintegrator.projects', artifactId: 'consolidated-project', version: '1.0.0' });
            }
        }

        workspace.updateWorkspaceFolders(
            foldersToRemove[0] ?? workspaceFolders.length,
            foldersToRemove.length,
            ...foldersToAdd
        );
        logInfo(`Workspace folders updated. removedCount=${foldersToRemove.length}, addedCount=${foldersToAdd.length}`);
    } else {
        logInfo('No workspace folder update needed. All target folders already exist in workspace.');
    }
}
