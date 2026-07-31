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

import { useState } from "react";
import styled from "@emotion/styled";
import { ConnectorEffectiveData, ConnectorEffectiveDependency, DependencyDetails } from "@wso2/mi-core";
import { Button, Codicon, Dialog, Tooltip } from "@wso2/ui-toolkit";
import { compareVersions } from "@wso2/mi-diagram/lib/utils/commons";
import { useForm } from "react-hook-form";
import { useVisualizerContext } from "@wso2/mi-rpc-client";
import { VSCodeTextField } from "@vscode/webview-ui-toolkit/react";
import { DependencyForm } from "./DependencyForm";

// ─── Connector row styled components ────────────────────────────────────────

const Container = styled.div`
    padding: 12px 14px;
    border: 1.5px solid var(--vscode-dropdown-border);
    border-radius: 8px;
    background-color: var(--vscode-menu-background);
    transition: background-color 0.2s ease;
    display: flex;
    flex-direction: column;

    &:hover {
        border: 1.5px solid var(--vscode-button-background);
        .action-button-container { opacity: 1 !important; }
        .dependency-artifact { color: var(--vscode-button-background); }
        .update-text { opacity: 1 !important; }
    }
    margin-bottom: 8px;
`;

const ConnectorRow = styled.div`
    display: flex;
    flex-direction: row;
`;

const DependencyTitle = styled.div`
    font-size: 13px;
    font-weight: 700;
    min-height: 24px;
    color: var(--vscode-settings-headerForeground);
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    margin-bottom: 5px;
    flex-wrap: nowrap;
`;

const DependencyDetailsContainer = styled.div`
    display: flex;
    flex-direction: column;
    gap: 5px;
`;

const IconContainer = styled.div`
    align-self: center;
    width: 32px;
`;

const DependencyField = styled.div`
    display: flex;
    align-items: center;
    font-size: 12px;
    color: var(--vscode-descriptionForeground);
    gap: 15px;

    .label {
        font-weight: 500;
        flex-shrink: 0;
    }

    .value {
        font-family: monospace;
        background-color: var(--vscode-badge-background);
        color: var(--vscode-badge-foreground);
        border-radius: 3px;
        font-size: 11px;
        padding: 2px 4px;
    }

    .group {
        font-family: monospace;
    }
`;

const OmittedBadge = styled.span`
    font-size: 11px;
    padding: 2px 6px;
    border-radius: 3px;
    background-color: var(--vscode-inputValidation-errorBackground);
    color: var(--vscode-inputValidation-errorForeground);
    margin-left: 6px;
    font-weight: 400;
`;

const DriverToggleButton = styled.button<{ color?: string }>`
    display: inline-flex;
    align-items: center;
    gap: 4px;
    background: none;
    border: none;
    cursor: pointer;
    padding: 3px 8px 3px 4px;
    border-radius: 4px;
    font-size: 11px;
    color: ${(p: { color?: string }) => p.color ?? 'var(--vscode-descriptionForeground)'};
    opacity: 0.85;

    &:hover {
        background-color: var(--vscode-toolbar-hoverBackground);
        opacity: 1;
    }

    &:disabled {
        opacity: 0.4;
        cursor: default;
    }
`;

// ─── Driver panel styled components ─────────────────────────────────────────

const DriverDivider = styled.div`
    border-top: 1px dashed var(--vscode-dropdown-border);
    margin: 10px 0 10px;
`;

const DriverPanelHeader = styled.div`
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
`;

const DriverLabel = styled.span`
    font-size: 11px;
    color: var(--vscode-descriptionForeground);
    opacity: 0.7;
    font-style: italic;
`;

const DriverDepRow = styled.div`
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 6px 10px;
    border-radius: 5px;
    background-color: var(--vscode-editor-background);
    margin-bottom: 4px;
    min-height: 32px;

    &:hover .driver-actions {
        opacity: 1 !important;
    }
`;

const DriverDepName = styled.span<{ muted?: boolean }>`
    font-size: 12px;
    font-weight: 600;
    color: var(--vscode-settings-headerForeground);
    opacity: ${(p: { muted?: boolean }) => p.muted ? 0.45 : 1};
`;

