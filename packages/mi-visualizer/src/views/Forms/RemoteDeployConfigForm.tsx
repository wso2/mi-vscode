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

import styled from "@emotion/styled";
import { Button, Codicon, Dialog, PasswordField, TextField, Typography } from "@wso2/ui-toolkit";
import { DeployConfigParam } from "@wso2/mi-core";
import { useVisualizerContext } from "@wso2/mi-rpc-client";
import { useForm } from "react-hook-form";
import { baseDialogSx, ModalHeader, SectionDivider, ButtonRow } from "../../components/DialogBoxStyles";

const FIELD_LABELS: Record<string, string> = {
    trustStorePath: "Truststore Path",
    trustStorePassword: "Truststore Password",
    trustStoreType: "Truststore Type",
    serverUrl: "Server URL",
    userName: "Username",
    password: "Password",
    serverType: "Server Type",
    operation: "Operation",
};

function toLabel(key: string): string {
    if (FIELD_LABELS[key]) { return FIELD_LABELS[key]; }
    return key
        .replace(/([A-Z])/g, " $1")
        .replace(/^./, s => s.toUpperCase())
        .trim();
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

const SectionNote = styled.div`
    font-size: 12px;
    opacity: 0.75;
    margin-top: -8px;
`;

const FieldGroup = styled.div`
    display: flex;
    flex-direction: column;
    gap: 12px;
`;

interface RemoteDeployConfigFormProps {
    configs: DeployConfigParam[];
    onClose: () => void;
    // Defaults to the single-project executeRemoteDeployWithParams RPC
    // Pass this to target a different deploy flow
    onDeploy?: (values: Record<string, string>) => void;
}

export function RemoteDeployConfigForm({ configs, onClose, onDeploy }: RemoteDeployConfigFormProps) {
    const { rpcClient } = useVisualizerContext();

    const fixedConfigs = configs.filter(c => !c.isParameterized);
    const paramConfigs = configs.filter(c => c.isParameterized);

    const defaultValues: Record<string, string> = {};
    configs.forEach(c => { defaultValues[c.key] = c.value || ""; });

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<Record<string, string>>({
        defaultValues,
        mode: "onChange",
    });

    const handleDeploy = (data: Record<string, string>) => {
        if (onDeploy) {
            onDeploy(data);
        } else {
            rpcClient.getMiVisualizerRpcClient().executeRemoteDeployWithParams({ values: data });
        }
        onClose();
    };

    const isPassword = (key: string) => /password/i.test(key);

    const renderField = (cfg: DeployConfigParam, allowParam: boolean) => {
        const rules = {
            required: `${toLabel(cfg.key)} is required`,
            ...(!allowParam ? { validate: (v: string) => !/\$\{[^}]+\}/.test(v) || "Parameterized values are not allowed here" } : {}),
        };
        const commonProps = {
            label: toLabel(cfg.key),
            ...register(cfg.key, rules),
            errorMsg: errors[cfg.key]?.message,
            description: cfg.isParameterized ? `Maven property: \${${cfg.paramName}}` : undefined,
        };

        return isPassword(cfg.key)
            ? <PasswordField key={cfg.key} {...commonProps} />
            : <TextField key={cfg.key} {...commonProps} />;
    };

    return (
        <Dialog isOpen onClose={onClose} sx={dialogSx}>
            <ModalHeader>
                <Codicon name="server" sx={{ fontSize: "16px", color: "var(--vscode-textLink-foreground)" }} />
                <Typography variant="h3" sx={{ margin: 0, flex: 1 }}>
                    Remote Deploy — Configure Parameters
                </Typography>
                <Button appearance="icon" onClick={onClose}>
                    <Codicon name="close" />
                </Button>
            </ModalHeader>

            <ScrollArea>
                {paramConfigs.length > 0 && (
                    <>
                        <SectionDivider>Required Parameters</SectionDivider>
                        <FieldGroup>
                            {paramConfigs.map(cfg => renderField(cfg, true))}
                        </FieldGroup>
                    </>
                )}

                {fixedConfigs.length > 0 && (
                    <>
                        <SectionDivider>Existing Configurations</SectionDivider>
                        <SectionNote>Note: Values entered here will overwrite the existing plugin configuration.</SectionNote>
                        <FieldGroup>
                            {fixedConfigs.map(cfg => renderField(cfg, false))}
                        </FieldGroup>
                    </>
                )}
            </ScrollArea>

            <ButtonRow>
                <Button appearance="secondary" onClick={onClose} disabled={isSubmitting}>
                    Cancel
                </Button>
                <Button appearance="primary" onClick={handleSubmit(handleDeploy)} disabled={isSubmitting}>
                    {isSubmitting ? "Deploying..." : "Deploy"}
                </Button>
            </ButtonRow>
        </Dialog>
    );
}
