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

import { PomNodeDetails } from "@wso2/mi-core";
import { getParamManagerValues, ParamConfig, ParamManager } from "@wso2/mi-diagram";
import { useVisualizerContext } from "@wso2/mi-rpc-client";
import { Button, Codicon, Dialog, FormActions, FormView, TextField, Typography } from "@wso2/ui-toolkit";
import { useState } from "react";
import { useForm } from "react-hook-form";
import styled from "@emotion/styled";
import { baseDialogSx, ModalHeader, SectionDivider, ButtonRow } from "../../../components/DialogBoxStyles";
import { COMMANDS } from "../../../constants";

interface ProjectConfigurables {
    projectUri: string;
    configurables: PomNodeDetails[];
}

interface ManageConfigurablesProps {
    onClose: () => void;
    // 'manage': the existing free-form editor opened from the Project Information panel.
    // 'run': the required-values dialog opened when Build-and-Run finds missing configurables.
    mode?: 'manage' | 'run';
    // Used by 'manage' mode (single project, opened from the Project Information panel).
    configurables?: PomNodeDetails[];
    // Used by 'run' mode (one or more projects with missing configurables).
    projectsConfigs?: ProjectConfigurables[];
    allProjectUris?: string[];
}

function projectName(projectUri: string): string {
    return projectUri.split(/[\\/]/).filter(Boolean).pop() ?? projectUri;
}

const dialogSx = {
    ...baseDialogSx,
    width: 520,
    maxHeight: "90vh",
    overflow: "hidden",
};

const ScrollArea = styled.div`
    flex: 1 1 auto;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin: 0 -8px;
    padding: 0 8px;
`;

const FieldGroup = styled.div`
    display: flex;
    flex-direction: column;
    gap: 18px;
`;

const ProjectGroup = styled.div`
    display: flex;
    flex-direction: column;
    gap: 12px;
`;

function RunConfigurablesDialog(props: ManageConfigurablesProps) {
    const { projectsConfigs = [], onClose, allProjectUris } = props;
    const { rpcClient } = useVisualizerContext();

    const defaultValues: Record<string, string> = {};
    projectsConfigs.forEach((group, i) => {
        group.configurables.forEach(c => { defaultValues[`${i}__${c.key}`] = c.value || ""; });
    });

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<Record<string, string>>({
        defaultValues,
        mode: "onChange",
    });

    const handleSaveAndRun = async (data: Record<string, string>) => {
        await Promise.all(projectsConfigs.map((group, i) => {
            const configValues = group.configurables.map((c) => ({
                key: c.key,
                type: c.type,
                value: data[`${i}__${c.key}`],
            }));
            return rpcClient.getMiVisualizerRpcClient().updateConfigFileValues({ configValues, projectUri: group.projectUri });
        }));

        const resumeProjectUris = allProjectUris && allProjectUris.length > 0
            ? allProjectUris
            : projectsConfigs.map(g => g.projectUri);
        if (resumeProjectUris.length > 0) {
            await rpcClient.getMiDiagramRpcClient().executeCommand({ commands: [COMMANDS.BUILD_AND_RUN_PROJECT, ...resumeProjectUris] });
            onClose();
        }
    };

    const renderField = (groupIndex: number, cfg: PomNodeDetails) => {
        const fieldName = `${groupIndex}__${cfg.key}`;
        return (
            <TextField
                key={fieldName}
                label={cfg.key}
                {...register(fieldName, { required: `${cfg.key} is required` })}
                errorMsg={errors[fieldName]?.message}
            />
        );
    };

    return (
        <Dialog isOpen onClose={onClose} sx={dialogSx}>
            <ModalHeader>
                <Codicon name="settings" sx={{ fontSize: "16px", color: "var(--vscode-textLink-foreground)" }} />
                <Typography variant="h3" sx={{ margin: 0, flex: 1 }}>
                    Setup Configurables
                </Typography>
                <Button appearance="icon" onClick={onClose}>
                    <Codicon name="close" />
                </Button>
            </ModalHeader>

            <ScrollArea>
                {projectsConfigs.map((group, i) => {
                    const missing = group.configurables.filter(c => !c.value);
                    const existing = group.configurables.filter(c => c.value);
                    return (
                        <ProjectGroup key={group.projectUri}>
                            <Typography variant="h4" sx={{ margin: 0 }}>
                                {projectName(group.projectUri)}
                            </Typography>
                            {missing.length > 0 && (
                                <>
                                    <SectionDivider>Missing Configurables</SectionDivider>
                                    <FieldGroup>
                                        {missing.map(cfg => renderField(i, cfg))}
                                    </FieldGroup>
                                </>
                            )}
                            {existing.length > 0 && (
                                <>
                                    <SectionDivider>Existing Configurables</SectionDivider>
                                    <FieldGroup>
                                        {existing.map(cfg => renderField(i, cfg))}
                                    </FieldGroup>
                                </>
                            )}
                        </ProjectGroup>
                    );
                })}
            </ScrollArea>

            <ButtonRow>
                <Button appearance="secondary" onClick={onClose} disabled={isSubmitting}>
                    Cancel
                </Button>
                <Button appearance="primary" onClick={handleSubmit(handleSaveAndRun)} disabled={isSubmitting}>
                    {isSubmitting ? "Saving..." : "Save & Run"}
                </Button>
            </ButtonRow>
        </Dialog>
    );
}

