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
import { webviews } from './visualizer/webview';
import { v4 as uuidv4 } from 'uuid';
import path from 'path';
import { COMMANDS, WI_EXTENSION_ID } from './constants';
import { enableLS } from './util/workspace';
import { disposeMIAgentPanelRpcManager } from './rpc-managers/agent-mode/rpc-handler';
import { isConsolidatedProject, isMiProject, ensureMavenWrapper } from './util/onboardingUtils';
const os = require('os');
const fs = require('fs');

const MAVEN_WRAPPER_SETUP_CONCURRENCY = 5;

export async function activate(context: vscode.ExtensionContext) {
	extension.context = context;

	// TODO: Remove when VSCode fixes: https://github.com/microsoft/vscode/issues/188257
	const orphanedTabs = vscode.window.tabGroups.all
		.flatMap((tabGroup) => tabGroup.tabs)
		.filter((tab) => (tab.input as any)?.viewType?.includes("micro-integrator."));
	vscode.window.tabGroups.close(orphanedTabs);

	if (workspace.workspaceFolders) {
		for (const folder of workspace.workspaceFolders) {
			await replaceWithSubProjects(folder);
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
	
	if (!oldProjects.length) {
		getStateMachine(firstProject);
	}

	if (workspace.workspaceFolders) {
		void ensureMavenWrapperForWorkspace(workspace.workspaceFolders);
	}

	workspace.onDidChangeWorkspaceFolders(async (event) => {
		if (event.added.length > 0) {
			for (const addedProject of event.added) {
				getStateMachine(addedProject.uri.fsPath);
			}
			void ensureMavenWrapperForWorkspace(event.added);
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

async function runWithConcurrencyLimit<T>(items: readonly T[], limit: number, task: (item: T) => Promise<void>): Promise<void> {
	let index = 0;
	const workers = Array.from({ length: Math.min(limit, items.length) }, async () => {
		while (index < items.length) {
			await task(items[index++]);
		}
	});
	await Promise.all(workers);
}

/**
 * Ensures the maven wrapper exists for every MI project folder, without waiting for the
 * Overview page to be opened. Also covers a consolidated project's root.
 */
async function ensureMavenWrapperForWorkspace(folders: readonly vscode.WorkspaceFolder[]): Promise<void> {
	const handledConsolidatedRoots = new Set<string>();
	await runWithConcurrencyLimit(folders, MAVEN_WRAPPER_SETUP_CONCURRENCY, async folder => {
		const folderPath = folder.uri.fsPath;
		try {
			if (await isMiProject(folderPath)) {
				await ensureMavenWrapper(folderPath);
			}
			if (!handledConsolidatedRoots.has(folderPath) && isConsolidatedProject(folderPath)) {
				handledConsolidatedRoots.add(folderPath);
				await ensureMavenWrapper(folderPath);
			}
		} catch (err) {
			console.error(`Failed to set up maven wrapper for ${folderPath}`, err);
		}

		const parent = path.dirname(folderPath);
		if (!handledConsolidatedRoots.has(parent)) {
			handledConsolidatedRoots.add(parent);
			try {
				if (isConsolidatedProject(parent)) {
					await ensureMavenWrapper(parent);
				}
			} catch (err) {
				console.error(`Failed to set up maven wrapper for consolidated root ${parent}`, err);
			}
		}
	});
}

async function replaceWithSubProjects(folder: vscode.WorkspaceFolder) {
	try {
		const folderPath = folder.uri.fsPath;
		if (!isConsolidatedProject(folderPath)) {
			return;
		}

		const entries = await fs.promises.readdir(folderPath, { withFileTypes: true });
		const subUris: vscode.Uri[] = [];

		for (const entry of entries) {
			const subPath = path.join(folderPath, entry.name);
			if (!entry.isDirectory() || entry.name.startsWith('.') || fs.existsSync(path.join(subPath, '.docker-build'))) {
				continue;
			}
			const pomPath = path.join(subPath, 'pom.xml');

			if (fs.existsSync(pomPath)) {
				subUris.push(vscode.Uri.file(subPath));
			}
		}

		if (subUris.length === 0) {
			return;
		}
		vscode.workspace.updateWorkspaceFolders(
			folder.index,   // start index
			1,              // remove the consolidated project folder
			...subUris.map(uri => ({ uri }))
		);

	} catch (err) {
		console.error('Error replacing consolidated project', err);
	}
}