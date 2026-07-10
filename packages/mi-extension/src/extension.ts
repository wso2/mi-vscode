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
import { MACHINE_VIEW } from '@wso2/mi-core';
import { webviews } from './visualizer/webview';
import { v4 as uuidv4 } from 'uuid';
import path from 'path';
import { COMMANDS, WI_EXTENSION_ID } from './constants';
import { enableLS } from './util/workspace';
import { disposeMIAgentPanelRpcManager } from './rpc-managers/agent-mode/rpc-handler';
import { isConsolidatedProject } from './util/onboardingUtils';
import { readConsolidatedProjectDetails } from './util/consolidatedPomUtils';
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
		// A consolidated project is opened as a generated .code-workspace file so
		// the Explorer shows the consolidated project's name instead of
		// "Untitled (Workspace)". Opening the workspace file reloads the window,
		// so stop activating this (soon-to-be-disposed) instance if we do.
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

	if (!oldProjects.length) {
		const wsFolders = workspace.workspaceFolders;
		const hasMultipleProjects = (wsFolders?.length ?? 0) > 1;
		// Land on the Workspace Overview page for any workspace on open and on
		// every window reload. This covers a multi-root workspace, a saved or
		// generated .code-workspace, and a consolidated project (which opens as a
		// workspace of its sub-projects, whose shared parent is the consolidated
		// root) — a workspace always has an overview page.
		const isWorkspace = !!workspace.workspaceFile
			|| (!!wsFolders?.length && isConsolidatedProject(path.dirname(wsFolders[0].uri.fsPath)));
		const showWorkspaceOverview = hasMultipleProjects || isWorkspace;
		getStateMachine(firstProject, showWorkspaceOverview ? { view: MACHINE_VIEW.WorkspaceOverview } : undefined);
	}
	updateMultiProjectContext();

	workspace.onDidChangeWorkspaceFolders(async (event) => {
		if (event.added.length > 0) {
			for (const addedProject of event.added) {
				getStateMachine(addedProject.uri.fsPath);
			}
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
	});
	StateMachineAI.initialize();

	activateUriHandlers();
	activateHistory();

	activateDebugger(context);
	activateMigrationSupport(context);
	activateRuntimeService(context, firstProject);
	activateVisualizer(context, firstProject);
	activateAiPanel(context);

	workspace.workspaceFolders?.forEach(folder => {
		context.subscriptions.push(...enableLS());
	});
}

export async function deactivate(): Promise<void> {
	const clients = await MILanguageClient.getAllInstances();
	clients.forEach(async client => {
		await client?.stop();
	});

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
 * Discover the sub-project folders (directories containing a pom.xml) of a
 * consolidated project root.
 */
async function getSubProjectUris(folderPath: string): Promise<vscode.Uri[]> {
	const entries = await fs.promises.readdir(folderPath, { withFileTypes: true });
	const subUris: vscode.Uri[] = [];

	for (const entry of entries) {
		const subPath = path.join(folderPath, entry.name);
		if (!entry.isDirectory() || entry.name.startsWith('.') || fs.existsSync(path.join(subPath, '.docker-build'))) {
			continue;
		}
		if (fs.existsSync(path.join(subPath, 'pom.xml'))) {
			subUris.push(vscode.Uri.file(subPath));
		}
	}

	return subUris;
}

/**
 * When a consolidated project folder is opened directly, generate a
 * .code-workspace file (named after the consolidated project) that lists its
 * sub-projects and open it. VSCode has no API to name an untitled multi-root
 * workspace, so a workspace file is the only way to show the project's name in
 * the Explorer. Returns true if the window is being reopened into the workspace
 * file (activation should stop in that case).
 */
async function openConsolidatedAsWorkspace(context: vscode.ExtensionContext): Promise<boolean> {
	try {
		// If we're already in a *saved* workspace file, its folders come from the
		// file — nothing to do. Untitled workspaces (scheme "untitled:") are still
		// converted below so the project shows its name; VSCode shows a one-time
		// "save workspace?" prompt when leaving an untitled workspace (unavoidable
		// via the API), after which the project is a saved file and never prompts.
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

		// Name the workspace (and hence the Explorer label) after the first
		// consolidated project.
		const primaryRoot = [...consolidatedRoots][0];
		const details = await readConsolidatedProjectDetails(primaryRoot);
		const rawName = details?.artifactId?.trim() || path.basename(primaryRoot);
		// Keep the file name filesystem-safe; it becomes the Explorer label.
		const workspaceName = rawName.replace(/[<>:"/\\|?*]/g, '_') || 'consolidated-project';

		const workspaceFileUri = await writeConsolidatedWorkspaceFile(context, primaryRoot, workspaceName, folderPaths);

		// Migrating an existing untitled workspace: suppress VSCode's
		// "save workspace?" prompt for THIS window only. `window.confirmSaveUntitledWorkspace`
		// is window-scoped, so writing it at Workspace target applies just to the
		// untitled workspace we're about to discard — it doesn't change the user's
		// global preference or leak into the generated workspace file.
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
 * Write the generated .code-workspace file into the extension's global storage.
 * The file lives in a per-project sub-directory (keyed by a hash of the
 * consolidated root) so the file name can stay exactly "<name>.code-workspace"
 * — VSCode derives the Explorer label from that base name.
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
	const content = {
		folders: folderPaths.map(folderPath => ({ path: folderPath })),
		settings: {}
	};
	await fs.promises.writeFile(workspaceFilePath, JSON.stringify(content, null, 2), 'utf-8');

	return vscode.Uri.file(workspaceFilePath);
}