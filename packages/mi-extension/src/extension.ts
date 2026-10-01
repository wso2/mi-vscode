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

	// Filter out sub-projects that openConsolidatedAsWorkspace is asynchronously removing.
	let excludedPaths = new Set<string>();
	if (workspace.workspaceFolders) {
		// Reopening as a consolidated workspace reloads the window.
		const result = await openConsolidatedAsWorkspace(context);
		if (result.stop) {
			return;
		}
		excludedPaths = result.excludedPaths;
	}

	const activeFolders = workspace.workspaceFolders?.filter(folder => !excludedPaths.has(folder.uri.fsPath));

	const oldProjects = activeFolders
		? (await Promise.all(
			activeFolders.map(async folder => {
				const isOld = await isOldProjectOrWorkspace(folder.uri.fsPath);
				if (isOld) getStateMachine(folder.uri.fsPath);
				return isOld ? folder : null;
			})
		)).filter((folder): folder is vscode.WorkspaceFolder => folder !== null)
		: [];
	const newProjects = activeFolders
		? activeFolders.filter(folder => !oldProjects.includes(folder))
		: [];

	const firstProject = newProjects?.[0]?.uri?.fsPath || 
						 oldProjects?.[0]?.uri?.fsPath || 
						 path.join(os.tmpdir(), uuidv4());
	
	const updateMultiProjectContext = () => {
		const count = workspace.workspaceFolders?.length ?? 0;
		vscode.commands.executeCommand('setContext', 'MI.hasMultipleProjects', count > 1);
	};

	if (!oldProjects.length) {
		const showWorkspaceOverview = shouldShowWorkspaceOverview();
		getStateMachine(firstProject, showWorkspaceOverview ? { view: MACHINE_VIEW.WorkspaceOverview } : undefined);
	}
	updateMultiProjectContext();

	workspace.onDidChangeWorkspaceFolders(async (event) => {
		if (event.added.length > 0) {
			const validAdded = await rejectIsolatedSubProjects(event.added);
			if (validAdded.length > 0) {
				// If several folders are added at once, this avoids opening one panel per folder.
				const showWorkspaceOverview = shouldShowWorkspaceOverview();
				getStateMachine(validAdded[0].uri.fsPath, showWorkspaceOverview ? { view: MACHINE_VIEW.WorkspaceOverview } : undefined);
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
 * Removes newly added folders that are sub-projects of a consolidated project
 * missing their siblings, and returns the rest.
 */
async function rejectIsolatedSubProjects(addedFolders: readonly vscode.WorkspaceFolder[]): Promise<vscode.WorkspaceFolder[]> {
	const valid: vscode.WorkspaceFolder[] = [];
	const rejectedPaths = new Set<string>();
	const warnedRoots = new Set<string>();
	const currentFolderPaths = (workspace.workspaceFolders ?? []).map(f => f.uri.fsPath);
	const getCachedSubUris = makeSubUrisCache();

	for (const folder of addedFolders) {
		const folderPath = folder.uri.fsPath;
		const parent = path.dirname(folderPath);
		const subUris = isConsolidatedProject(parent) ? await getCachedSubUris(parent) : [];
		// Only enforce completeness for folders that are actually declared modules.
		const isDeclaredModule = subUris.some(uri => uri.fsPath === folderPath);
		if (isDeclaredModule) {
			const { missingFromDisk, missingPom, missingFromWorkspace } = classifyMissingModules(parent, currentFolderPaths);
			if (missingFromDisk.length > 0 || missingPom.length > 0 || missingFromWorkspace.length > 0) {
				if (!warnedRoots.has(parent)) {
					warnedRoots.add(parent);
					const details = await readConsolidatedProjectDetails(parent);
					const projectName = details?.artifactId?.trim() || path.basename(parent);
					if (missingFromDisk.length > 0 || missingPom.length > 0) {
						vscode.window.showErrorMessage(
							`${describeBrokenModules(projectName, missingFromDisk, missingPom)} It cannot be opened until they're restored.`,
							{ modal: true }
						);
					} else {
						vscode.window.showErrorMessage(
							`This project is a part of the consolidated project "${projectName}" and cannot be added to a workspace on its own.`,
							{ modal: true }
						);
					}
				}
				rejectedPaths.add(folderPath);
				continue;
			}
		}
		valid.push(folder);
	}

	if (rejectedPaths.size > 0) {
		const remaining = (workspace.workspaceFolders ?? []).filter(f => !rejectedPaths.has(f.uri.fsPath));
		workspace.updateWorkspaceFolders(0, workspace.workspaceFolders?.length ?? 0, ...remaining.map(f => ({ uri: f.uri, name: f.name })));
	}

	return valid;
}

/**
 * All module paths declared in a consolidated project's pom.xml, on disk or not.
 */
function getDeclaredModulePaths(folderPath: string): string[] {
	try {
		const pom = parseConsolidatedProjectPom(path.join(folderPath, 'pom.xml'));
		return getModules(pom.project)
			.map(name => path.join(folderPath, name))
			.filter(modulePath => path.basename(modulePath) !== 'docker-build');
	} catch (err) {
		console.error('Could not read modules from consolidated project pom.xml', err);
		return [];
	}
}

/**
 * Returns a consolidated project root's declared sub-project folders that exist on disk.
 */
async function getSubProjectUris(folderPath: string): Promise<vscode.Uri[]> {
	return getDeclaredModulePaths(folderPath)
		.filter(modulePath => fs.existsSync(path.join(modulePath, 'pom.xml')))
		.map(modulePath => vscode.Uri.file(modulePath));
}

/**
 * Splits a consolidated project's missing modules into ones missing from disk,
 * missing a pom.xml and present but not open.
 */
function classifyMissingModules(root: string, presentFolderPaths: string[]): { missingFromDisk: string[]; missingPom: string[]; missingFromWorkspace: string[] } {
	const missing = getDeclaredModulePaths(root).filter(modulePath => !presentFolderPaths.includes(modulePath));
	const hasPom = (modulePath: string) => fs.existsSync(path.join(modulePath, 'pom.xml'));
	return {
		missingFromDisk: missing.filter(modulePath => !fs.existsSync(modulePath)),
		missingPom: missing.filter(modulePath => fs.existsSync(modulePath) && !hasPom(modulePath)),
		missingFromWorkspace: missing.filter(hasPom),
	};
}

/**
 * Describes a consolidated project's broken modules to create an error message.
 */
function describeBrokenModules(projectName: string, missingFromDisk: string[], missingPom: string[]): string {
	const names = (modulePaths: string[]) => modulePaths.map(modulePath => path.basename(modulePath)).join(', ');
	const problems: string[] = [];
	if (missingFromDisk.length > 0) {
		problems.push(`module(s) "${names(missingFromDisk)}" missing on disk`);
	}
	if (missingPom.length > 0) {
		problems.push(`module(s) "${names(missingPom)}" without a pom.xml`);
	}
	return `The consolidated project "${projectName}" has ${problems.join(' and ')}.`;
}

/**
 * Caches getSubProjectUris() per root, since a batch of folders can share one.
 */
function makeSubUrisCache() {
	const cache = new Map<string, Promise<vscode.Uri[]>>();
	return (root: string) => {
		let subUris = cache.get(root);
		if (!subUris) {
			subUris = getSubProjectUris(root);
			cache.set(root, subUris);
		}
		return subUris;
	};
}

type ConsolidatedWorkspaceResult = {
	// True if the window is reloading or closing — caller should return immediately.
	stop: boolean;
	// Folders already removed from the workspace; treat as absent for the rest of activation.
	excludedPaths: Set<string>;
};

/**
 * Generates a named .code-workspace for a consolidated project, or removes any
 * sub-project opened without its siblings.
 */
async function openConsolidatedAsWorkspace(context: vscode.ExtensionContext): Promise<ConsolidatedWorkspaceResult> {
	const proceed: ConsolidatedWorkspaceResult = { stop: false, excludedPaths: new Set() };
	try {
		const folders = workspace.workspaceFolders;
		if (!folders || folders.length === 0) {
			return proceed;
		}

		// Build the target folder set by collecting consolidated root(s) and which folder belongs to which.
		const getCachedSubUris = makeSubUrisCache();
		const consolidatedRoots = new Set<string>();
		const folderPaths: string[] = [];
		const rootByFolderPath = new Map<string, string>();
		for (const folder of folders) {
			const folderPath = folder.uri.fsPath;
			if (isConsolidatedProject(folderPath)) {
				// Consolidated root opened directly: expand into its sub-projects.
				consolidatedRoots.add(folderPath);
				rootByFolderPath.set(folderPath, folderPath);
				const subUris = await getCachedSubUris(folderPath);
				folderPaths.push(...subUris.map(uri => uri.fsPath));
			} else {
				folderPaths.push(folderPath);
				// A restored untitled workspace lists sub-projects instead of the root.
				// Detect via the parent directory, but only if it's a declared module.
				const parent = path.dirname(folderPath);
				if (isConsolidatedProject(parent)) {
					const subUris = await getCachedSubUris(parent);
					if (subUris.some(uri => uri.fsPath === folderPath)) {
						consolidatedRoots.add(parent);
						rootByFolderPath.set(folderPath, parent);
					}
				}
			}
		}

		if (consolidatedRoots.size === 0 || folderPaths.length === 0) {
			return proceed;
		}

		// A sub-project only works alongside its siblings. Collect all affected folders
		// first so multiple incomplete roots are resolved in one update.
		const excludedPaths = new Set<string>();
		for (const root of consolidatedRoots) {
			const { missingFromDisk, missingPom, missingFromWorkspace } = classifyMissingModules(root, folderPaths);
			if (missingFromDisk.length === 0 && missingPom.length === 0 && missingFromWorkspace.length === 0) {
				continue;
			}
			const affectedPaths = [...rootByFolderPath.entries()].filter(([, r]) => r === root).map(([p]) => p);

			if (missingFromDisk.length > 0 || missingPom.length > 0) {
				await showBrokenConsolidatedProjectError(root, missingFromDisk, missingPom);
			} else {
				// A missing-but-existing sibling can still be fixed by opening the root.
				const reopened = await promptOpenConsolidatedProject(root);
				if (reopened) {
					return { stop: true, excludedPaths: new Set() };
				}
			}
			affectedPaths.forEach(p => excludedPaths.add(p));
		}

		if (excludedPaths.size > 0) {
			if (excludedPaths.size === folders.length) {
				await vscode.commands.executeCommand('workbench.action.closeFolder');
				return { stop: true, excludedPaths: new Set() };
			}
			const remaining = folders.filter(f => !excludedPaths.has(f.uri.fsPath));
			workspace.updateWorkspaceFolders(0, folders.length, ...remaining.map(f => ({ uri: f.uri, name: f.name })));
			return { stop: false, excludedPaths };
		}

		// Already a saved, complete workspace file — nothing to regenerate.
		if (workspace.workspaceFile && workspace.workspaceFile.scheme !== 'untitled') {
			return proceed;
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
		return { stop: true, excludedPaths: new Set() };
	} catch (err) {
		console.error('Error opening consolidated project as workspace', err);
		return proceed;
	}
}

/**
 * Shown when a declared module is missing from disk or has no pom.xml — unfixable by reopening.
 */
async function showBrokenConsolidatedProjectError(consolidatedRoot: string, missingFromDisk: string[], missingPom: string[]): Promise<void> {
	const details = await readConsolidatedProjectDetails(consolidatedRoot);
	const projectName = details?.artifactId?.trim() || path.basename(consolidatedRoot);
	await vscode.window.showErrorMessage(
		`${describeBrokenModules(projectName, missingFromDisk, missingPom)} Restore them before opening this project.`,
		{ modal: true }
	);
}

/**
 * Warns that a sub-project was opened without its siblings and offers to open the
 * consolidated project instead.
 */
async function promptOpenConsolidatedProject(consolidatedRoot: string): Promise<boolean> {
	const details = await readConsolidatedProjectDetails(consolidatedRoot);
	const projectName = details?.artifactId?.trim() || path.basename(consolidatedRoot);
	const selection = await vscode.window.showErrorMessage(
		`This project is a part of the consolidated project "${projectName}" and cannot be opened on its own. Open the consolidated project instead.`,
		{ modal: true },
		'Open Consolidated Project'
	);
	if (selection === 'Open Consolidated Project') {
		await vscode.commands.executeCommand('vscode.openFolder', vscode.Uri.file(consolidatedRoot), false);
		return true;
	}
	return false;
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