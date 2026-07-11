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
import { ConnectorEffectiveData, DependencyDetails, ExternalConnectorDetail } from "@wso2/mi-core";
import { useVisualizerContext } from "@wso2/mi-rpc-client";
import { Button, Typography, Codicon, LinkButton, ProgressRing, Overlay, Dialog } from "@wso2/ui-toolkit";
import { DependencyItem } from "../Overview/ProjectInformation/DependencyItem";
import { DependencyForm } from "../Overview/ProjectInformation/DependencyForm";
import { Colors } from "@wso2/mi-diagram/lib/resources/constants";
import { compareVersions } from "@wso2/mi-diagram/lib/utils/commons";
import { ConnectorAddPanel } from "./ConnectorAddPanel";
import { ProjectAddPanel } from "./ProjectAddPanel";
import styled from "@emotion/styled";

const DRIVER_MANAGEMENT_MIN_VERSION = "4.4.0";

export type DependencyType = "zip" | "jar" | "car" | "inbound";

// Inbound connector modules follow the "mi-inbound-*" Maven artifact naming convention
// (e.g. mi-inbound-kafka, mi-inbound-cdc) — used to split them out of the regular
// connector dependency list and into their own tab.
const isInboundArtifact = (artifact: string | undefined) => !!artifact?.toLowerCase().startsWith('mi-inbound');

const LoaderContainer = styled.div`
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    color: white;
    z-index: 2001;
`;

const DialogMessage = styled.div`
    margin-bottom: 24px;
    text-align: center;
    line-height: 1.5;
    color: var(--vscode-foreground);
`;

const DialogActions = styled.div`
    display: flex;
    justify-content: center;
    gap: 12px;
`;

const SectionDivider = styled.div`
    border-top: 1px solid var(--vscode-panel-border);
    margin: 24px 0 12px;
`;

const SectionTitle = styled.div`
    font-size: 13px;
    font-weight: 600;
    color: var(--vscode-settings-headerForeground);
    margin-bottom: 12px;
`;

const FolderConnectorRow = styled.div`
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

const FolderConnectorInfo = styled.div`
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
`;

const FolderConnectorName = styled.span`
    font-size: 13px;
    font-weight: 600;
    color: var(--vscode-settings-headerForeground);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
