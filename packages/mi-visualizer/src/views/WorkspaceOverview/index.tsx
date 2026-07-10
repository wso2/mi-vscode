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
import {
    ConsolidatedProjectDetails,
    ConsolidatedRemoteDeployConfig,
    DeployProjectRequest,
    EVENT_TYPE,
    MACHINE_VIEW,
    WorkspaceFolder,
    WorkspaceProjectSummary,
} from "@wso2/mi-core";
import { useVisualizerContext } from "@wso2/mi-rpc-client";
import { ViewHeader } from "../../components/View";
import {
    Alert,
    Button,
    Codicon,
    colors,
    Divider,
    Icon,
    ProgressRing,
    TextField,
    ToggleSwitch,
    Typography,
} from "@wso2/ui-toolkit";
import styled from "@emotion/styled";
import { DeploymentOptions } from "../Overview/DeploymentStatus";
import { RemoteDeployConfigModal } from "./RemoteDeployConfigModal";
import { COMMANDS } from "../../constants";
import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import * as yup from "yup";
import { yupResolver } from "@hookform/resolvers/yup";

const Body = styled.div`
    padding: 0 32px 16px;
    background: ${colors.vscodeEditorBackground};
    flex: 1;
    overflow: hidden;
    min-height: 0;
`;

const ProjectSection = styled.div`
    background: ${colors.vscodeTextCodeBlockBackground};
    border-radius: 12px;
    padding: 24px;
    margin-bottom: 16px;
`;

const ProjectHeader = styled.div`
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
`;

const ProjectTitle = styled.div`
    font-size: 15px;
    font-weight: 600;
    flex: 1;
`;

const TwoColumn = styled.div`
    display: flex;
    flex-direction: row;
    gap: 24px;
    height: 100%;
    align-items: stretch;

    @media (max-width: 700px) {
        flex-direction: column;
    }
`;

const ProjectListColumn = styled.div`
    display: flex;
    flex-direction: column;
    flex: 1;
    min-width: 0;
    min-height: 0;
`;

const ProjectScrollArea = styled.div`
    flex: 1;
    overflow-y: auto;
    min-height: 0;
`;

const DeployColumn = styled.div`
    background: ${colors.vscodeTextCodeBlockBackground};
    border-radius: 12px;
    padding: 24px;
`;

const ArtifactRow = styled.div`
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
    margin-top: 8px;
`;

const ArtifactBadge = styled.div`
    display: flex;
    align-items: center;
    gap: 5px;
    opacity: 0.65;
    font-size: 12px;
`;

const MetaItem = styled.div`
    display: flex;
    align-items: center;
    gap: 6px;
    opacity: 0.6;
    font-size: 12px;
    margin-bottom: 4px;
`;

const RightColumn = styled.div`
    display: flex;
    flex-direction: column;
    gap: 16px;
    width: 380px;
    flex-shrink: 0;
    overflow-y: auto;
    min-height: 0;

    @media (max-width: 700px) {
        width: auto;
    }
`;

const InfoPanel = styled.div`
    background: ${colors.vscodeTextCodeBlockBackground};
    border-radius: 12px;
    padding: 24px;
`;

const InfoPanelTitle = styled(Typography)`
    margin: 0 0 16px;
    opacity: 0.8;
`;

const InfoRow = styled.div`
    display: flex;
    flex-direction: column;
    margin-bottom: 12px;
`;

const InfoLabel = styled.span`
    font-size: 11px;
    opacity: 0.5;
    margin-bottom: 2px;
`;

const InfoValue = styled.span`
    font-size: 13px;
    opacity: 0.85;
    word-break: break-all;
`;

const ConvertBanner = styled.div`
    background: ${colors.vscodeTextCodeBlockBackground};
    border-radius: 12px;
    padding: 20px 24px;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 16px;
`;

interface ProjectData {
    workspace: WorkspaceFolder;
    summary?: WorkspaceProjectSummary;
    loading: boolean;
}

const DEFAULT_DETAILS: ConsolidatedProjectDetails = {
    groupId: "com.example",
    artifactId: "",
    version: "1.0.0",
    runtimeVersion: "",
    cappBuildPluginVersion: "",
    dockerBaseImage: "",
};

