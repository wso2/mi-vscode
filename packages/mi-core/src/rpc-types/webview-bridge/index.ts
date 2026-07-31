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

/**
 * Shared wire contract for the MI project-creation form webview-communication
 * layer. Used by BOTH the webview client (`mi-visualizer` `MiWsClient`) and the
 * extension server (`mi-extension` `DefaultServer`) over the
 * `@wso2/webview-giga-bridge` transport, in proxy (postMessage) and websocket
 * modes.
 */
export const WEBVIEW_WS_EVENTS = {
    /** Correlated reply to a `request()`/`notify()`. */
    WS_RESPONSE: "mi.ws.response",
} as const;

/** Request envelope the form sends; the bridge unwraps it. The per-session
 *  `token` is required only in websocket (embedded) mode. */
export interface WebviewWsRequest {
    action: string;
    params?: unknown;
    token?: string;
}

export interface WebviewWsResponseMessage {
    type: typeof WEBVIEW_WS_EVENTS.WS_RESPONSE;
    action: string;
    success: boolean;
    result?: unknown;
    error?: string;
}

export type WebviewWsResponse = WebviewWsResponseMessage;

/** Connection coordinates resolved at form load. `proxy` talks to the MI
 *  visualizer host over postMessage; `websocket` connects to the MI
 *  extension's giga-bridge server (used for the integrator embed). */
export interface WebviewTransportBootstrap {
    mode: "proxy" | "websocket";
    wsServer: string;
    wsPort: number;
    /** Per-session token required by the websocket server. */
    token?: string;
}

/** Coordinates the extension relays to the embedded form so it can connect over
 *  websocket (host + OS-allocated port + per-session token). */
export interface WebviewWsBootstrap {
    host: string;
    port: number;
    token: string;
}

/** Parameters the MI project-creation form submits. */
export interface CreateMiProjectRequest {
    directory: string;
    name: string;
    open: boolean;
    groupID?: string;
    artifactID?: string;
    version?: string;
    miVersion: string;
    isConsolidatedProject?: boolean;
    subProjects?: string[];
}

export interface CreateMiProjectResponse {
    filePath: string;
}

export interface GetSupportedMIVersionsResponse {
    versions: string[];
}

export interface WorkspaceRootResponse {
    path: string;
}
