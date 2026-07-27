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

import React, { createContext, useContext, useEffect, useMemo } from "react";
import { MiWsClient } from "./WsClient";
import { WebviewTransportBootstrap } from "@wso2/mi-core";

interface MiWsContextValue {
    /** The MI-form WS manager client (giga-bridge, proxy or websocket). */
    wsClient: MiWsClient;
    /** Back navigation. In the integrator embed this closes the form and returns
     *  to the integrator welcome. */
    onBack: () => void;
}

const MiWsContext = createContext<MiWsContextValue | undefined>(undefined);

interface MiWsClientProviderProps {
    /** Inject a pre-built client (e.g. one wired to a host-provided bootstrap).
     *  When omitted, a client is created from the resolved bridge bootstrap. */
    wsClient?: MiWsClient;
    /** Force a specific transport bootstrap (the integrator embed passes websocket coords). */
    bootstrap?: WebviewTransportBootstrap;
    /** Optional back-navigation override (the integrator embed supplies its own). */
    onBack?: () => void;
    children: React.ReactNode;
}

/**
 * Provides the MI-form `wsClient`. Mirrors the integrator's
 * `useVisualizerContext().wsClient` seam so the same form code runs in the MI
 * visualizer and embedded in the integrator.
 */
export function MiWsClientProvider({ wsClient, bootstrap, onBack, children }: MiWsClientProviderProps) {
    // Create the client from (injected client | bootstrap) only — NOT from `onBack`.
    // Including `onBack` here would spin up a fresh MiWsClient (and WS connection)
    // every time the parent passed a new inline `onBack` reference.
    const client = useMemo(() => wsClient ?? new MiWsClient(bootstrap), [wsClient, bootstrap]);

    // Dispose only a client we created here; an injected client is owned by the caller.
    useEffect(() => {
        if (wsClient) {
            return;
        }
        return () => client.dispose();
    }, [client, wsClient]);

    const value = useMemo<MiWsContextValue>(
        () => ({
            wsClient: client,
            onBack: onBack ?? (() => client.goBack()),
        }),
        [client, onBack],
    );

    return <MiWsContext.Provider value={value}>{children}</MiWsContext.Provider>;
}

export function useMiWsContext(): MiWsContextValue {
    const ctx = useContext(MiWsContext);
    if (!ctx) {
        throw new Error("useMiWsContext must be used within a MiWsClientProvider");
    }
    return ctx;
}
