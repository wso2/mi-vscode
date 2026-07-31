/**
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com)
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

import { useEffect } from "react";
import { Button, Codicon, Dialog, Typography, ToggleSwitch, TextField, PasswordField } from "@wso2/ui-toolkit";
import { ConsolidatedRemoteDeployConfig } from "@wso2/mi-core";
import { Controller, useForm } from "react-hook-form";
import * as yup from "yup";
import { yupResolver } from "@hookform/resolvers/yup";
import { baseDialogSx, ModalHeader, SectionDivider, ButtonRow } from "../../components/DialogBoxStyles";

const dialogSx = {
    ...baseDialogSx,
    width: 480,
};

interface Props {
    initialConfig?: ConsolidatedRemoteDeployConfig | null;
    onSave: (config: ConsolidatedRemoteDeployConfig) => Promise<void>;
    onClose: () => void;
}

const DEFAULT: ConsolidatedRemoteDeployConfig = {
    serverUrl: "https://localhost:9164",
    username: "",
    password: "",
    truststorePath: "",
    truststorePassword: "",
    truststoreType: "JKS",
    serverType: "mi",
    isEnabled: true,
};

const schema = yup.object({
    isEnabled: yup.boolean().required(),
    serverUrl: yup.string().when("isEnabled", { is: true, then: (s) => s.trim().required("Server URL is required.") }),
    username: yup.string().when("isEnabled", { is: true, then: (s) => s.trim().required("Username is required.") }),
    password: yup.string().when("isEnabled", { is: true, then: (s) => s.required("Password is required.") }),
    truststorePath: yup.string().when("isEnabled", { is: true, then: (s) => s.trim().required("Trust Store Path is required.") }),
    truststorePassword: yup.string().when("isEnabled", { is: true, then: (s) => s.required("Trust Store Password is required.") }),
    truststoreType: yup.string().when("isEnabled", { is: true, then: (s) => s.trim().required("Trust Store Type is required.") }),
});

export function RemoteDeployConfigModal({ initialConfig, onSave, onClose }: Props) {
    const isEditMode = !!initialConfig;

    const {
        register,
        handleSubmit,
        control,
        watch,
        reset,
        setError,
        formState: { errors, isSubmitting },
    } = useForm<ConsolidatedRemoteDeployConfig>({
        defaultValues: { ...DEFAULT, ...(initialConfig ?? {}) },
        resolver: yupResolver(schema) as any,
        mode: "onSubmit",
    });

    useEffect(() => {
        if (initialConfig) {
            reset({ ...DEFAULT, ...initialConfig });
        }
    }, [initialConfig, reset]);

    const isEnabled = watch("isEnabled");

    const onSubmit = async (values: ConsolidatedRemoteDeployConfig) => {
        if (!isEditMode && !values.isEnabled) {
            onClose();
            return;
        }
        try {
            await onSave(values);
            onClose();
        } catch {
            setError("root", { message: "Failed to save configuration." });
        }
    };

    return (
        <Dialog isOpen onClose={onClose} sx={dialogSx}>
            <ModalHeader>
                <Codicon name="server" sx={{ fontSize: "16px", color: "var(--vscode-textLink-foreground)" }} />
                <Typography variant="h3" sx={{ margin: 0, flex: 1 }}>
                    Remote Deployment Configuration
                </Typography>
                <Button appearance="icon" onClick={onClose}>
                    <Codicon name="close" />
                </Button>
            </ModalHeader>
            {isEnabled && (
                <>
                    <SectionDivider>Server</SectionDivider>
                    <TextField
                        label="Server URL"
                        required
                        description="Management API URL of the remote MI server"
                        placeholder="https://localhost:9164"
                        autoFocus
                        errorMsg={errors.serverUrl?.message}
                        {...register("serverUrl")}
                    />
                    <TextField
                        label="Username"
                        required
                        placeholder="admin"
                        errorMsg={errors.username?.message}
                        {...register("username")}
                    />
                    <PasswordField
                        label="Password"
                        required
                        placeholder="admin"
                        errorMsg={errors.password?.message}
                        {...register("password")}
                    />
                    <SectionDivider>Trust Store</SectionDivider>
                    <TextField
                        label="Trust Store Path"
                        required
                        placeholder="repository/resources/security/client-truststore.jks"
                        errorMsg={errors.truststorePath?.message}
                        {...register("truststorePath")}
                    />
                    <PasswordField
                        label="Trust Store Password"
                        required
                        placeholder="wso2carbon"
                        errorMsg={errors.truststorePassword?.message}
                        {...register("truststorePassword")}
                    />
                    <TextField
                        label="Trust Store Type"
                        required
                        placeholder="JKS"
                        errorMsg={errors.truststoreType?.message}
                        {...register("truststoreType")}
                    />
                </>
            )}
            {errors.root?.message && (
                <Typography sx={{ color: "var(--vscode-errorForeground)", fontSize: "12px" }}>
                    {errors.root.message}
                </Typography>
            )}
            <ButtonRow>
                {isEditMode && (
                    <div style={{ display: "flex", alignItems: "center", gap: 8, marginRight: "auto" }}>
                        <Controller
                            control={control}
                            name="isEnabled"
                            render={({ field: { value, onChange } }) => (
                                <ToggleSwitch
                                    checked={value}
                                    onChange={onChange}
                                    sx={{ fontSize: 8 }}
                                />
                            )}
                        />
                        <Typography sx={{ fontSize: 13 }}>Enable</Typography>
                    </div>
                )}
                <Button appearance="secondary" onClick={onClose} disabled={isSubmitting}>
                    Cancel
                </Button>
                <Button appearance="primary" onClick={handleSubmit(onSubmit)} disabled={isSubmitting}>
                    {isSubmitting ? "Saving..." : isEditMode ? "Save" : "Save & Enable"}
                </Button>
            </ButtonRow>
        </Dialog>
    );
}