function ManageConfigurablesForm(props: ManageConfigurablesProps) {
    const { configurables = [], onClose } = props;
    const { rpcClient } = useVisualizerContext();
    const [paramConfig, setParamConfig] = useState<ParamConfig>({
        paramValues: configurables.map((config, index) => (
            {
                id: index,
                key: config.key,
                value: config.value,
                icon: 'query',
                paramValues: [
                    { value: config.key },
                    { value: config.type },
                    { value: config.value },
                ]
            }
        )) || [],
        paramFields: [
            {
                "type": "TextField",
                "label": "Key",
                "defaultValue": "",
                "isRequired": true,
                "canChange": false
            },
            {
                "type": "Dropdown",
                "label": "Type",
                values: [
                    "string",
                    "cert",
                ],
                "defaultValue": "string",
                "isRequired": true,
                "canChange": false
            },
            {
                "type": "TextField",
                "label": "Value",
                "defaultValue": "",
                "isRequired": true,
                "canChange": false
            }
        ]
    });

    const updateConfigurables = async () => {
        const values = getParamManagerValues(paramConfig);

        const configs = values.map((value) => {
            return {
                key: value[0]!,
                type: value[1]!,
                value: value[2]!,
            };
        });
        await rpcClient.getMiVisualizerRpcClient().updateConfigFileValues({ configValues: configs });
        onClose();
    };

    return (
        <FormView title={"Configurables"} onClose={onClose}>
            <div style={{
                padding: "10px",
                marginBottom: "20px",
                borderBottom: "1px solid var(--vscode-editorWidget-border)",
                display: "flex",
                flexDirection: 'row'
            }}>
                <Typography>
                    Manage Configurables used in the project. The values will be read from the environment and can also be overridden in the .env file.
                </Typography>
            </div>

            {paramConfig.paramValues.length === 0 && <Typography>No configurables found</Typography>}
            <ParamManager
                allowDuplicates={false}
                paramConfigs={paramConfig}
                readonly={false}
                addParamText="Add Configurable"
                onChange={(values: ParamConfig) => {
                    values.paramValues = values.paramValues.map((param: any) => {
                        const paramValues = param.paramValues;
                        param.key = paramValues[0].value;
                        param.type = paramValues[1].value;
                        param.value = paramValues[2].value;
                        param.icon = 'query';
                        return param;
                    });
                    setParamConfig(values);
                }}
            />
            <FormActions>
                <Button
                    appearance="secondary"
                    onClick={onClose}
                >
                    Cancel
                </Button>
                <Button
                    appearance="primary"
                    onClick={updateConfigurables}
                >
                    {"Update Configurables"}
                </Button>
            </FormActions>
        </FormView>
    );
}

export function ManageConfigurables(props: ManageConfigurablesProps) {
    return props.mode === 'run'
        ? <RunConfigurablesDialog {...props} />
        : <ManageConfigurablesForm {...props} />;
}