const DriverBadge = styled.span<{ variant?: 'override' | 'omit' | 'default' }>`
    font-family: monospace;
    font-size: 11px;
    padding: 2px 5px;
    border-radius: 3px;
    background-color: ${(p: { variant?: string }) =>
        p.variant === 'omit'     ? 'var(--vscode-inputValidation-errorBackground)' :
        p.variant === 'override' ? 'var(--vscode-charts-blue)' :
        'var(--vscode-badge-background)'};
    color: ${(p: { variant?: string }) =>
        p.variant === 'omit' ? 'var(--vscode-inputValidation-errorForeground)' :
        'var(--vscode-badge-foreground)'};
`;

const DriverActions = styled.div`
    display: flex;
    gap: 2px;
    opacity: 0;
    transition: opacity 0.15s ease;
    flex-shrink: 0;
`;

const DialogMessage = styled.div`
    margin-bottom: 20px;
    line-height: 1.5;
    color: var(--vscode-foreground);
`;

const DialogActions = styled.div`
    display: flex;
    justify-content: center;
    gap: 12px;
`;

// ─── Props / types ───────────────────────────────────────────────────────────

interface DependencyFormData {
    groupId: string;
    artifact: string;
    version: string;
}

interface DependencyItemProps {
    dependency: DependencyDetails;
    onClose: () => void;
    // Returning false keeps the edit form opened when new coordinates could not be resolved
    onEdit?: (updatedDependency: { groupId: string; artifact: string; version: string }) => Promise<boolean> | boolean | void;
    onDelete?: (dependency: DependencyDetails) => void;
    connectors?: any[];
    inboundConnectors?: any[];
    // Driver props — provided only for connector ZIP deps
    driverData?: ConnectorEffectiveData;
    onDriverUpdated?: () => Promise<void>;
}

// ─── Component ───────────────────────────────────────────────────────────────