const detailsSchema = yup.object({
    groupId: yup.string().trim().required("Group ID is required.")
        .matches(/^([a-zA-Z_$][a-zA-Z\d_$]*\.)*[a-zA-Z_$][a-zA-Z\d_$]*$/, "Invalid Group ID."),
    artifactId: yup.string().trim().required("Artifact ID is required.")
        .matches(/^[a-zA-Z][a-zA-Z\d._-]*$/, "Invalid Artifact ID."),
    version: yup.string().trim().required("Version is required."),
    runtimeVersion: yup.string().trim().required("Runtime version is required."),
    cappBuildPluginVersion: yup.string().trim().required("CApp build plugin version is required."),
    dockerBaseImage: yup.string().trim().required("Docker base image is required."),
});

export function WorkspaceOverview() {
    const { rpcClient } = useVisualizerContext();
    const [projects, setProjects] = useState<ProjectData[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [workspaceName, setWorkspaceName] = useState("");
    const [isConsolidated, setIsConsolidated] = useState(false);
    const [isEditingDetails, setIsEditingDetails] = useState(false);
    const [remoteDeployConfig, setRemoteDeployConfig] = useState<ConsolidatedRemoteDeployConfig | null>(null);
    const [showRemoteDeployModal, setShowRemoteDeployModal] = useState(false);

    const {
        register,
        handleSubmit,
        reset,
        watch,
        trigger,
        formState: { errors, isValid },
    } = useForm<ConsolidatedProjectDetails>({
        defaultValues: DEFAULT_DETAILS,
        resolver: yupResolver(detailsSchema) as any,
        mode: "onChange",
    });
    const details = watch();

    useEffect(() => {
        rpcClient.getMiVisualizerRpcClient().getConsolidatedProjectDetails().then(d => {
            if (d) reset(d);
        });
        rpcClient.getMiVisualizerRpcClient().getConsolidatedRemoteDeployConfig().then(config => {
            setRemoteDeployConfig(config);
        });
    }, []);

    const toggleEditing = () => {
        if (isEditingDetails) {
            handleSubmit((values) => {
                rpcClient.getMiVisualizerRpcClient().updateConsolidatedProjectDetails({ details: values });
                reset(values);
                setIsEditingDetails(false);
            })();
        } else {
            setIsEditingDetails(true);
            trigger();
        }
    };

    const { data: devantMetadata } = useQuery({
        queryKey: ["devant-metadata-workspace"],
        queryFn: () => rpcClient.getMiDiagramRpcClient().getDevantMetadata(),
        refetchInterval: 5000,
        enabled: isConsolidated,
    });

    useEffect(() => {
        async function fetchAll() {
            try {
                const [{ workspaces }, consolidatedResult] = await Promise.all([
                    rpcClient.getMiVisualizerRpcClient().getWorkspaces(),
                    rpcClient.getMiDiagramRpcClient().canCreateConsolidatedProject(),
                ]);

                setWorkspaceName(deriveWorkspaceName(workspaces));
                setIsConsolidated(consolidatedResult?.isConsolidatedProject ?? false);
                setProjects(workspaces.map(ws => ({ workspace: ws, loading: true })));
                setIsLoading(false);

                for (const ws of workspaces) {
                    rpcClient.getMiVisualizerRpcClient()
                        .getWorkspaceProjectSummary({ documentUri: ws.fsPath })
                        .then(summary => {
                            setProjects(prev => prev.map(p =>
                                p.workspace.fsPath === ws.fsPath ? { ...p, summary, loading: false } : p
                            ));
                        })
                        .catch(() => {
                            setProjects(prev => prev.map(p =>
                                p.workspace.fsPath === ws.fsPath ? { ...p, loading: false } : p
                            ));
                        });
                }
            } catch {
                setIsLoading(false);
            }
        }
        fetchAll();
    }, []);

    const deriveWorkspaceName = (workspaces: WorkspaceFolder[]) => {
        if (!workspaces.length) return "Workspace";
        const parts = workspaces[0].fsPath.split(/[/\\]/);
        return parts[parts.length - 2] || "Workspace";
    };

    const openProjectOverview = (ws: WorkspaceFolder) => {
        rpcClient.getMiVisualizerRpcClient().openView({
            type: EVENT_TYPE.OPEN_VIEW,
            location: { view: MACHINE_VIEW.Overview, projectUri: ws.fsPath },
        });
    };

    const handleCreateProject = () => {
        rpcClient.getMiVisualizerRpcClient().openView({
            type: EVENT_TYPE.OPEN_VIEW,
            location: { view: MACHINE_VIEW.Welcome },
        });
    };

    const handleConvertToConsolidated = () => {
        rpcClient.getMiDiagramRpcClient().executeCommand({
            commands: ["MI.convert.to.consolidated"],
        });
    };

    // Consolidated workspace deployment handlers — all operations target the
    // consolidated project root so they build / deploy the whole workspace.
    const handleDockerBuild = () =>
        rpcClient.getMiDiagramRpcClient().buildProject({ buildType: "docker" });

    const handleCappBuild = () =>
        rpcClient.getMiDiagramRpcClient().buildProject({ buildType: "capp" });

    const handleConsolidatedBuild = () =>
        rpcClient.getMiDiagramRpcClient().buildProject({ buildType: "consolidated" });

    const handleRemoteDeploy = () => {
        if (remoteDeployConfig?.isEnabled) {
            rpcClient.getMiDiagramRpcClient().remoteDeploy();
        } else {
            setShowRemoteDeployModal(true);
        }
    };

    const handleSaveRemoteDeployConfig = async (config: ConsolidatedRemoteDeployConfig) => {
        await rpcClient.getMiVisualizerRpcClient().saveConsolidatedRemoteDeployConfig(config);
        const updated = await rpcClient.getMiVisualizerRpcClient().getConsolidatedRemoteDeployConfig();
        setRemoteDeployConfig(updated);
    };

    const handleConfigureKubernetes = async () => {
        const conf = await rpcClient.getMiDiagramRpcClient().isKubernetesConfigured();
        if (conf) {
            rpcClient.getMiDiagramRpcClient().executeCommand({
                commands: [COMMANDS.EDIT_K8_CONFIGURATION_COMMAND],
            });
        } else {
            rpcClient.getMiVisualizerRpcClient().openView({
                type: EVENT_TYPE.OPEN_VIEW,
                location: { view: MACHINE_VIEW.KubernetesConfigurationForm },
            });
        }
    };

    const handleDeploy = (params: DeployProjectRequest) =>
        rpcClient.getMiDiagramRpcClient().deployProject(params);

    const goToDevant = () =>
        rpcClient.getMiDiagramRpcClient().executeCommand({
            commands: ["MI.openAiPanel"],
        });

    const EMPTY_COUNTS = { apis: 0, automations: 0, eventIntegrations: 0, other: 0 };

    if (isLoading) {
        return (
            <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100vh" }}>
                <ProgressRing />
            </div>
        );
    }

    const projectList = (
        <ProjectListColumn>
            <Typography variant="h4" sx={{ margin: "0 0 12px", opacity: 0.8, flexShrink: 0 }}>
                Projects ({projects.length})
            </Typography>
            <ProjectScrollArea>
            {projects.map(({ workspace, summary, loading }) => {
                const counts = summary?.artifactCounts ?? EMPTY_COUNTS;
                const version = summary?.runtimeVersion;
                const hasArtifacts = counts.apis + counts.automations + counts.eventIntegrations + counts.other > 0;
                return (
                    <ProjectSection key={workspace.fsPath}>
                        <ProjectHeader>
                            <Icon name="project" sx={{ fontSize: "16px", color: "var(--vscode-textLink-foreground)" }} />
                            <ProjectTitle>{workspace.name}</ProjectTitle>
                            <Button
                                appearance="secondary"
                                onClick={() => openProjectOverview(workspace)}
                                sx={{ fontSize: "12px" }}
                            >
                                <Codicon name="link-external" sx={{ marginRight: "6px" }} />
                                Open Overview
                            </Button>
                        </ProjectHeader>

                        {loading ? (
                            <div style={{ display: "flex", alignItems: "center", gap: 8, opacity: 0.6 }}>
                                <ProgressRing />
                                <Typography sx={{ fontSize: "12px" }}>Loading...</Typography>
                            </div>
                        ) : (
                            <>
                                {version && (
                                    <MetaItem>
                                        <Icon name="versions" isCodicon sx={{ fontSize: "12px" }} />
                                        <span>Runtime {version}</span>
                                    </MetaItem>
                                )}
                                <ArtifactRow>
                                    <ArtifactBadge>
                                        <Icon name="globe" isCodicon sx={{ fontSize: "12px" }} />
                                        <span>{counts.apis} {counts.apis === 1 ? "API" : "APIs"}</span>
                                    </ArtifactBadge>
                                    <ArtifactBadge>
                                        <Icon name="zap" isCodicon sx={{ fontSize: "12px" }} />
                                        <span>{counts.automations} {counts.automations === 1 ? "Automation" : "Automations"}</span>
                                    </ArtifactBadge>
                                    <ArtifactBadge>
                                        <Icon name="broadcast" isCodicon sx={{ fontSize: "12px" }} />
                                        <span>{counts.eventIntegrations} {counts.eventIntegrations === 1 ? "Event Integration" : "Event Integrations"}</span>
                                    </ArtifactBadge>
                                    {counts.other > 0 && (
                                        <ArtifactBadge>
                                            <Icon name="symbol-misc" isCodicon sx={{ fontSize: "12px" }} />
                                            <span>{counts.other} Other</span>
                                        </ArtifactBadge>
                                    )}
                                </ArtifactRow>
                                {!hasArtifacts && (
                                    <Typography sx={{ opacity: 0.5, fontSize: "12px", marginTop: "8px" }}>
                                        No artifacts added yet.
                                    </Typography>
                                )}
                            </>
                        )}
                    </ProjectSection>
                );
            })}
            </ProjectScrollArea>
        </ProjectListColumn>
    );

    return (
        <>
        <div style={{ display: "flex", flexDirection: "column", height: "calc(100vh - 30px)", overflow: "hidden", padding: "10px 0" }}>
            <div style={{ padding: "0 16px", marginBottom: "16px" }}>
                <ViewHeader
                    title={"Workspace: " + workspaceName}
                    icon="project"
                    iconSx={{ fontSize: "18px", color: "var(--vscode-textLink-foreground)" }}
                >
                    <Button
                        appearance="primary"
                        onClick={handleCreateProject}
                        tooltip="Create New Project"
                        sx={{ background: "var(--vscode-button-background)", "&:hover": { background: "var(--vscode-button-hoverBackground)" } }}
                    >
                        <Codicon name="add" sx={{ marginRight: "8px" }} />
                        Create New Project
                    </Button>
                </ViewHeader>
            </div>
            <Body>
                {projects.length === 0 ? (
                    <Alert
                        title="No projects found"
                        subTitle="Create a new project to get started."
                        variant="primary"
                    />
                ) : isConsolidated ? (
                    /* Consolidated workspace: project list on the left,
                       project info + deployment panels on the right */
                    <TwoColumn>
                        {projectList}
                        <RightColumn>
                            <DeployColumn>
                                <DeploymentOptions
                                    handleDockerBuild={handleDockerBuild}
                                    handleConfigureKubernetes={handleConfigureKubernetes}
                                    handleCAPPBuild={handleCappBuild}
                                    handleConsolidatedBuild={handleConsolidatedBuild}
                                    handleRemoteDeploy={handleRemoteDeploy}
                                    handleDeploy={handleDeploy}
                                    goToDevant={goToDevant}
                                    devantMetadata={devantMetadata}
                                    isConsolidatedProject={true}
                                    showAllOptions={true}
                                />
                            </DeployColumn>
                            <InfoPanel>
                                <div style={{ display: "flex", alignItems: "center", marginBottom: "16px" }}>
                                    <InfoPanelTitle variant="h4" sx={{ margin: 0, flex: 1 }}>Project Details</InfoPanelTitle>
                                    <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                                        <span style={{ fontSize: "11px", opacity: 0.7 }}>Edit</span>
                                        <ToggleSwitch
                                            checked={isEditingDetails}
                                            onChange={toggleEditing}
                                            disabled={isEditingDetails && !isValid}
                                            sx={{ fontSize: 8 }}
                                        />
                                    </div>
                                </div>
                                <InfoRow>
                                    <InfoLabel>Group ID:</InfoLabel>
                                    {isEditingDetails
                                        ? <TextField autoFocus errorMsg={errors.groupId?.message} {...register("groupId")} />
                                        : <InfoValue>{details.groupId}</InfoValue>}
                                </InfoRow>
                                <InfoRow>
                                    <InfoLabel>Artifact ID:</InfoLabel>
                                    {isEditingDetails
                                        ? <TextField errorMsg={errors.artifactId?.message} {...register("artifactId")} />
                                        : <InfoValue>{details.artifactId}</InfoValue>}
                                </InfoRow>
                                <InfoRow>
                                    <InfoLabel>Version:</InfoLabel>
                                    {isEditingDetails
                                        ? <TextField errorMsg={errors.version?.message} {...register("version")} />
                                        : <InfoValue>{details.version}</InfoValue>}
                                </InfoRow>
                                <InfoRow>
                                    <InfoLabel>Runtime Version:</InfoLabel>
                                    {isEditingDetails
                                        ? <TextField errorMsg={errors.runtimeVersion?.message} {...register("runtimeVersion")} />
                                        : <InfoValue>{details.runtimeVersion}</InfoValue>}
                                </InfoRow>
                                <InfoRow>
                                    <InfoLabel>CApp Build Plugin Version:</InfoLabel>
                                    {isEditingDetails
                                        ? <TextField errorMsg={errors.cappBuildPluginVersion?.message} {...register("cappBuildPluginVersion")} />
                                        : <InfoValue>{details.cappBuildPluginVersion}</InfoValue>}
                                </InfoRow>
                                <InfoRow>
                                    <InfoLabel>Docker Base Image:</InfoLabel>
                                    {isEditingDetails
                                        ? <TextField errorMsg={errors.dockerBaseImage?.message} {...register("dockerBaseImage")} />
                                        : <InfoValue>{details.dockerBaseImage}</InfoValue>}
                                </InfoRow>
                                {remoteDeployConfig?.isEnabled && !isEditingDetails && (
                                    <InfoRow>
                                        <InfoLabel>Remote Deployment</InfoLabel>
                                        <div style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "4px" }}>
                                            <InfoValue style={{ opacity: 0.7, fontSize: "12px" }}>
                                                {remoteDeployConfig.serverUrl || "Configured"}
                                            </InfoValue>
                                            <Button
                                                appearance="icon"
                                                onClick={() => setShowRemoteDeployModal(true)}
                                                tooltip="Edit remote deployment configuration"
                                                sx={{ padding: "2px 6px", fontSize: "11px" }}
                                            >
                                                <Codicon name="edit" sx={{ fontSize: "12px", marginRight: "4px" }} />
                                                Edit
                                            </Button>
                                        </div>
                                    </InfoRow>
                                )}
                            </InfoPanel>
                        </RightColumn>
                    </TwoColumn>
                ) : (
                    /* Non-consolidated workspace: project list + convert banner */
                    <div style={{ height: "100%", overflowY: "auto" }}>
                        <ConvertBanner>
                            <Icon name="layers" isCodicon sx={{ fontSize: "22px", opacity: 0.7, flexShrink: 0 }} />
                            <div style={{ flex: 1 }}>
                                <Typography variant="h4" sx={{ margin: "0 0 4px", opacity: 0.9 }}>
                                    Convert to Consolidated Project
                                </Typography>
                                <Typography sx={{ fontSize: "12px", opacity: 0.65 }}>
                                    Merge these projects into a single consolidated workspace to enable shared build and deployment options.
                                </Typography>
                            </div>
                            <Button
                                appearance="primary"
                                onClick={handleConvertToConsolidated}
                                sx={{ flexShrink: 0 }}
                            >
                                <Codicon name="layers" sx={{ marginRight: "8px" }} />
                                Convert to Consolidated
                            </Button>
                        </ConvertBanner>
                        <Divider />
                        <div style={{ marginTop: 16 }}>
                            {projectList}
                        </div>
                    </div>
                )}
            </Body>
        </div>
        {showRemoteDeployModal && (
            <RemoteDeployConfigModal
                initialConfig={remoteDeployConfig}
                onSave={handleSaveRemoteDeployConfig}
                onClose={() => setShowRemoteDeployModal(false)}
            />
        )}
        </>
    );
}
