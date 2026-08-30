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

import * as vscode from 'vscode';
import { extension } from './MIExtensionContext';
import { activate as activateHistory } from './history';
import { activateVisualizer } from './visualizer/activate';
import { activateAiPanel } from './ai-features/activate';

import { activateDebugger } from './debugger/activate';
import { activateMigrationSupport } from './migration';
import { activateRuntimeService } from './runtime-services-panel/activate';
import { MILanguageClient } from './lang-client/activator';
import { activateUriHandlers } from './uri-handler';
import { extensions, workspace } from 'vscode';
import { StateMachineAI } from './ai-features/aiMachine';
import { isOldProjectOrWorkspace, getStateMachine } from './stateMachine';
import { MACHINE_VIEW, onWorkspaceFoldersChanged } from '@wso2/mi-core';
import { webviews, VisualizerWebview } from './visualizer/webview';
import { RPCLayer } from './RPCLayer';
import { v4 as uuidv4 } from 'uuid';
import path from 'path';
import { COMMANDS, WI_EXTENSION_ID } from './constants';
import { enableLS, shouldShowWorkspaceOverview } from './util/workspace';
import { disposeMIAgentPanelRpcManager } from './rpc-managers/agent-mode/rpc-handler';
import { isConsolidatedProject } from './util/onboardingUtils';
import { readConsolidatedProjectDetails } from './util/consolidatedPomUtils';
import { getModules, parseConsolidatedProjectPom } from './debugger/pomResolver';
const os = require('os');
const fs = require('fs');
const crypto = require('crypto');

export async function activate(context: vscode.ExtensionContext) {
	extension.context = context;

	// TODO: Remove when VSCode fixes: https://github.com/microsoft/vscode/issues/188257
	const orphanedTabs = vscode.window.tabGroups.all
		.flatMap((tabGroup) => tabGroup.tabs)
		.filter((tab) => (tab.input as any)?.viewType?.includes("micro-integrator."));
	vscode.window.tabGroups.close(orphanedTabs);

	if (workspace.workspaceFolders) {
		// Reopening as a consolidated workspace reloads the window
		const reopening = await openConsolidatedAsWorkspace(context);
		if (reopening) {
			return;
		}
	}

	const oldProjects = workspace.workspaceFolders
		? (await Promise.all(
			workspace.workspaceFolders.map(async folder => {
				const isOld = await isOldProjectOrWorkspace(folder.uri.fsPath);
				if (isOld) getStateMachine(folder.uri.fsPath);
				return isOld ? folder : null;
			})
		)).filter((folder): folder is vscode.WorkspaceFolder => folder !== null)
		: [];
	const newProjects = workspace.workspaceFolders
		? workspace.workspaceFolders.filter(folder => !oldProjects.includes(folder))
		: [];

	const firstProject = newProjects?.[0]?.uri?.fsPath || 
						 oldProjects?.[0]?.uri?.fsPath || 
						 path.join(os.tmpdir(), uuidv4());
	
	const updateMultiProjectContext = () => {
		const count = workspace.workspaceFolders?.length ?? 0;
		vscode.commands.executeCommand('setContext', 'MI.hasMultipleProjects', count > 1);
	};

	// One panel only: the language server registers a ProjectContext for every workspace
	// folder from `initialize`/`didChangeWorkspaceFolders`, so a state machine per project
	// isn't needed to make the other folders available.
	if (!oldProjects.length) {
		const showWorkspaceOverview = shouldShowWorkspaceOverview();
		getStateMachine(firstProject, showWorkspaceOverview ? { view: MACHINE_VIEW.WorkspaceOverview } : undefined);
	}
	updateMultiProjectContext();

	workspace.onDidChangeWorkspaceFolders(async (event) => {
		if (event.added.length > 0) {
			// If several folders are added at once, this avoids opening one panel per folder.
			const showWorkspaceOverview = shouldShowWorkspaceOverview();
			getStateMachine(event.added[0].uri.fsPath, showWorkspaceOverview ? { view: MACHINE_VIEW.WorkspaceOverview } : undefined);
		}
		if (event.removed.length > 0) {
			for (const removedProject of event.removed) {
				disposeMIAgentPanelRpcManager(removedProject.uri.fsPath);
				const webview = webviews.get(removedProject.uri.fsPath);
				if (webview) {
					webview.dispose();
				}
			}
		}
		updateMultiProjectContext();
		// refresh project explorer
		vscode.commands.executeCommand(COMMANDS.REFRESH_COMMAND);
		// notify any open Workspace Overview webview to refresh its project list
		for (const projectUri of webviews.keys()) {
			RPCLayer._messengers.get(projectUri)?.sendNotification(
				onWorkspaceFoldersChanged,
				{ type: 'webview', webviewType: VisualizerWebview.viewType }
			);
		}
	});
	StateMachineAI.initialize();

	activateUriHandlers();
	activateHistory();

	activateDebugger(context);
	activateMigrationSupport(context);
	activateRuntimeService(context, firstProject);
	activateVisualizer(context, firstProject);
	activateAiPanel(context);

	// enableLS() is workspace-wide (one shared language client), not per-folder,
	// so it's registered once regardless of how many MI folders are open.
	context.subscriptions.push(...enableLS());
}

