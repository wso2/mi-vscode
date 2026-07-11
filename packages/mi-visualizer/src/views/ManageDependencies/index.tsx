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

import React, { useState } from "react";
import { EVENT_TYPE, MACHINE_VIEW } from "@wso2/mi-core";
import { useVisualizerContext } from "@wso2/mi-rpc-client";
import { Button, Codicon } from "@wso2/ui-toolkit";
import { View, ViewHeader, ViewContent } from "../../components/View";
import { DependencyTab, DependencyType } from "./DependencyTab";
import styled from "@emotion/styled";

const TabBar = styled.div`
    display: flex;
    gap: 4px;
    border-bottom: 1px solid var(--vscode-panel-border);
    margin-bottom: 8px;
`;

const TabButton = styled.button<{ active: boolean }>`
    background: none;
    border: none;
    border-bottom: 2px solid ${(p: { active: boolean }) => (p.active ? 'var(--vscode-panelTitle-activeBorder, var(--vscode-button-background))' : 'transparent')};
    color: ${(p: { active: boolean }) => (p.active ? 'var(--vscode-panelTitle-activeForeground, var(--vscode-foreground))' : 'var(--vscode-panelTitle-inactiveForeground, var(--vscode-descriptionForeground))')};
    padding: 8px 14px;
    font-size: 13px;
    cursor: pointer;
    white-space: nowrap;

    &:hover {
        color: var(--vscode-foreground);
    }
`;

interface TabDef {
    id: DependencyType;
    label: string;
}

const TABS: TabDef[] = [
    { id: "zip", label: "Connector Dependencies" },
    { id: "inbound", label: "Inbound-endpoint Dependencies" },
    { id: "car", label: "Integration Project Dependencies" },
    { id: "jar", label: "JAR Dependencies" },
];

export function ManageDependencies() {
    const { rpcClient } = useVisualizerContext();
    const [activeTab, setActiveTab] = useState<DependencyType>("zip");

    const goToOverview = () => {
        rpcClient.getMiVisualizerRpcClient().openView({
            type: EVENT_TYPE.OPEN_VIEW,
            location: { view: MACHINE_VIEW.Overview }
        });
    };

    return (
        <View>
            <ViewHeader title="Manage Dependencies" codicon="package">
                <Button appearance="secondary" onClick={goToOverview}>
                    <Codicon name="arrow-left" />&nbsp;Back to Overview
                </Button>
            </ViewHeader>
            <ViewContent padding>
                <TabBar>
                    {TABS.map(tab => (
                        <TabButton
                            key={tab.id}
                            active={activeTab === tab.id}
                            onClick={() => setActiveTab(tab.id)}
                        >
                            {tab.label}
                        </TabButton>
                    ))}
                </TabBar>
                {/* Render only the active tab and key it by tab id so switching tabs remounts the
                    component, resetting its state (e.g. a half-opened add form) back to the list view. */}
                <DependencyTab key={activeTab} type={activeTab} />
            </ViewContent>
        </View>
    );
}
