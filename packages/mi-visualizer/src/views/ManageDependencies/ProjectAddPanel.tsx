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

import React, { useEffect, useState } from "react";
import { DependencyDetails, WorkspaceMiProject } from "@wso2/mi-core";
import { useVisualizerContext } from "@wso2/mi-rpc-client";
import { Button, Codicon, Typography, ProgressRing } from "@wso2/ui-toolkit";
import { DependencyForm } from "../Overview/ProjectInformation/DependencyForm";
import styled from "@emotion/styled";

const HeaderBar = styled.div`
    display: flex;
    align-items: center;
    margin: 16px 0;
`;

const OptionCard = styled.div`
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px;
    border: 1.5px solid var(--vscode-dropdown-border);
    border-radius: 8px;
    background-color: var(--vscode-menu-background);
    margin-bottom: 10px;
    cursor: pointer;
    transition: border-color 0.2s ease;

    &:hover {
        border-color: var(--vscode-button-background);
    }
`;

const OptionText = styled.div`
    display: flex;
    flex-direction: column;
    gap: 2px;
`;

const OptionTitle = styled.span`
    font-size: 13px;
    font-weight: 600;
    color: var(--vscode-settings-headerForeground);
`;

const OptionDescription = styled.span`
    font-size: 12px;
    color: var(--vscode-descriptionForeground);
`;

const ProjectRow = styled.div`
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 14px;
    border: 1.5px solid var(--vscode-dropdown-border);
    border-radius: 8px;
    background-color: var(--vscode-menu-background);
    margin-bottom: 8px;

    &:hover {
        border-color: var(--vscode-button-background);
    }
`;

const ProjectInfo = styled.div`
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
`;

const ProjectName = styled.span`
    font-size: 13px;
    font-weight: 600;
    color: var(--vscode-settings-headerForeground);
`;

const ProjectGav = styled.span`
    font-size: 11px;
    font-family: monospace;
    color: var(--vscode-descriptionForeground);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
`;

type ProjectAddMode = "choose" | "manual" | "pom" | "workspace";

interface ProjectAddPanelProps {
    existingDependencies: DependencyDetails[];
    isBusy: boolean;
    duplicateError?: string;
    onClose: () => void;
    onSubmit: (dep: { groupId: string; artifact: string; version: string }, source: "manual" | "pom" | "workspace") => void;
}

export function ProjectAddPanel(props: ProjectAddPanelProps) {
    const { existingDependencies, isBusy, duplicateError, onClose, onSubmit } = props;
    const { rpcClient } = useVisualizerContext();

    const [mode, setMode] = useState<ProjectAddMode>("choose");
    const [prefill, setPrefill] = useState<{ groupId: string; artifact: string; version: string }>({ groupId: "", artifact: "", version: "" });
    const [workspaceProjects, setWorkspaceProjects] = useState<WorkspaceMiProject[] | undefined>(undefined);

    useEffect(() => {
        if (mode === "workspace" && workspaceProjects === undefined) {
            fetchWorkspaceProjects();
        }
    }, [mode]);

    const fetchWorkspaceProjects = async () => {
        try {
            const res = await rpcClient.getMiVisualizerRpcClient().getWorkspaceMiProjects();
            // Hide projects already present as a dependency (matched by Group ID + Artifact ID).
            const available = (res.projects ?? []).filter(project => !existingDependencies.some(
                dep => dep.groupId === project.groupId && dep.artifact === project.artifactId
            ));
            setWorkspaceProjects(available);
        } catch (e) {
            console.error("Failed to fetch workspace MI projects", e);
            setWorkspaceProjects([]);
        }
    };

    const handleSelectPom = async () => {
        const res = await rpcClient.getMiDiagramRpcClient().browseFile({
            canSelectFiles: true,
            canSelectFolders: false,
            canSelectMany: false,
            defaultUri: "",
            title: "Select pom.xml",
            filters: { "POM File": ["xml"] }
        });
        if (!res?.filePath) {
            return;
        }
        const gav = await rpcClient.getMiDiagramRpcClient().parsePomGav({ pomPath: res.filePath });
        setPrefill({ groupId: gav.groupId, artifact: gav.artifactId, version: gav.version });
        setMode("pom");
    };

    if (mode === "manual" || mode === "pom") {
        return (
            <DependencyForm
                groupId={prefill.groupId}
                artifact={prefill.artifact}
                version={prefill.version}
                title="Add Dependency"
                showLoader={isBusy}
                duplicateError={duplicateError}
                onClose={() => { setPrefill({ groupId: "", artifact: "", version: "" }); setMode("choose"); }}
                onUpdate={(dep) => onSubmit(dep, mode)}
            />
        );
    }

    if (mode === "workspace") {
        return (
            <>
                <HeaderBar>
                    <Button appearance="icon" onClick={() => setMode("choose")} tooltip="Back">
                        <Codicon name="arrow-left" />&nbsp;Back
                    </Button>
                </HeaderBar>
                {workspaceProjects === undefined ? (
                    <div style={{ display: 'flex', justifyContent: 'center', paddingTop: '20px' }}>
                        <ProgressRing />
                    </div>
                ) : workspaceProjects.length === 0 ? (
                    <Typography sx={{ marginTop: '10px', opacity: 0.7 }}>
                        No other integration projects found in the workspace.
                    </Typography>
                ) : (
                    workspaceProjects.map((project) => (
                        <ProjectRow key={project.fsPath}>
                            <ProjectInfo>
                                <ProjectName>{project.name}</ProjectName>
                                <ProjectGav>{project.groupId}:{project.artifactId}:{project.version}</ProjectGav>
                            </ProjectInfo>
                            <Button
                                appearance="primary"
                                disabled={isBusy || !project.groupId || !project.artifactId || !project.version}
                                onClick={() => onSubmit({ groupId: project.groupId, artifact: project.artifactId, version: project.version }, "workspace")}
                            >
                                <Codicon name="add" />&nbsp;Add
                            </Button>
                        </ProjectRow>
                    ))
                )}
            </>
        );
    }

    return (
        <>
            <HeaderBar>
                <Button appearance="icon" onClick={onClose} tooltip="Back to dependencies">
                    <Codicon name="arrow-left" />&nbsp;Back
                </Button>
            </HeaderBar>

            <OptionCard onClick={() => { setPrefill({ groupId: "", artifact: "", version: "" }); setMode("manual"); }}>
                <Codicon name="edit" iconSx={{ fontSize: 20 }} />
                <OptionText>
                    <OptionTitle>Configure dependency</OptionTitle>
                    <OptionDescription>Enter the Group ID, Artifact ID and Version manually.</OptionDescription>
                </OptionText>
            </OptionCard>

            <OptionCard onClick={handleSelectPom}>
                <Codicon name="file-code" iconSx={{ fontSize: 20 }} />
                <OptionText>
                    <OptionTitle>Add dependency via pom file</OptionTitle>
                    <OptionDescription>Pick a pom.xml from the filesystem to read its coordinates.</OptionDescription>
                </OptionText>
            </OptionCard>

            <OptionCard onClick={() => setMode("workspace")}>
                <Codicon name="folder-library" iconSx={{ fontSize: 20 }} />
                <OptionText>
                    <OptionTitle>Add project from the workspace</OptionTitle>
                    <OptionDescription>Add another integration project from this workspace as a dependency.</OptionDescription>
                </OptionText>
            </OptionCard>
        </>
    );
}
