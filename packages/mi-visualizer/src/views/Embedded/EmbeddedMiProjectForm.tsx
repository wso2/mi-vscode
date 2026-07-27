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

import React from "react";
import { WebviewTransportBootstrap } from "@wso2/mi-core";
import { MiProjectForm } from "./MiProjectForm";
import { resolveMiBridgeBootstrap } from "./wsManager/WsClient";
import { MiWsClientProvider } from "./wsManager/WsClientContext";

export interface EmbeddedMiProjectFormProps {
    /** Back navigation supplied by the host (closes the form and returns to the
     *  host's welcome view). */
    onBack?: () => void;
    /** Websocket coordinates of the MI extension's giga-bridge server. When
     *  omitted, the bootstrap is resolved from `window.__MI_BRIDGE_BOOTSTRAP`
     *  (which the host injects before mounting this component). */
    bootstrap?: WebviewTransportBootstrap;
}

/**
 * Federated entry point for the MI project-creation form. Loaded by the WSO2
 * Integrator's welcome webview via Webpack Module Federation; talks to the MI
 * extension host over the token-gated giga-bridge websocket.
 */
export default function EmbeddedMiProjectForm({ onBack, bootstrap }: EmbeddedMiProjectFormProps) {
    const resolved = React.useMemo(() => bootstrap ?? resolveMiBridgeBootstrap(), [bootstrap]);
    return (
        <MiWsClientProvider bootstrap={resolved} onBack={onBack}>
            <MiProjectForm />
        </MiWsClientProvider>
    );
}
