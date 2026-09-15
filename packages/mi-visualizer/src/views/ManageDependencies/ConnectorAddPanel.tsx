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

import { useEffect, useMemo, useRef, useState } from "react";
import { ConnectorStatus, DependencyDetails } from "@wso2/mi-core";
import { useVisualizerContext } from "@wso2/mi-rpc-client";
import { Button, Codicon, TextField, Typography, ProgressRing, Overlay } from "@wso2/ui-toolkit";
import { VSCodeDropdown, VSCodeOption } from "@vscode/webview-ui-toolkit/react";
import { Colors } from "@wso2/mi-diagram/lib/resources/constants";
import { FirstCharToUpperCase, compareVersions } from "@wso2/mi-diagram/lib/utils/commons";
import { DependencyForm } from "../Overview/ProjectInformation/DependencyForm";
import styled from "@emotion/styled";

const HeaderBar = styled.div`
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin: 16px 0;
`;

const ActionButtons = styled.div`
    display: flex;
    gap: 8px;
`;

const ConnectorRow = styled.div`
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

const ConnectorInfo = styled.div`
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
`;

const ConnectorIcon = styled.img`
    width: 28px;
    height: 28px;
    object-fit: contain;
`;

const ConnectorName = styled.span`
    font-size: 13px;
    font-weight: 600;
    color: var(--vscode-settings-headerForeground);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
