/* eslint-disable @typescript-eslint/no-explicit-any */

/**
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
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

import { createExtensionTransportManager, createRequestRouter } from "@wso2/webview-giga-bridge";
import { randomBytes } from "crypto";
import * as path from "path";
import { commands, Disposable, Uri, WebviewPanel, window, workspace } from "vscode";
import { COMMANDS } from "../constants";
import { MiDiagramRpcManager } from "../rpc-managers/mi-diagram/rpc-manager";
import { getSupportedMIVersionsHigherThan } from "../util/onboardingUtils";
// Shared wire contract — single source of truth for both this server and the
// webview client (`mi-visualizer` `MiWsClient`).
import {
    CreateMiProjectRequest,
    CreateMiProjectResponse,
    WEBVIEW_WS_EVENTS,
    WebviewWsBootstrap,
    WebviewWsRequest,
    WebviewWsResponse,
} from "@wso2/mi-core";

export type { WebviewWsBootstrap };

type TransportManager = ReturnType<typeof createExtensionTransportManager<WebviewWsRequest, WebviewWsResponse>>;

/**
 * The webview-communication layer for the MI project-creation form. Built on
 * the shared `@wso2/webview-giga-bridge`, mirroring the Ballerina extension's
 * `DefaultServer`.
 *
 * One router of action handlers (calling the underlying business logic directly,
 * NOT the vscode-messenger RPC handlers) is served over TWO transports:
 *  - **proxy** — wired to the MI visualizer webview panel (postMessage), so the
 *    standalone visualizer's forms can use this bridge; and
 *  - **websocket** — an OS-allocated, token-gated socket the embedded integrator
 *    webview connects to.
 */
export class DefaultServer {
    private static instance: DefaultServer | undefined;

    private readonly token = randomBytes(32).toString("hex");
    private readonly router = createRequestRouter<WebviewWsRequest, WebviewWsResponse>();
    private proxyManager: TransportManager | undefined;
    private wsManager: TransportManager | undefined;
    private wsBootstrap: WebviewWsBootstrap | undefined;
    private readonly disposables: Disposable[] = [];

    private constructor() {
        this.registerHandlers();
    }

    static getInstance(): DefaultServer {
        if (!DefaultServer.instance) {
            DefaultServer.instance = new DefaultServer();
        }
        return DefaultServer.instance;
    }

    /** Attaches the MI visualizer webview panel for proxy (standalone) mode. */
    registerVisualizerPanel(panel: WebviewPanel): Disposable {
        const mgr = this.ensureProxyManager();
        return mgr.registerWebviewPanel(panel as any);
    }

    /** Starts (if needed) the websocket server for the embedded integrator form
     *  and returns the connection coordinates. */
    getWsBootstrap(): WebviewWsBootstrap {
        if (!this.wsBootstrap) {
            const mgr = createExtensionTransportManager<WebviewWsRequest, WebviewWsResponse>({
                initialMode: "websocket",
                wsPort: 0,
                handleRequest: (request) => {
                    if (!request || request.token !== this.token) {
                        return this.errorResponse(request?.action ?? "unknown", "Unauthorized MI bridge request.");
                    }
                    return this.router.handle(request);
                },
            });
            this.wsManager = mgr;
            const wb = mgr.getWebviewBootstrap();
            this.wsBootstrap = { host: wb.wsServer, port: wb.wsPort, token: this.token };
        }
        return this.wsBootstrap;
    }

    dispose(): void {
        this.disposables.forEach((d) => d.dispose());
        this.disposables.length = 0;
        this.proxyManager?.dispose();
        this.wsManager?.dispose();
        this.proxyManager = undefined;
        this.wsManager = undefined;
        this.wsBootstrap = undefined;
        DefaultServer.instance = undefined;
    }

    private ensureProxyManager(): TransportManager {
        if (!this.proxyManager) {
            this.proxyManager = createExtensionTransportManager<WebviewWsRequest, WebviewWsResponse>({
                initialMode: "proxy",
                wsPort: 0,
                // Proxy is in-process to the trusted visualizer webview — no token gate.
                handleRequest: (request) => this.router.handle(request),
            });
        }
        return this.proxyManager;
    }

    private successResponse(action: string, result: unknown): WebviewWsResponse {
        return { type: WEBVIEW_WS_EVENTS.WS_RESPONSE, action, success: true, result: result ?? null };
    }

    private errorResponse(action: string, error: string): WebviewWsResponse {
        return { type: WEBVIEW_WS_EVENTS.WS_RESPONSE, action, success: false, error };
    }

    private register(action: string, handler: (params: any) => unknown | Promise<unknown>): void {
        this.router.register(action, async (request) => {
            try {
                return this.successResponse(action, await handler(request.params));
            } catch (error) {
                return this.errorResponse(action, error instanceof Error ? error.message : "MI bridge handler failed.");
            }
        });
    }

    private registerHandlers(): void {
        // Project-less manager instance — the handlers below don't rely on a
        // project URI (same pattern as the MI.project-explorer.create-project command).
        const miDiagram = new MiDiagramRpcManager("");

        this.register("createMiProject", (p: CreateMiProjectRequest) => this.createMiProject(p));
        this.register("importProjectFromCapp", () => commands.executeCommand(COMMANDS.IMPORT_FROM_CAPP));
        this.register("getSupportedMIVersionsHigherThan", async (version: string) => ({
            versions: await getSupportedMIVersionsHigherThan(typeof version === "string" ? version : ""),
        }));
        this.register("getWorkspaceRoot", () => ({
            path: workspace.workspaceFolders?.[0]?.uri.fsPath ?? "",
        }));
        this.register("getSubFolderNames", (p) => miDiagram.getSubFolderNames(p));
        this.register("askProjectDirPath", () => miDiagram.askProjectDirPath());
        this.register("showErrorMessage", (p) => {
            window.showErrorMessage(typeof p === "string" ? p : p?.message ?? "An error occurred.");
        });
    }

    /** Creates an MI project via the project-explorer command (the same flow the
     *  WSO2 Integrator delegated to before the form moved here). */
    private async createMiProject(params: CreateMiProjectRequest): Promise<CreateMiProjectResponse> {
        try {
            const miCommandParams = {
                name: params.name,
                path: path.join(params.directory, params.name),
                scope: "user",
                open: params.open,
                miVersion: params.miVersion,
                isConsolidatedProject: params.isConsolidatedProject ?? false,
                subProjects: params.subProjects ?? [],
                groupId: params.groupID ?? "com.microintegrator.projects",
                artifactId: params.artifactID ?? params.name,
                version: params.version ?? "1.0.0",
            };
            const result = await commands.executeCommand(COMMANDS.CREATE_PROJECT_COMMAND, miCommandParams);
            if (result) {
                const response = result as CreateMiProjectResponse;
                await commands.executeCommand("vscode.openFolder", Uri.file(path.resolve(response.filePath)));
                return response;
            }
            return { filePath: "" };
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);
            window.showErrorMessage(`Failed to create MI project: ${errorMessage}`);
            throw error;
        }
    }
}