export async function deactivate(): Promise<void> {
	await MILanguageClient.stopSharedInstance();

	// close all webviews
	const allWebviews = Array.from(webviews.values());
	for (let i = 0; i < allWebviews.length; i++) {
		const webview = allWebviews[i];
		if (webview) {
			webview.dispose();
		}
	}
}

export function checkForWso2IntegratorExt() {
	const wso2PlatformExtension = extensions.getExtension(WI_EXTENSION_ID);
	if (!wso2PlatformExtension) {
		vscode.window.showErrorMessage('The WSO2 Integrator extension is not installed. Please install it to proceed.', "Install WSO2 Integrator").then(selection => {
			if (selection === "Install WSO2 Integrator") {
				vscode.commands.executeCommand(COMMANDS.INSTALL_EXTENSION_COMMAND, WI_EXTENSION_ID).then(() => {
					vscode.window.showInformationMessage('WSO2 Integrator extension installed. Please reload VSCode to complete the extension activation.', "Reload Window").then(reloadSelection => {
						if (reloadSelection === "Reload Window") {
							vscode.commands.executeCommand(COMMANDS.RELOAD_WINDOW);
						}
					});
				});
			}
		});
		return false;
	}
	return true;
}

/**
 * Discover the sub-project folders of a consolidated project root.
 */
async function getSubProjectUris(folderPath: string): Promise<vscode.Uri[]> {
	let declaredModules: string[];
	try {
		const pom = parseConsolidatedProjectPom(path.join(folderPath, 'pom.xml'));
		declaredModules = getModules(pom.project);
	} catch (err) {
		console.error('Could not read modules from consolidated project pom.xml', err);
		return [];
	}

	const entries = await fs.promises.readdir(folderPath, { withFileTypes: true });
	const subUris: vscode.Uri[] = [];

	for (const entry of entries) {
		const subPath = path.join(folderPath, entry.name);
		if (!entry.isDirectory() || entry.name.startsWith('.') || fs.existsSync(path.join(subPath, '.docker-build'))) {
			continue;
		}
		if (declaredModules.includes(entry.name) && fs.existsSync(path.join(subPath, 'pom.xml'))) {
			subUris.push(vscode.Uri.file(subPath));
		}
	}

	return subUris;
}

/**
 * Generates a named .code-workspace for a consolidated project.
 * Returns true if the window is reopening into it.
 */