`;

const RowActions = styled.div`
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
`;

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

interface ConnectorAddPanelProps {
    connectors?: any[];
    existingDependencies: DependencyDetails[];
    isBusy: boolean;
    duplicateError?: string;
    onClose: () => void;
    onManualSubmit: (dep: { groupId: string; artifact: string; version: string }) => void;
    onChanged: () => Promise<void>;
    // When true, this panel is scoped to inbound endpoint modules (mi-inbound-* artifacts) and
    // the zip import goes into the inbound-endpoints folder and labels reflect that scope.
    inboundOnly?: boolean;
}

export function ConnectorAddPanel(props: ConnectorAddPanelProps) {
    const { connectors, existingDependencies, isBusy, duplicateError, onClose, onManualSubmit, onChanged, inboundOnly } = props;
    const { rpcClient } = useVisualizerContext();

    const [showManualForm, setShowManualForm] = useState(false);
    const [searchValue, setSearchValue] = useState("");
    const [selectedVersions, setSelectedVersions] = useState<Record<string, string>>({});
    const [downloading, setDownloading] = useState(false);
    const [downloadingName, setDownloadingName] = useState<string>("");
    const connectorStatusQueue = useRef<ConnectorStatus[]>([]);

    useEffect(() => {
        rpcClient.onConnectorStatusUpdate((connectorStatus: ConnectorStatus) => {
            connectorStatusQueue.current.push(connectorStatus);
        });
    }, []);

    const availableConnectors = useMemo(() => {
        const list = Array.isArray(connectors) ? connectors : Object.values(connectors ?? {});
        return list
            .filter((c: any) => c && c.mavenGroupId && c.mavenArtifactId)
            // Hide connectors already present as a dependency.
            .filter((c: any) => !existingDependencies.some(
                dep => dep.groupId === c.mavenGroupId && dep.artifact === c.mavenArtifactId
            ))
            .filter((c: any) => {
                if (!searchValue) return true;
                const name = (c.connectorName ?? c.mavenArtifactId ?? "").toLowerCase();
                return name.includes(searchValue.toLowerCase());
            })
            // The inbound-endpoint catalog ranks with "rank" while the outbound catalog uses
            // "connectorRank" — support both so either source sorts correctly.
            .sort((a: any, b: any) => (a.connectorRank ?? a.rank ?? 0) - (b.connectorRank ?? b.rank ?? 0));
    }, [connectors, existingDependencies, searchValue]);

    const versionsFor = (connector: any): string[] => {
        const latest = connector?.version?.tagName;
        const others = Object.keys(connector?.otherVersions ?? {});
        const unique = Array.from(new Set([latest, ...others].filter(Boolean)));
        return unique.sort((a, b) => compareVersions(b, a));
    };

    const effectiveVersion = (connector: any): string =>
        selectedVersions[connector.connectorName] ?? connector?.version?.tagName ?? "";

    const handleDownloadConnector = async (connector: any) => {
        const version = effectiveVersion(connector);
        if (!version) return;
        setDownloading(true);
        setDownloadingName(connector.connectorName ?? connector.mavenArtifactId);
        try {
            await rpcClient.getMiVisualizerRpcClient().updateDependencies({
                dependencies: [{
                    groupId: connector.mavenGroupId,
                    artifact: connector.mavenArtifactId,
                    version,
                    type: 'zip'
                }]
            });
            const response = await rpcClient.getMiVisualizerRpcClient().updateConnectorDependencies();
            const downloadSucceeded = response === "Success" || !response.includes(connector.mavenArtifactId);
            if (!downloadSucceeded) {
                // remove the connector dependency as the download failed
                const projectDetails = await rpcClient.getMiVisualizerRpcClient().getProjectDetails();
                const connectorDependencies = projectDetails.dependencies.connectorDependencies;
                for (const d of connectorDependencies) {
                    if (d.artifact === connector.mavenArtifactId && d.version === version) {
                        await rpcClient.getMiVisualizerRpcClient().updatePomValues({
                            pomValues: [{ range: d.range, value: '' }]
                        });
                        break;
                    }
                }
                await rpcClient.getMiDiagramRpcClient().formatPomFile();
                await onChanged();
                rpcClient.getMiVisualizerRpcClient().showNotification({
                    message: `Failed to download the ${connector.connectorName ?? connector.mavenArtifactId} connector.`,
                    type: "error"
                });
                return;
            }
            await rpcClient.getMiDiagramRpcClient().formatPomFile();
            await onChanged();
            onClose();
        } catch (error) {
            console.error("Error adding connector dependency:", error);
            rpcClient.getMiVisualizerRpcClient().showNotification({
                message: "Failed to add the connector dependency.",
                type: "error"
            });
        } finally {
            setDownloading(false);
            setDownloadingName("");
        }
    };

    const waitForConnectorStatus = (expectedConnectorName?: string) => {
        return new Promise<ConnectorStatus>((resolve, reject) => {
            if (!expectedConnectorName?.trim()) {
                reject(new Error('No connector name was provided to match against the import status.'));
                return;
            }

            const checkInterval = setInterval(() => {
                const queue = connectorStatusQueue.current;
                const matchIndex = queue.findIndex(status => status.connector === expectedConnectorName);
                if (matchIndex !== -1) {
                    const [status] = queue.splice(matchIndex, 1);
                    clearInterval(checkInterval);
                    resolve(status);
                }
            }, 200);

            // Reject the promise after 15 seconds
            setTimeout(() => {
                clearInterval(checkInterval);
                reject(new Error('Event did not occur within 15 seconds'));
            }, 15000);
        });
    };

    const handleImportZip = async (isInbound: boolean) => {
        const selected = await rpcClient.getMiDiagramRpcClient().askFileDirPath({
            filters: { [isInbound ? 'Inbound Endpoint Zip' : 'Connector Zip']: ['zip'] }
        });
        if (!selected?.path || !selected.path.endsWith('.zip')) {
            return;
        }
        setDownloading(true);
        setDownloadingName(selected.path.split(/[\\/]/).pop() ?? "connector");
        connectorStatusQueue.current = [];
        try {
            const response = await rpcClient.getMiDiagramRpcClient().copyConnectorZip({ connectorPath: selected.path, isInbound });

            if (!response.success) {
                rpcClient.getMiVisualizerRpcClient().showNotification({
                    message: response.error || (isInbound
                        ? "The selected file is not a valid inbound endpoint."
                        : "The selected file is not a valid connector."),
                    type: "error",
                    modal: true
                });
                return;
            }

            if (!isInbound) {
                try {
                    const newConnector = await waitForConnectorStatus(response.parsedConnectorName);
                    if (!newConnector?.isSuccess) {
                        await removeInvalidConnector(response.connectorPath);
                        rpcClient.getMiVisualizerRpcClient().showNotification({
                            message: "The selected file is not a valid connector.",
                            type: "error",
                            modal: true
                        });
                        return;
                    }
                } catch (error) {
                    console.error("Timed out waiting for connector validation:", error);
                    await removeInvalidConnector(response.connectorPath);
                    rpcClient.getMiVisualizerRpcClient().showNotification({
                        message: "The selected file is not a valid connector.",
                        type: "error",
                        modal: true
                    });
                    return;
                }
            }

            await rpcClient.getMiDiagramRpcClient().formatPomFile();
            await onChanged();
            onClose();
        } catch (error) {
            console.error("Error importing from zip file:", error);
            rpcClient.getMiVisualizerRpcClient().showNotification({
                message: "Failed to import from the zip file.",
                type: "error",
                modal: true
            });
        } finally {
            setDownloading(false);
            setDownloadingName("");
        }
    };

    const removeInvalidConnector = async (connectorPath: string) => {
        try {
            await rpcClient.getMiDiagramRpcClient().removeConnector({ connectorPath });
        } catch (error) {
            console.error("Error removing invalid connector:", error);
        }
    };

    if (showManualForm) {
        return (
            <DependencyForm
                groupId=""
                artifact=""
                version=""
                title="Add Dependency"
                showLoader={isBusy}
                duplicateError={duplicateError}
                onClose={() => setShowManualForm(false)}
                onUpdate={onManualSubmit}
            />
        );
    }

    return (
        <>
            <HeaderBar>
                <Button appearance="icon" onClick={onClose} tooltip="Back to dependencies">
                    <Codicon name="arrow-left" />&nbsp;Back
                </Button>
                <ActionButtons>
                    <Button appearance="secondary" onClick={() => handleImportZip(!!inboundOnly)}>
                        <Codicon name="file-zip" />&nbsp;{inboundOnly ? "Import Inbound Endpoint" : "Import Connector"}
                    </Button>
                    <Button appearance="secondary" onClick={() => setShowManualForm(true)}>
                        <Codicon name="edit" />&nbsp;Configure Dependency
                    </Button>
                </ActionButtons>
            </HeaderBar>

            <TextField
                placeholder={inboundOnly ? "Search inbound endpoints" : "Search connectors"}
                value={searchValue}
                onTextChange={setSearchValue}
                icon={{ iconComponent: <Codicon name="search" sx={{ cursor: "auto" }} />, position: 'start' }}
                sx={{ width: '100%', marginBottom: '12px' }}
            />

            {connectors === undefined ? (
                <div style={{ display: 'flex', justifyContent: 'center', paddingTop: '20px' }}>
                    <ProgressRing />
                </div>
            ) : availableConnectors.length === 0 ? (
                <Typography sx={{ marginTop: '10px', opacity: 0.7 }}>
                    No {inboundOnly ? "inbound endpoints" : "connectors"} available to add. Use "Import {inboundOnly ? "Inbound Endpoint" : "Connector"} (.zip)" or "Configure Dependency".
                </Typography>
            ) : (
                <div>
                    {availableConnectors.map((connector: any) => {
                        // Inbound endpoints are always added at the latest version — no picker.
                        const versions = inboundOnly ? [] : versionsFor(connector);
                        return (
                            <ConnectorRow key={connector.connectorName ?? connector.mavenArtifactId}>
                                <ConnectorInfo>
                                    {connector.iconUrl && <ConnectorIcon src={connector.iconUrl} alt="" />}
                                    <ConnectorName>
                                        {FirstCharToUpperCase(connector.connectorName ?? connector.mavenArtifactId)}
                                    </ConnectorName>
                                </ConnectorInfo>
                                <RowActions>
                                    {versions.length > 1 ? (
                                        <VSCodeDropdown
                                            value={effectiveVersion(connector)}
                                            onChange={(e: any) => setSelectedVersions(prev => ({
                                                ...prev,
                                                [connector.connectorName]: e.target.value
                                            }))}
                                        >
                                            {versions.map(v => (
                                                <VSCodeOption key={v} value={v}>{v}</VSCodeOption>
                                            ))}
                                        </VSCodeDropdown>
                                    ) : (
                                        <span style={{ fontSize: '12px', opacity: 0.7 }}>{effectiveVersion(connector)}</span>
                                    )}
                                    <Button appearance="primary" onClick={() => handleDownloadConnector(connector)} disabled={downloading}>
                                        <Codicon name="cloud-download" />&nbsp;Download
                                    </Button>
                                </RowActions>
                            </ConnectorRow>
                        );
                    })}
                </div>
            )}

            {downloading && (
                <>
                    <Overlay sx={{ background: `${Colors.SURFACE_CONTAINER}`, opacity: `0.3`, zIndex: 2000 }} />
                    <LoaderContainer>
                        <ProgressRing sx={{ height: '32px', width: '32px' }} />
                        <span>Adding {downloadingName}...</span>
                    </LoaderContainer>
                </>
            )}
        </>
    );
}
