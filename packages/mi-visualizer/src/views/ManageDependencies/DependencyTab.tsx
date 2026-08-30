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

import { useEffect, useState } from "react";
import { ConnectorEffectiveData, DependenciesDetails, DependencyDetails, ExternalConnectorDetail } from "@wso2/mi-core";
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
const INBOUND_STORE_FETCH_TIMEOUT_MS = 15000;

export type DependencyType = "zip" | "jar" | "car" | "inbound";

// Inbound endpoint modules follow the "mi-inbound-*" Maven artifact naming convention
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

type AddMode = "list" | "manual" | "add";

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

    const [allConnectorDrivers, setAllConnectorDrivers] = useState<{ [id: string]: ConnectorEffectiveData }>({});
    const [supportsDriverManagement, setSupportsDriverManagement] = useState(false);

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

    const selectDependencyList = (dependenciesDetails: DependenciesDetails): DependencyDetails[] =>
        type === 'zip' ?
            dependenciesDetails.connectorDependencies.filter(dep => !isInboundArtifact(dep.artifact)) : type === 'inbound' ?
            dependenciesDetails.connectorDependencies.filter(dep => isInboundArtifact(dep.artifact)) : type === 'car' ?
            dependenciesDetails.integrationProjectDependencies : dependenciesDetails.otherDependencies;

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
        setDependencies(selectDependencyList(projectDetails.dependencies));
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

    const fetchInboundStoreConnectors = async () => {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), INBOUND_STORE_FETCH_TIMEOUT_MS);
        try {
            const runtimeVersion = await rpcClient.getMiDiagramRpcClient().getMIVersionFromPom();
            const response = await fetch(
                process.env.MI_CONNECTOR_STORE_BACKEND_INBOUND_ENDPOINTS.replace('${version}', runtimeVersion.version),
                { signal: controller.signal }
            );
            const data = await response.json();
            setInboundConnectors(Array.isArray(data) ? data : []);
        } catch (e) {
            console.error("Failed to fetch inbound endpoints from the store", e);
            // Recover the panel from the loading state rather than leaving it stuck on a stalled/failed fetch.
            setInboundConnectors([]);
        } finally {
            clearTimeout(timeoutId);
        }
    };

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
        try {
            await rpcClient.getMiVisualizerRpcClient().updatePomValues({
                pomValues: [{ range: dependency.range, value: '' }]
            });

            await rpcClient.getMiVisualizerRpcClient().reloadDependencies({ isProjectDependenciesUpdated: dependency.type === 'car' });
            await rpcClient.getMiDiagramRpcClient().formatPomFile();

            await fetchDependencies();
        } catch (e) {
            console.error("Failed to delete dependency", e);
        } finally {
            setIsUpdating(false);
        }
    };

    const handleEditDependency = async (
        prevDependency: DependencyDetails,
        updatedDependency: { groupId: string; artifact: string; version: string }
    ): Promise<boolean> => {
        setIsUpdating(true);

        const dependencyToUpdate = {
            ...prevDependency,
            groupId: updatedDependency.groupId,
            artifact: updatedDependency.artifact,
            version: updatedDependency.version
        };

        try {
            await rpcClient.getMiVisualizerRpcClient().updateDependenciesFromOverview({
                dependencies: [dependencyToUpdate]
            });
            const reloadDependenciesResult = await rpcClient.getMiVisualizerRpcClient().reloadDependencies({
                newDependencies: [dependencyToUpdate],
                isProjectDependenciesUpdated: dependencyToUpdate.type === 'car'
            });

            if (!reloadDependenciesResult) {
                await rpcClient.getMiVisualizerRpcClient().updateDependenciesFromOverview({
                    dependencies: [{
                        groupId: prevDependency.groupId,
                        artifact: prevDependency.artifact,
                        version: prevDependency.version,
                        type: prevDependency.type
                    }]
                });
                await rpcClient.getMiVisualizerRpcClient().reloadDependencies({
                    isProjectDependenciesUpdated: prevDependency.type === 'car'
                });
            }

            await rpcClient.getMiDiagramRpcClient().formatPomFile();

            await fetchDependencies();

            return reloadDependenciesResult;
        } catch (e) {
            console.error("Failed to edit dependency", e);
            return false;
        } finally {
            setIsUpdating(false);
        }
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
            type: (type === 'inbound' ? 'zip' : type) as "zip" | "jar" | "car"
        };

        try {
            await rpcClient.getMiVisualizerRpcClient().updateDependenciesFromOverview({
                dependencies: [addedDependency]
            });

            const reloadDependenciesResult = await rpcClient.getMiVisualizerRpcClient().reloadDependencies({
                newDependencies: [addedDependency],
                isProjectDependenciesUpdated: addedDependency.type === 'car',
                fromLocalProjectSource: source === 'pom' || source === 'workspace'
            });

            if (!reloadDependenciesResult) {
                const projectDetails = await rpcClient.getMiVisualizerRpcClient().getProjectDetails();
                const persistedDependency = selectDependencyList(projectDetails.dependencies).find(
                    dep => dep.groupId === addedDependency.groupId && dep.artifact === addedDependency.artifact
                );
                if (persistedDependency?.range) {
                    await rpcClient.getMiVisualizerRpcClient().updatePomValues({
                        pomValues: [{ range: persistedDependency.range, value: '' }]
                    });
                }
                rpcClient.getMiVisualizerRpcClient().showNotification({
                    message: `Failed to add dependency "${addedDependency.artifact}".`,
                    type: "error"
                });
            }

            await rpcClient.getMiDiagramRpcClient().formatPomFile();

            await fetchDependencies();

            if (reloadDependenciesResult) {
                setAddMode("list");
            }
        } catch (e) {
            console.error("Failed to add dependency", e);
        } finally {
            setIsAddingDependency(false);
        }
    };

    const handleConfirmOverwrite = async (confirmed: boolean) => {
        setShowConfirmDialog(false);

        if (confirmed && pendingDependency && existingDependencyToReplace) {
            // Deleting the existing dependency
            setIsUpdating(true);
            let deleteSucceeded = false;
            try {
                await rpcClient.getMiVisualizerRpcClient().updatePomValues({
                    pomValues: [{ range: existingDependencyToReplace.range, value: '' }]
                });

                await rpcClient.getMiVisualizerRpcClient().reloadDependencies({ isProjectDependenciesUpdated: existingDependencyToReplace.type === 'car' });
                await rpcClient.getMiDiagramRpcClient().formatPomFile();
                await fetchDependencies();
                deleteSucceeded = true;
            } catch (e) {
                console.error("Failed to remove the existing dependency before overwrite", e);
            } finally {
                setIsUpdating(false);
            }

            // Adding the new dependency, only if the existing one was removed successfully
            if (deleteSucceeded) {
                await addDependencyToProject(pendingDependency, pendingSource);
            }
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
                        {dependencies.map((dependency) => (
                            <DependencyItem
                                key={`${dependency.groupId}-${dependency.artifact}`}
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
                    <SectionTitle>{type === 'inbound' ? 'Imported Inbound Endpoints' : 'Imported Connectors'}</SectionTitle>
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