export function DependencyItem(props: DependencyItemProps) {
    const { dependency, onEdit, onDelete, connectors, driverData, onDriverUpdated } = props;
    const { rpcClient } = useVisualizerContext();

    // Connector ZIP edit state
    const [isEditFormOpen, setIsEditFormOpen] = useState(false);

    // Driver panel state
    const [isDriverPanelOpen, setIsDriverPanelOpen] = useState(false);
    const [showInactiveDeps, setShowInactiveDeps] = useState(false);
    const [driverEditState, setDriverEditState] = useState<{
        connectionType: string | undefined;
        groupId: string | undefined;
        artifactId: string | undefined;
        // true for user-added dependencies, which expose all three maven coordinates
        isAdded: boolean;
        // editable field values.
        groupIdValue: string;
        artifactIdValue: string;
        value: string;
    } | null>(null);
    const [confirmOmitDriver, setConfirmOmitDriver] = useState<{
        connectionType: string | undefined;
        groupId: string | undefined;
        artifactId: string | undefined;
    } | null>(null);
    const [confirmOmitAllDrivers, setConfirmOmitAllDrivers] = useState(false);
    const [addDriverForm, setAddDriverForm] = useState<{
        groupId: string;
        artifactId: string;
        version: string;
    } | null>(null);
    const [isSaving, setIsSaving] = useState(false);

    const { reset } = useForm<DependencyFormData>({
        defaultValues: {
            groupId: dependency.groupId,
            artifact: dependency.artifact,
            version: dependency.version
        }
    });

    // ── Connector ZIP helpers ────────────────────────────────────────────────

    const findLatestVersion = () => {
        if (!connectors || connectors.length === 0) return null;
        const match = connectors.find(c =>
            c.mavenGroupId === dependency.groupId && c.mavenArtifactId === dependency.artifact
        );
        return match?.version?.tagName || null;
    };

    const latestVersion = findLatestVersion();

    const handleEditDependencyClick = () => {
        reset({ groupId: dependency.groupId, artifact: dependency.artifact, version: dependency.version });
        setIsEditFormOpen(true);
    };

    const handleEditDependencySave = async (updated: { groupId: string; artifact: string; version: string }) => {
        const result = await onEdit?.(updated);
        if (result !== false) {
            setIsEditFormOpen(false);
        }
    };

    // ── Driver helpers ───────────────────────────────────────────────────────

    const connectorArtifactId = dependency.artifact;
    const driverDeps = driverData?.dependencies ?? [];
    // A manually-added dependency isn't part of the descriptor defaults, so it has no defaultVersion.
    const isManuallyAdded = (d: ConnectorEffectiveDependency) =>
        !d.defaultVersion && (d.isOverridden || !!d.overriddenVersion || !!d.localPath);
    const activeDeps = driverDeps.filter(d => d.isConnectionTypeActive !== false || isManuallyAdded(d));
    const inactiveDeps = driverDeps.filter(d => d.isConnectionTypeActive === false && !isManuallyAdded(d));
    const driverCount = activeDeps.length;

    const driverDepLabel = (dep: ConnectorEffectiveDependency) => {
        if (dep.connectionType) return dep.connectionType;
        if (dep.artifactId) return dep.groupId ? `${dep.groupId}: ${dep.artifactId}` : dep.artifactId;
        return 'driver';
    };

    const handleDriverEditSave = async () => {
        if (!driverEditState) return;
        const version = driverEditState.value.trim();

        if (driverEditState.isAdded) {
            // Added deps allow editing all coordinates. Validate the full set.
            const newGroupId = driverEditState.groupIdValue.trim();
            const newArtifactId = driverEditState.artifactIdValue.trim();
            if (!newGroupId || !newArtifactId || !version) return;

            const coordsChanged = newGroupId !== (driverEditState.groupId ?? '')
                || newArtifactId !== (driverEditState.artifactId ?? '');

            setIsSaving(true);
            try {
                if (coordsChanged) {
                    await rpcClient.getMiDiagramRpcClient().resetConnectorDependencyOverrides({
                        connectorArtifactId,
                        groupId: driverEditState.groupId,
                        artifactId: driverEditState.artifactId,
                    });
                }
                await rpcClient.getMiDiagramRpcClient().updateConnectorDependencyOverride({
                    connectorArtifactId,
                    groupId: newGroupId,
                    artifactId: newArtifactId,
                    version,
                    localPath: '',
                    additionalDependency: true,
                });
                setDriverEditState(null);
                await onDriverUpdated?.();
            } catch (e) {
                console.error("Failed to update driver dependency override", e);
                rpcClient.getMiVisualizerRpcClient().showNotification({
                    message: "Failed to update the driver dependency.",
                    type: "error"
                });
            } finally {
                setIsSaving(false);
            }
            return;
        }

        if (!version) return;
        setIsSaving(true);
        try {
            await rpcClient.getMiDiagramRpcClient().updateConnectorDependencyOverride({
                connectorArtifactId,
                connectionType: driverEditState.connectionType,
                groupId: driverEditState.groupId,
                artifactId: driverEditState.artifactId,
                version,
                localPath: '',
            });
            setDriverEditState(null);
            await onDriverUpdated?.();
        } finally {
            setIsSaving(false);
        }
    };

    const handleAddDriverSave = async () => {
        if (!addDriverForm) return;
        const groupId = addDriverForm.groupId.trim();
        const artifactId = addDriverForm.artifactId.trim();
        const version = addDriverForm.version.trim();
        if (!groupId || !artifactId || !version) return;
        setIsSaving(true);
        try {
            await rpcClient.getMiDiagramRpcClient().updateConnectorDependencyOverride({
                connectorArtifactId,
                groupId,
                artifactId,
                version,
                localPath: '',
                additionalDependency: true,
            });
            setAddDriverForm(null);
            await onDriverUpdated?.();
        } finally {
            setIsSaving(false);
        }
    };

    const handleBrowseLocalJar = async (dep: ConnectorEffectiveDependency) => {
        const result = await rpcClient.getMiDiagramRpcClient().askDriverPath();
        if (!result?.path) return;
        setIsSaving(true);
        try {
            await rpcClient.getMiDiagramRpcClient().updateConnectorDependencyOverride({
                connectorArtifactId,
                connectionType: dep.connectionType,
                groupId: dep.groupId,
                artifactId: dep.artifactId,
                localPath: result.path,
                version: '',
            });
            await onDriverUpdated?.();
        } finally {
            setIsSaving(false);
        }
    };

    const handleDriverOmitConfirm = async (confirmed: boolean) => {
        if (confirmed && confirmOmitDriver) {
            setIsSaving(true);
            try {
                await rpcClient.getMiDiagramRpcClient().updateConnectorDependencyOverride({
                    connectorArtifactId,
                    connectionType: confirmOmitDriver.connectionType,
                    groupId: confirmOmitDriver.groupId,
                    artifactId: confirmOmitDriver.artifactId,
                    omit: true,
                });
                await onDriverUpdated?.();
            } finally {
                setIsSaving(false);
            }
        }
        setConfirmOmitDriver(null);
    };

    const handleDriverReset = async (connectionType: string | undefined, groupId?: string, artifactId?: string) => {
        setIsSaving(true);
        try {
            await rpcClient.getMiDiagramRpcClient().resetConnectorDependencyOverrides({
                connectorArtifactId,
                connectionType,
                groupId,
                artifactId,
            });
            await onDriverUpdated?.();
        } finally {
            setIsSaving(false);
        }
    };

    const handleOmitAllDriversConfirm = async (confirmed: boolean) => {
        if (confirmed) {
            setIsSaving(true);
            try {
                // Reset individual overrides first, then set the connector-level flag.
                // Order matters: resetConnectorDependencyOverrides (no connectionType) removes the
                // entire connector entry, so updateConnectorFlags must come after to re-create it.
                await rpcClient.getMiDiagramRpcClient().resetConnectorDependencyOverrides({
                    connectorArtifactId,
                });
                await rpcClient.getMiDiagramRpcClient().updateConnectorFlags({
                    connectorArtifactId,
                    omitAllDrivers: true,
                });
                await onDriverUpdated?.();
            } finally {
                setIsSaving(false);
            }
        }
        setConfirmOmitAllDrivers(false);
    };

    const handleResetAllDrivers = async () => {
        setIsSaving(true);
        try {
            await rpcClient.getMiDiagramRpcClient().updateConnectorFlags({
                connectorArtifactId,
                omitAllDrivers: false,
            });
            await rpcClient.getMiDiagramRpcClient().resetConnectorDependencyOverrides({
                connectorArtifactId,
            });
            await onDriverUpdated?.();
        } finally {
            setIsSaving(false);
        }
    };

    const handleConnectorOmitToggle = async () => {
        setIsSaving(true);
        try {
            await rpcClient.getMiDiagramRpcClient().updateConnectorFlags({
                connectorArtifactId,
                omit: !driverData?.omit,
            });
            await onDriverUpdated?.();
        } finally {
            setIsSaving(false);
        }
    };

    // ── Render ───────────────────────────────────────────────────────────────

    if (isEditFormOpen) {
        return (
            <DependencyForm
                groupId={dependency.groupId}
                artifact={dependency.artifact}
                version={dependency.version}
                title="Edit Dependency"
                onClose={() => setIsEditFormOpen(false)}
                onUpdate={handleEditDependencySave}
            />
        );
    }

    return (
        <>
            <Container
                key={`${dependency.groupId}-${dependency.artifact}-${dependency.version}`}
                data-testid={`${dependency.groupId}-${dependency.artifact}-${dependency.version}`}
            >
                {/* ── Connector row ── */}
                <ConnectorRow>
                    <IconContainer>
                        <Codicon
                            name="package"
                            sx={{ color: driverData?.omit ? 'var(--vscode-disabledForeground)' : 'var(--vscode-badge-background)' }}
                            iconSx={{ fontSize: 20 }}
                        />
                    </IconContainer>
                    <div style={{ display: 'flex', flexDirection: 'column', width: '100%' }}>
                        <DependencyTitle>
                            <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
                                <div style={{ display: 'flex', alignItems: 'center' }}>
                                    <span
                                        className="dependency-artifact"
                                        style={{ opacity: driverData?.omit ? 0.45 : 1 }}
                                    >
                                        {dependency.artifact}:
                                    </span>
                                </div>
                                <DependencyField>
                                    <span className="value">{dependency.version}</span>
                                </DependencyField>
                                {driverData?.omit && <OmittedBadge>omitted</OmittedBadge>}
                                {latestVersion && compareVersions(latestVersion, dependency.version) > 0 && (
                                    <div style={{ display: 'flex', gap: '5px', alignItems: 'center' }}>
                                        <Tooltip content="A new version is available">
                                            <div style={{ display: 'flex', alignItems: 'center' }}>
                                                <Codicon name="warning" sx={{ marginLeft: 5, fontSize: '0.8em', color: 'var(--vscode-editorWarning-foreground)' }} />
                                                <span style={{ marginLeft: '4px', fontSize: '11px', fontWeight: '300', color: 'var(--vscode-editorWarning-foreground)', opacity: 0, transition: 'opacity 0.2s ease' }} className="update-text">
                                                    Update available: {latestVersion}
                                                </span>
                                            </div>
                                        </Tooltip>
                                        <div className="action-button-container" style={{ opacity: 0, transition: 'opacity 0.2s ease' }}>
                                            <Button appearance="icon" onClick={() => onEdit?.({ groupId: dependency.groupId, artifact: dependency.artifact, version: latestVersion })} tooltip="Update Dependency" buttonSx={{ color: 'var(--vscode-charts-blue)' }}>
                                                <Codicon name="sync" />
                                            </Button>
                                        </div>
                                    </div>
                                )}
                            </div>
                            <div style={{ display: 'flex', gap: '2px', alignItems: 'center', flexShrink: 0 }}>
                                {/* Connector omit toggle — only for zip deps */}
                                {driverData !== undefined && (
                                    <div className="action-button-container" style={{ opacity: 0, transition: 'opacity 0.2s ease' }}>
                                        <Button
                                            appearance="icon"
                                            onClick={handleConnectorOmitToggle}
                                            tooltip={driverData.omit ? "Re-enable connector packing" : "Omit connector from CAR"}
                                            buttonSx={{ color: driverData.omit ? 'var(--vscode-charts-green)' : 'var(--vscode-editorWarning-foreground)' }}
                                            disabled={isSaving}
                                        >
                                            <Codicon name={driverData.omit ? "plug" : "debug-disconnect"} />
                                        </Button>
                                    </div>
                                )}
                                <div className="action-button-container" style={{ opacity: 0, transition: 'opacity 0.2s ease' }}>
                                    <Button appearance="icon" onClick={handleEditDependencyClick} tooltip="Edit Dependency" buttonSx={{ color: 'var(--vscode-charts-green)' }}>
                                        <Codicon name="edit" />
                                    </Button>
                                </div>
                                <div className="action-button-container" style={{ opacity: 0, transition: 'opacity 0.2s ease' }}>
                                    <Button appearance="icon" onClick={() => onDelete?.(dependency)} tooltip="Remove Dependency" buttonSx={{ color: 'var(--vscode-charts-red)' }}>
                                        <Codicon name="trash" />
                                    </Button>
                                </div>
                            </div>
                        </DependencyTitle>
                        <DependencyDetailsContainer>
                            <DependencyField>
                                <span className="label">Group ID:</span>
                                <span className="group">{dependency.groupId}</span>
                            </DependencyField>
                        </DependencyDetailsContainer>
                        {/* Driver toggle — second row, below Group ID */}
                        {driverData !== undefined && driverCount > 0 && (
                            <div style={{ marginTop: '4px' }}>
                                <DriverToggleButton onClick={() => setIsDriverPanelOpen(!isDriverPanelOpen)}>
                                    <Codicon name={isDriverPanelOpen ? "chevron-down" : "chevron-right"} />
                                    <span>Drivers ({driverCount})</span>
                                </DriverToggleButton>
                            </div>
                        )}
                    </div>
                </ConnectorRow>

                {/* ── Driver panel (expandable) ── */}
                {driverData !== undefined && isDriverPanelOpen && driverCount > 0 && (
                    <>
                        <DriverDivider />
                        <DriverPanelHeader>
                            <DriverLabel>from descriptor.yml</DriverLabel>
                            <div style={{ display: 'flex', gap: '4px' }}>
                                <DriverToggleButton
                                    color="var(--vscode-charts-blue)"
                                    onClick={() => setAddDriverForm({ groupId: '', artifactId: '', version: '' })}
                                    disabled={isSaving || addDriverForm !== null}
                                >
                                    <Codicon name="add" />
                                    <span>Add</span>
                                </DriverToggleButton>
                                {!driverData.omitAllDrivers && (
                                    <DriverToggleButton
                                        color="var(--vscode-editorWarning-foreground)"
                                        onClick={() => setConfirmOmitAllDrivers(true)}
                                        disabled={isSaving}
                                    >
                                        <Codicon name="circle-slash" />
                                        <span>Omit All</span>
                                    </DriverToggleButton>
                                )}
                                {(driverData.omitAllDrivers || driverDeps.some(d => d.isOverridden || d.omit)) && (
                                    <DriverToggleButton
                                        color="var(--vscode-charts-green)"
                                        onClick={handleResetAllDrivers}
                                        disabled={isSaving}
                                    >
                                        <Codicon name="discard" />
                                        <span>Reset All</span>
                                    </DriverToggleButton>
                                )}
                            </div>
                        </DriverPanelHeader>

                        {addDriverForm && (
                            <DriverDepRow style={{ flexDirection: 'column', alignItems: 'stretch', gap: '8px' }}>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    <VSCodeTextField
                                        value={addDriverForm.groupId}
                                        placeholder="com.example"
                                        onInput={(e: any) => setAddDriverForm({ ...addDriverForm, groupId: e.target.value })}
                                        style={{ width: '100%' }}
                                    >
                                        Group ID
                                    </VSCodeTextField>
                                    <VSCodeTextField
                                        value={addDriverForm.artifactId}
                                        placeholder="my-dependency"
                                        onInput={(e: any) => setAddDriverForm({ ...addDriverForm, artifactId: e.target.value })}
                                        style={{ width: '100%' }}
                                    >
                                        Artifact ID
                                    </VSCodeTextField>
                                    <VSCodeTextField
                                        value={addDriverForm.version}
                                        placeholder="1.0.0"
                                        onInput={(e: any) => setAddDriverForm({ ...addDriverForm, version: e.target.value })}
                                        style={{ width: '100%' }}
                                    >
                                        Version
                                    </VSCodeTextField>
                                </div>
                                <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end' }}>
                                    <Button
                                        appearance="primary"
                                        onClick={handleAddDriverSave}
                                        disabled={isSaving || !addDriverForm.groupId.trim() || !addDriverForm.artifactId.trim() || !addDriverForm.version.trim()}
                                    >
                                        Add
                                    </Button>
                                    <Button appearance="secondary" onClick={() => setAddDriverForm(null)}>Cancel</Button>
                                </div>
                            </DriverDepRow>
                        )}

                        {driverData.omitAllDrivers && (
                            <div style={{ fontSize: '11px', color: 'var(--vscode-inputValidation-errorForeground)', background: 'var(--vscode-inputValidation-errorBackground)', padding: '4px 8px', borderRadius: '4px', marginBottom: '6px' }}>
                                All drivers omitted for this connector
                            </div>
                        )}

                        {activeDeps.map((dep, idx) => {
                            const label = driverDepLabel(dep);
                            const effectiveVersion = dep.overriddenVersion ?? dep.defaultVersion ?? '';
                            const isEditing = driverEditState?.connectionType === dep.connectionType
                                && driverEditState?.groupId === dep.groupId
                                && driverEditState?.artifactId === dep.artifactId;
                            const isEffectivelyOmitted = dep.omit || driverData.omitAllDrivers;
                            const isAddedDep = !dep.defaultVersion;

                            return (
                                <DriverDepRow key={`${label}-${idx}`}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', flex: 1 }}>
                                        <DriverDepName muted={isEffectivelyOmitted}>{label}</DriverDepName>
                                        {isEffectivelyOmitted ? (
                                            <DriverBadge variant="omit">omitted</DriverBadge>
                                        ) : dep.localPath ? (
                                            <>
                                                <DriverBadge title={dep.localPath} style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                                    📁 {dep.localPath.split(/[\\/]/).pop()}
                                                </DriverBadge>
                                                <span style={{ fontSize: '11px', opacity: 0.55 }}>local JAR</span>
                                            </>
                                        ) : dep.isOverridden && dep.defaultVersion ? (
                                            <>
                                                <DriverBadge>{effectiveVersion}</DriverBadge>
                                                <span style={{ fontSize: '11px', opacity: 0.55 }}>default: {dep.defaultVersion}</span>
                                            </>
                                        ) : dep.isOverridden ? (
                                            <>
                                                <DriverBadge>{effectiveVersion}</DriverBadge>
                                                <span style={{ fontSize: '11px', opacity: 0.55 }}>added</span>
                                            </>
                                        ) : (
                                            <DriverBadge>{effectiveVersion}</DriverBadge>
                                        )}

                                        {isEditing && (driverEditState.isAdded ? (
                                            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '4px', width: '100%' }}>
                                                <VSCodeTextField
                                                    value={driverEditState.groupIdValue}
                                                    placeholder="com.example"
                                                    onInput={(e: any) => setDriverEditState({ ...driverEditState, groupIdValue: e.target.value })}
                                                    style={{ width: '100%' }}
                                                >
                                                    Group ID
                                                </VSCodeTextField>
                                                <VSCodeTextField
                                                    value={driverEditState.artifactIdValue}
                                                    placeholder="my-dependency"
                                                    onInput={(e: any) => setDriverEditState({ ...driverEditState, artifactIdValue: e.target.value })}
                                                    style={{ width: '100%' }}
                                                >
                                                    Artifact ID
                                                </VSCodeTextField>
                                                <VSCodeTextField
                                                    value={driverEditState.value}
                                                    placeholder="1.0.0"
                                                    onInput={(e: any) => setDriverEditState({ ...driverEditState, value: e.target.value })}
                                                    style={{ width: '100%' }}
                                                >
                                                    Version
                                                </VSCodeTextField>
                                                <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end' }}>
                                                    <Button
                                                        appearance="primary"
                                                        onClick={handleDriverEditSave}
                                                        disabled={isSaving || !driverEditState.groupIdValue.trim() || !driverEditState.artifactIdValue.trim() || !driverEditState.value.trim()}
                                                    >
                                                        Save
                                                    </Button>
                                                    <Button appearance="secondary" onClick={() => setDriverEditState(null)}>Cancel</Button>
                                                </div>
                                            </div>
                                        ) : (
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '4px', width: '100%' }}>
                                                <VSCodeTextField
                                                    value={driverEditState.value}
                                                    placeholder="1.0.0"
                                                    onInput={(e: any) => setDriverEditState({ ...driverEditState, value: e.target.value })}
                                                    style={{ flex: 1 }}
                                                />
                                                <Button appearance="primary" onClick={handleDriverEditSave} disabled={!driverEditState.value.trim() || isSaving}>Save</Button>
                                                <Button appearance="secondary" onClick={() => setDriverEditState(null)}>Cancel</Button>
                                            </div>
                                        ))}
                                    </div>

                                    {!isEditing && (
                                        <DriverActions className="driver-actions">
                                            {isAddedDep ? (
                                                <>
                                                    {!isEffectivelyOmitted && !dep.localPath && (
                                                        <Button
                                                            appearance="icon"
                                                            tooltip="Edit dependency"
                                                            onClick={() => setDriverEditState({
                                                                connectionType: dep.connectionType,
                                                                groupId: dep.groupId,
                                                                artifactId: dep.artifactId,
                                                                isAdded: true,
                                                                groupIdValue: dep.groupId ?? '',
                                                                artifactIdValue: dep.artifactId ?? '',
                                                                value: effectiveVersion
                                                            })}
                                                            buttonSx={{ color: 'var(--vscode-charts-green)' }}
                                                            disabled={isSaving}
                                                        >
                                                            <Codicon name="edit" />
                                                        </Button>
                                                    )}
                                                    {!isEffectivelyOmitted && (
                                                        <Button
                                                            appearance="icon"
                                                            tooltip={dep.localPath ? "Change local JAR" : "Use local JAR"}
                                                            onClick={() => handleBrowseLocalJar(dep)}
                                                            buttonSx={{ color: 'var(--vscode-charts-blue)' }}
                                                            disabled={isSaving}
                                                        >
                                                            <Codicon name="folder-opened" />
                                                        </Button>
                                                    )}
                                                    <Button
                                                        appearance="icon"
                                                        tooltip="Delete dependency"
                                                        onClick={() => handleDriverReset(dep.connectionType, dep.groupId, dep.artifactId)}
                                                        buttonSx={{ color: 'var(--vscode-charts-red)' }}
                                                        disabled={isSaving}
                                                    >
                                                        <Codicon name="trash" />
                                                    </Button>
                                                </>
                                            ) : (
                                                <>
                                                    {!isEffectivelyOmitted && !dep.localPath && (
                                                        <Button
                                                            appearance="icon"
                                                            tooltip="Override version"
                                                            onClick={() => setDriverEditState({
                                                                connectionType: dep.connectionType,
                                                                groupId: dep.groupId,
                                                                artifactId: dep.artifactId,
                                                                isAdded: false,
                                                                groupIdValue: dep.groupId ?? '',
                                                                artifactIdValue: dep.artifactId ?? '',
                                                                value: effectiveVersion
                                                            })}
                                                            buttonSx={{ color: 'var(--vscode-charts-green)' }}
                                                            disabled={isSaving}
                                                        >
                                                            <Codicon name="edit" />
                                                        </Button>
                                                    )}
                                                    {!isEffectivelyOmitted && (
                                                        <Button
                                                            appearance="icon"
                                                            tooltip={dep.localPath ? "Change local JAR" : "Use local JAR"}
                                                            onClick={() => handleBrowseLocalJar(dep)}
                                                            buttonSx={{ color: 'var(--vscode-charts-blue)' }}
                                                            disabled={isSaving}
                                                        >
                                                            <Codicon name="folder-opened" />
                                                        </Button>
                                                    )}
                                                    {!isEffectivelyOmitted && !driverData.omitAllDrivers && (
                                                        <Button
                                                            appearance="icon"
                                                            tooltip="Omit from CAR"
                                                            onClick={() => setConfirmOmitDriver({ connectionType: dep.connectionType, groupId: dep.groupId, artifactId: dep.artifactId })}
                                                            buttonSx={{ color: 'var(--vscode-editorWarning-foreground)' }}
                                                            disabled={isSaving}
                                                        >
                                                            <Codicon name="circle-slash" />
                                                        </Button>
                                                    )}
                                                    {(dep.isOverridden || dep.omit || dep.localPath) && !driverData.omitAllDrivers && (
                                                        <Button
                                                            appearance="icon"
                                                            tooltip="Reset to default"
                                                            onClick={() => handleDriverReset(dep.connectionType, dep.groupId, dep.artifactId)}
                                                            buttonSx={{ color: 'var(--vscode-charts-red)' }}
                                                            disabled={isSaving}
                                                        >
                                                            <Codicon name="discard" />
                                                        </Button>
                                                    )}
                                                </>
                                            )}
                                        </DriverActions>
                                    )}
                                </DriverDepRow>
                            );
                        })}

                        {inactiveDeps.length > 0 && (
                            <>
                                <DriverToggleButton
                                    style={{ marginTop: '4px', marginBottom: '4px' }}
                                    onClick={() => setShowInactiveDeps(!showInactiveDeps)}
                                >
                                    <Codicon name={showInactiveDeps ? "chevron-down" : "chevron-right"} />
                                    <span style={{ opacity: 0.6 }}>Inactive drivers ({inactiveDeps.length})</span>
                                </DriverToggleButton>
                                {showInactiveDeps && inactiveDeps.map((dep, idx) => {
                                    const label = driverDepLabel(dep);
                                    const effectiveVersion = dep.overriddenVersion ?? dep.defaultVersion ?? '';
                                    return (
                                        <DriverDepRow key={`inactive-${label}-${idx}`} style={{ opacity: 0.5 }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', flex: 1 }}>
                                                <DriverDepName muted>{label}</DriverDepName>
                                                <DriverBadge>{effectiveVersion}</DriverBadge>
                                                <DriverLabel>no matching connection</DriverLabel>
                                            </div>
                                        </DriverDepRow>
                                    );
                                })}
                            </>
                        )}
                    </>
                )}
            </Container>

            {/* ── Confirm: omit single driver ── */}
            <Dialog
                isOpen={confirmOmitDriver !== null}
                onClose={() => handleDriverOmitConfirm(false)}
                sx={{ width: '400px', padding: '24px' }}
            >
                <DialogMessage>
                    Omit the <strong>{confirmOmitDriver ? driverDepLabel(driverDeps.find(d => d.connectionType === confirmOmitDriver.connectionType && d.groupId === confirmOmitDriver.groupId && d.artifactId === confirmOmitDriver.artifactId) ?? {}) : ''}</strong> driver from the CAR?
                    <br /><br />
                    It will not be packed at build time. Click Reset to undo.
                </DialogMessage>
                <DialogActions>
                    <Button appearance="secondary" onClick={() => handleDriverOmitConfirm(false)}>Cancel</Button>
                    <Button appearance="primary" onClick={() => handleDriverOmitConfirm(true)}>Omit</Button>
                </DialogActions>
            </Dialog>

            {/* ── Confirm: omit all drivers ── */}
            <Dialog
                isOpen={confirmOmitAllDrivers}
                onClose={() => handleOmitAllDriversConfirm(false)}
                sx={{ width: '400px', padding: '24px' }}
            >
                <DialogMessage>
                    Omit all driver dependencies for <strong>{connectorArtifactId}</strong> from the CAR?
                    <br /><br />
                    No drivers will be packed at build time. Click Reset All to undo.
                </DialogMessage>
                <DialogActions>
                    <Button appearance="secondary" onClick={() => handleOmitAllDriversConfirm(false)}>Cancel</Button>
                    <Button appearance="primary" onClick={() => handleOmitAllDriversConfirm(true)}>Omit All</Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
