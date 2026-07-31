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

import {
    CreateMiProjectRequest,
    CreateMiProjectResponse,
    GetSubFoldersRequest,
    GetSubFoldersResponse,
    GetSupportedMIVersionsResponse,
    ProjectDirResponse,
    WEBVIEW_WS_EVENTS,
    WebviewTransportBootstrap,
    WebviewWsRequest,
    WebviewWsResponse,
    WorkspaceRootResponse,
} from "@wso2/mi-core";
import { ConnectionStatus, createWebviewTransportAdapter } from "@wso2/webview-giga-bridge/webview";

declare global {
    interface Window {
        /** Injected by the host to force websocket mode + coords when the form is
         *  embedded outside the MI visualizer (e.g. the integrator webview). */
        __MI_BRIDGE_BOOTSTRAP?: WebviewTransportBootstrap;
    }
}

const DEFAULT_WS_SERVER = "127.0.0.1";
const DEFAULT_WS_PORT = 8787;

/**
 * Resolves the bridge transport. An explicitly-injected bootstrap always wins
 * (the integrator embed injects `{ mode: 'websocket', ... }`). Otherwise, when a
 * VS Code webview API is present we are inside the MI visualizer and use
 * `proxy` (postMessage) mode; failing that, websocket.
 */
export function resolveMiBridgeBootstrap(): WebviewTransportBootstrap {
    const injected = window.__MI_BRIDGE_BOOTSTRAP;
    if (injected?.mode) {
        return {
            mode: injected.mode,
            wsServer: injected.wsServer ?? DEFAULT_WS_SERVER,
            wsPort: injected.wsPort ?? DEFAULT_WS_PORT,
            token: injected.token,
        };
    }
    const hasVsCodeApi = typeof (globalThis as { acquireVsCodeApi?: unknown }).acquireVsCodeApi === "function";
    return {
        mode: hasVsCodeApi ? "proxy" : "websocket",
        wsServer: DEFAULT_WS_SERVER,
        wsPort: DEFAULT_WS_PORT,
    };
}

/**
 * The MI project-creation form WS manager client. One flat client over the
 * shared giga-bridge transport, mirroring the Ballerina `BiWsClient`. Works
 * unchanged in proxy and websocket modes.
 */
export class MiWsClient {
    private readonly bootstrap: WebviewTransportBootstrap;
    private readonly transport: ReturnType<typeof createWebviewTransportAdapter<WebviewWsRequest, WebviewWsResponse>>;

    constructor(bootstrap: WebviewTransportBootstrap = resolveMiBridgeBootstrap()) {
        this.bootstrap = bootstrap;
        this.transport = createWebviewTransportAdapter<WebviewWsRequest, WebviewWsResponse>({
            mode: bootstrap.mode,
            server: bootstrap.wsServer,
            port: bootstrap.wsPort,
            // Presented during the websocket handshake; the host rejects the
            // upgrade without it. Ignored in proxy mode, which does not use a
            // socket.
            token: bootstrap.token,
        });
        this.transport.subscribe(
            () => undefined,
            (status) => this.handleConnectionStatus(status),
        );
    }

    // ── Project creation ──────────────────────────────────────
    public createMiProject(params: CreateMiProjectRequest): Promise<CreateMiProjectResponse> {
        return this.request("createMiProject", params);
    }

    public importProjectFromCapp(): Promise<void> {
        return this.request("importProjectFromCapp");
    }

    public getSupportedMIVersionsHigherThan(version: string): Promise<GetSupportedMIVersionsResponse> {
        return this.request("getSupportedMIVersionsHigherThan", version);
    }

    public getWorkspaceRoot(): Promise<WorkspaceRootResponse> {
        return this.request("getWorkspaceRoot");
    }

    public getSubFolderNames(params: GetSubFoldersRequest): Promise<GetSubFoldersResponse> {
        return this.request("getSubFolderNames", params);
    }

    public askProjectDirPath(): Promise<ProjectDirResponse> {
        return this.request("askProjectDirPath");
    }

    public showErrorMessage(params: any): Promise<void> {
        return this.request("showErrorMessage", params);
    }

    /** Returns to the welcome view. In the integrator embed this is overridden by
     *  the host's `onBack`. */
    public goBack(): Promise<void> {
        return this.request("goBack");
    }

    // ── Transport ─────────────────────────────────────────────
    public async request<T = any>(action: string, params?: unknown): Promise<T> {
        const payload: WebviewWsRequest = { action };
        if (params !== undefined) {
            payload.params = params;
        }
        const response = await this.transport.request(payload);
        if (!response || response.type !== WEBVIEW_WS_EVENTS.WS_RESPONSE || response.action !== action) {
            throw new Error(`Unexpected response for "${action}"`);
        }
        if (!response.success) {
            throw new Error(response.error ?? `Request failed for "${action}"`);
        }
        return response.result as T;
    }

    public notify(action: string, params?: unknown): void {
        void this.request(action, params).catch((error) => {
            console.warn(`[MI bridge] Failed to send "${action}"`, error);
        });
    }

    public dispose(): void {
        this.transport.close();
    }

    private handleConnectionStatus(status: ConnectionStatus): void {
        if (status === "error") {
            console.warn("[MI bridge] connection error");
        }
    }
}