`;

// 'list'    – the dependency list with an "Add Dependency" entry point
// 'manual'  – the GAV form (jar / fallback for all types)
// 'add'     – type-specific add panel (connector store / project options)
type AddMode = "list" | "manual" | "add";

// Where an add request originated. The "pom" and "workspace" options pull in a local integration
// project, so a resolution failure should advise building the project locally.
type AddDependencySource = "manual" | "pom" | "workspace" | undefined;

interface DependencyTabProps {
    type: DependencyType;
}

export function DependencyTab(props: DependencyTabProps) {
    const { type } = props;
    const { rpcClient } = useVisualizerContext();
    const [dependencies, setDependencies] = useState<DependencyDetails[]>([]);
    const [addMode, setAddMode] = useState<AddMode>("list");
    const [connectors, setConnectors] = useState(undefined as any[]);
    const [inboundConnectors, setInboundConnectors] = useState(undefined as any[]);
    const [isUpdating, setIsUpdating] = useState(false);
    const [isAddingDependency, setIsAddingDependency] = useState(false);
    const [duplicateError, setDuplicateError] = useState<string>('');
    const [showConfirmDialog, setShowConfirmDialog] = useState(false);
    const [confirmDialogMessage, setConfirmDialogMessage] = useState('');
    const [pendingDependency, setPendingDependency] = useState<{ groupId: string; artifact: string; version: string } | null>(null);
    const [pendingSource, setPendingSource] = useState<AddDependencySource>(undefined);
    const [existingDependencyToReplace, setExistingDependencyToReplace] = useState<DependencyDetails | null>(null);

    // Driver dependency state (only used when type === 'zip' and runtime >= 4.4.0)
    const [allConnectorDrivers, setAllConnectorDrivers] = useState<{ [id: string]: ConnectorEffectiveData }>({});
    const [supportsDriverManagement, setSupportsDriverManagement] = useState(false);

    // Connectors physically present in the project's connectors folder (only shown on the
    // zip and inbound tabs).
    const [folderConnectors, setFolderConnectors] = useState<ExternalConnectorDetail[]>([]);

    useEffect(() => {
        fetchDependencies();
        fetchConnectors();
        if (type === 'zip' || type === 'inbound') {
            fetchFolderConnectors();
        }
        if (type === 'inbound') {
            fetchInboundStoreConnectors();
        }
    }, []);

    const fetchDependencies = async () => {
        const projectDetails = await rpcClient.getMiVisualizerRpcClient().getProjectDetails();
        const runtimeVersion = projectDetails.primaryDetails?.runtimeVersion?.value;
        const driverManagementSupported = (type === 'zip' || type === 'inbound')
            && !!runtimeVersion
            && compareVersions(runtimeVersion, DRIVER_MANAGEMENT_MIN_VERSION) >= 0;
        setSupportsDriverManagement(driverManagementSupported);
        if (driverManagementSupported) {
            fetchDriverDependencies();
        }
        const dependencyList = type === 'zip' ?
            projectDetails.dependencies.connectorDependencies.filter(dep => !isInboundArtifact(dep.artifact)) : type === 'inbound' ?
            projectDetails.dependencies.connectorDependencies.filter(dep => isInboundArtifact(dep.artifact)) : type === 'car' ?
            projectDetails.dependencies.integrationProjectDependencies : projectDetails.dependencies.otherDependencies;
        setDependencies(dependencyList);
    };

    const fetchConnectors = async () => {
        try {
            if (navigator.onLine) {
                const response = await rpcClient.getMiDiagramRpcClient().getStoreConnectorJSON();
                setConnectors(response.connectors);
            } else {
                console.error('No internet connection. Unable to fetch available connector versions.');
            }
        } catch (error) {
            console.error('Error fetching connector versions:', error);
        }
    };

    const fetchDriverDependencies = async () => {
        try {
            const res = await rpcClient.getMiDiagramRpcClient().getConnectorDependencies({});
            setAllConnectorDrivers(res?.allConnectors ?? {});
        } catch (e) {
            console.error("Failed to fetch connector driver dependencies", e);
        }
    };

    const fetchFolderConnectors = async () => {
        try {
            const res = await rpcClient.getMiDiagramRpcClient().getExternalConnectorDetails();
            setFolderConnectors(res?.connectorDetails ?? []);
        } catch (e) {
            console.error("Failed to fetch connectors from the connectors folder", e);
        }
    };

    // Mirrors InboundEPWizard's own fetch (views/Forms/InboundEPform/index.tsx): the inbound
    // connector catalog is fetched directly from the store backend URL baked into the webview
    // bundle, rather than proxied through getStoreConnectorJSON — that's the path that's
    // actually kept up to date for inbound connectors.
    const fetchInboundStoreConnectors = async () => {
        try {
            const runtimeVersion = await rpcClient.getMiDiagramRpcClient().getMIVersionFromPom();
            const response = await fetch(
                process.env.MI_CONNECTOR_STORE_BACKEND_INBOUND_ENDPOINTS.replace('${version}', runtimeVersion.version)
            );
            const data = await response.json();
            setInboundConnectors(Array.isArray(data) ? data : []);
        } catch (e) {
            console.error("Failed to fetch inbound connectors from the store", e);
        }
    };

    // Refresh both the pom dependency list and the imported-connectors folder list.
    const refreshLists = async () => {
        await fetchDependencies();
        if (type === 'zip' || type === 'inbound') {
            await fetchFolderConnectors();
        }
    };

    const handleDeleteFolderConnector = async (connector: ExternalConnectorDetail) => {
        setIsUpdating(true);
        try {
            await rpcClient.getMiDiagramRpcClient().removeConnector({ connectorPath: connector.path });
            // Sync the pom with the connectors folder and reformat.
            await rpcClient.getMiVisualizerRpcClient().updateConnectorDependencies();
            await rpcClient.getMiDiagramRpcClient().formatPomFile();
            await fetchDependencies();
            await fetchFolderConnectors();
        } catch (e) {
            console.error("Failed to delete connector from the connectors folder", e);
        } finally {
            setIsUpdating(false);
        }
    };

    const handleDeleteDependency = async (dependency: DependencyDetails) => {
        setIsUpdating(true);

        await rpcClient.getMiVisualizerRpcClient().updatePomValues({
            pomValues: [{ range: dependency.range, value: '' }]
        });

        await rpcClient.getMiVisualizerRpcClient().reloadDependencies({ isProjectDependenciesUpdated: dependency.type === 'car' });
        await rpcClient.getMiDiagramRpcClient().formatPomFile();

        await fetchDependencies();

        setIsUpdating(false);
    };

    const handleEditDependency = async (
        prevDependency: DependencyDetails,
        updatedDependency: { groupId: string; artifact: string; version: string }
    ) => {
        setIsUpdating(true);

        const dependencyToUpdate = {
            ...prevDependency,
            groupId: updatedDependency.groupId,
            artifact: updatedDependency.artifact,
            version: updatedDependency.version
        };

        await rpcClient.getMiVisualizerRpcClient().updateDependenciesFromOverview({
            dependencies: [dependencyToUpdate]
        });
        await rpcClient.getMiVisualizerRpcClient().reloadDependencies({ isProjectDependenciesUpdated: dependencyToUpdate.type === 'car' });
        await rpcClient.getMiDiagramRpcClient().formatPomFile();

        await fetchDependencies();

        setIsUpdating(false);
    };

    const handleAddDependency = async (
        newDependency: { groupId: string; artifact: string; version: string },
        source: AddDependencySource = undefined
    ) => {
        setDuplicateError('');

        // Check for dependency duplicates (same groupId, artifactId, and version)
        const exactDuplicate = dependencies.some(
            dep => dep.groupId === newDependency.groupId &&
                dep.artifact === newDependency.artifact &&
                dep.version === newDependency.version
        );

        if (exactDuplicate) {
            setDuplicateError(`A dependency with Group ID "${newDependency.groupId}", Artifact ID "${newDependency.artifact}", and Version "${newDependency.version}" already exists.`);
            return;
        }

        // Check for same groupId and artifactId but different version
        const existingDependency = dependencies.find(
            dep => dep.groupId === newDependency.groupId &&
                dep.artifact === newDependency.artifact &&
                dep.version !== newDependency.version
        );

        if (existingDependency) {
            const message = `A dependency with Group ID "${existingDependency.groupId}" and Artifact ID "${existingDependency.artifact}" already exists with version "${existingDependency.version}".\n\nDo you want to overwrite it with version "${newDependency.version}"?`;

            setConfirmDialogMessage(message);
            setPendingDependency(newDependency);
            setPendingSource(source);
            setExistingDependencyToReplace(existingDependency);
            setShowConfirmDialog(true);
            return;
        }

        await addDependencyToProject(newDependency, source);
    };

    const addDependencyToProject = async (
        newDependency: { groupId: string; artifact: string; version: string },
        source: AddDependencySource = undefined
    ) => {
        setIsAddingDependency(true);
        setDuplicateError('');

        const addedDependency = {
            groupId: newDependency.groupId,
            artifact: newDependency.artifact,
            version: newDependency.version,
            // Inbound connector modules are still plain "zip" pom dependencies — "inbound" is
            // only a UI-level grouping, not a distinct backend dependency type.
            type: (type === 'inbound' ? 'zip' : type) as "zip" | "jar" | "car"
        };

        await rpcClient.getMiVisualizerRpcClient().updateDependenciesFromOverview({
            dependencies: [addedDependency]
        });

        const reloadDependenciesResult = await rpcClient.getMiVisualizerRpcClient().reloadDependencies({
            newDependencies: [addedDependency],
            isProjectDependenciesUpdated: addedDependency.type === 'car',
            fromLocalProjectSource: source === 'pom' || source === 'workspace'
        });
        await rpcClient.getMiDiagramRpcClient().formatPomFile();

        await fetchDependencies();

        setIsAddingDependency(false);
        if (reloadDependenciesResult) {
            setAddMode("list");
        }
    };

    const handleConfirmOverwrite = async (confirmed: boolean) => {
        setShowConfirmDialog(false);

        if (confirmed && pendingDependency && existingDependencyToReplace) {
            // Deleting the existing dependency
            setIsUpdating(true);

            await rpcClient.getMiVisualizerRpcClient().updatePomValues({
                pomValues: [{ range: existingDependencyToReplace.range, value: '' }]
            });

            await rpcClient.getMiVisualizerRpcClient().reloadDependencies({ isProjectDependenciesUpdated: existingDependencyToReplace.type === 'car' });
            await rpcClient.getMiDiagramRpcClient().formatPomFile();
            await fetchDependencies();

            setIsUpdating(false);

            // Adding the new dependency
            await addDependencyToProject(pendingDependency, pendingSource);
        }

        // Reset state
        setPendingDependency(null);
        setPendingSource(undefined);
        setExistingDependencyToReplace(null);
    };

    const closeAddPanel = () => {
        setAddMode("list");
        setDuplicateError('');
    };

    // Entry point for "Add Dependency": jar opens the manual form directly,
    // zip/car open their type-specific panels.
    const openAddPanel = () => {
        setDuplicateError('');
        setAddMode(type === 'jar' ? "manual" : "add");
    };

    if (addMode === "manual") {
        return (
            <DependencyForm
                groupId=""
                artifact=""
                version=""
                title="Add Dependency"
                showLoader={isAddingDependency}
                duplicateError={duplicateError}
                onClose={closeAddPanel}
                onUpdate={handleAddDependency}
            />
        );
    }

    if (addMode === "add" && (type === 'zip' || type === 'inbound')) {
        return (
            <ConnectorAddPanel
                connectors={type === 'inbound' ? inboundConnectors : connectors}
                existingDependencies={dependencies}
                isBusy={isAddingDependency}
                duplicateError={duplicateError}
                onClose={closeAddPanel}
                onManualSubmit={handleAddDependency}
                onChanged={refreshLists}
                inboundOnly={type === 'inbound'}
            />
        );
    }

    if (addMode === "add" && type === 'car') {
        return (
            <ProjectAddPanel
                existingDependencies={dependencies}
                isBusy={isAddingDependency}
                duplicateError={duplicateError}
                onClose={closeAddPanel}
                onSubmit={handleAddDependency}
            />
        );
    }

    // The connectors folder holds both regular and inbound connector zips; only show the ones
    // matching the current tab.
    const visibleFolderConnectors = folderConnectors.filter(c =>
        type === 'inbound' ? c.type === 'inbound' : c.type === 'connector'
    );

    return (
        <>
            <div style={{ marginTop: '10px' }}>
                <LinkButton
                    sx={{ padding: '0 5px', margin: '20px 0' }}
                    onClick={openAddPanel}
                >
                    <Codicon name="add" />
                    Add Dependency
                </LinkButton>
                {dependencies.length === 0 ? (
                    <Typography>No dependencies found</Typography>
                ) : (
                    <div>
                        {dependencies.map((dependency, index) => (
                            <DependencyItem
                                key={`${dependency.groupId}-${dependency.artifact}-${index}`}
                                onEdit={(updatedDependency) =>
                                    handleEditDependency(dependency, updatedDependency)
                                }
                                onDelete={(dependency) => handleDeleteDependency(dependency)}
                                onClose={() => setAddMode("list")}
                                dependency={dependency}
                                connectors={type === 'inbound' ? inboundConnectors : connectors}
                                inboundConnectors={inboundConnectors}
                                driverData={supportsDriverManagement ? allConnectorDrivers[dependency.artifact] : undefined}
                                onDriverUpdated={supportsDriverManagement ? fetchDriverDependencies : undefined}
                            />
                        ))}
                    </div>
                )}
            </div>

            {(type === 'zip' || type === 'inbound') && visibleFolderConnectors.length > 0 && (
                <div>
                    <SectionDivider />
                    <SectionTitle>{type === 'inbound' ? 'Imported Inbound Connectors' : 'Imported Connectors'}</SectionTitle>
                    {visibleFolderConnectors.map((connector) => (
                        <FolderConnectorRow key={connector.path}>
                            <FolderConnectorInfo>
                                <Codicon name="package" sx={{ color: 'var(--vscode-badge-background)' }} iconSx={{ fontSize: 18 }} />
                                <FolderConnectorName>{connector.name}</FolderConnectorName>
                            </FolderConnectorInfo>
                            <Button
                                appearance="icon"
                                onClick={() => handleDeleteFolderConnector(connector)}
                                tooltip="Delete connector"
                                buttonSx={{ color: 'var(--vscode-charts-red)' }}
                            >
                                <Codicon name="trash" />
                            </Button>
                        </FolderConnectorRow>
                    ))}
                </div>
            )}

            {isUpdating && (
                <>
                    <Overlay sx={{ background: `${Colors.SURFACE_CONTAINER}`, opacity: `0.3`, zIndex: 2000 }} />
                    <LoaderContainer data-testid="dependency-manager-loader">
                        <ProgressRing sx={{ height: '32px', width: '32px' }} />
                    </LoaderContainer>
                </>
            )}

            <Dialog
                isOpen={showConfirmDialog}
                onClose={() => handleConfirmOverwrite(false)}
                sx={{ width: '400px', padding: '24px' }}
            >
                <DialogMessage>{confirmDialogMessage}</DialogMessage>
                <DialogActions>
                    <Button appearance="secondary" onClick={() => handleConfirmOverwrite(false)}>
                        Cancel
                    </Button>
                    <Button appearance="primary" onClick={() => handleConfirmOverwrite(true)}>
                        Overwrite
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