async function openConsolidatedAsWorkspace(context: vscode.ExtensionContext): Promise<boolean> {
	try {
		// Already a saved workspace file — nothing to do. Untitled workspaces get converted.
		if (workspace.workspaceFile && workspace.workspaceFile.scheme !== 'untitled') {
			return false;
		}

		const folders = workspace.workspaceFolders;
		if (!folders || folders.length === 0) {
			return false;
		}

		// Build the target folder set and collect the consolidated root(s).
		const consolidatedRoots = new Set<string>();
		const folderPaths: string[] = [];
		for (const folder of folders) {
			const folderPath = folder.uri.fsPath;
			if (isConsolidatedProject(folderPath)) {
				// Consolidated root opened directly: expand into its sub-projects.
				consolidatedRoots.add(folderPath);
				const subUris = await getSubProjectUris(folderPath);
				folderPaths.push(...subUris.map(uri => uri.fsPath));
			} else {
				folderPaths.push(folderPath);
				// A restored untitled workspace lists the sub-projects rather than
				// the consolidated root — detect it via the parent directory.
				const parent = path.dirname(folderPath);
				if (isConsolidatedProject(parent)) {
					consolidatedRoots.add(parent);
				}
			}
		}

		if (consolidatedRoots.size === 0 || folderPaths.length === 0) {
			return false;
		}

		// Name the workspace after the first consolidated project.
		const primaryRoot = [...consolidatedRoots][0];
		const details = await readConsolidatedProjectDetails(primaryRoot);
		const rawName = details?.artifactId?.trim() || path.basename(primaryRoot);
		// Keep the file name filesystem-safe; it becomes the Explorer label.
		const workspaceName = rawName.replace(/[<>:"/\\|?*]/g, '_') || 'consolidated-project';

		const workspaceFileUri = await writeConsolidatedWorkspaceFile(context, primaryRoot, workspaceName, folderPaths);

		// Workspace-target update is window-scoped: suppresses the save prompt only for
		// the untitled workspace being discarded, not globally or in the new file.
		if (workspace.workspaceFile?.scheme === 'untitled') {
			try {
				await workspace.getConfiguration().update(
					'window.confirmSaveUntitledWorkspace',
					false,
					vscode.ConfigurationTarget.Workspace
				);
			} catch (err) {
				// Fall back to VSCode's default (prompt) if the override fails.
				console.error('Could not suppress untitled workspace save prompt', err);
			}
		}

		await vscode.commands.executeCommand('vscode.openFolder', workspaceFileUri, false);
		return true;
	} catch (err) {
		console.error('Error opening consolidated project as workspace', err);
		return false;
	}
}

/**
 * Writes the .code-workspace file under a per-project hashed sub-directory,
 * so its base name can stay "<name>.code-workspace".
 */
async function writeConsolidatedWorkspaceFile(
	context: vscode.ExtensionContext,
	consolidatedRoot: string,
	workspaceName: string,
	folderPaths: string[]
): Promise<vscode.Uri> {
	const projectHash = crypto.createHash('md5').update(consolidatedRoot).digest('hex').slice(0, 8);
	const dir = path.join(context.globalStorageUri.fsPath, 'consolidated-workspaces', projectHash);
	await fs.promises.mkdir(dir, { recursive: true });

	const workspaceFilePath = path.join(dir, `${workspaceName}.code-workspace`);
	// Preserve settings/extensions a user may have added to a previously generated
	// workspace file — only the folder list is regenerated.
	let existing: Record<string, unknown> = {};
	try {
		existing = JSON.parse(await fs.promises.readFile(workspaceFilePath, 'utf-8'));
	} catch {
		// No existing file, or it's unreadable/invalid — start fresh.
	}

	const content = {
		...existing,
		folders: folderPaths.map(folderPath => ({ path: folderPath })),
		settings: existing.settings ?? {}
	};
	await fs.promises.writeFile(workspaceFilePath, JSON.stringify(content, null, 2), 'utf-8');

	return vscode.Uri.file(workspaceFilePath);
}