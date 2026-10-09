/**
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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
import { Control, Controller, UseFormGetValues, UseFormSetValue } from "react-hook-form";
import { CheckBox, TextField, Typography } from "@wso2/ui-toolkit";

const ExButton = styled.div<{ isActive: boolean }>`
    margin-left: 6px;
    position: relative;
    top: 1px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 3px 5px;
    cursor: pointer;
    background-color: ${(props: { isActive: boolean }) => props.isActive ? "var(--vscode-inputOption-activeBackground)" : "var(--vscode-inputOption-inactiveBackground)"};
    border: 1px solid ${(props: { isActive: boolean }) => props.isActive ? "var(--vscode-inputOption-activeBorder)" : "transparent"};
    &:hover {
        background-color: ${(props: { isActive: boolean }) => props.isActive ? "var(--vscode-inputOption-activeBackground)" : "var(--vscode-inputOption-hoverBackground)"};
    }
`;

export type BooleanOrTextFieldProps = {
    id?: string;
    label: string;
    control: Control<any>;
    getValues: UseFormGetValues<any>;
    setValue: UseFormSetValue<any>;
    booleanName: string;
    textName: string;
    isTextName: string;
    placeholder?: string;
    errorMsg?: string;
};

/**
 * A checkbox that can be toggled to a text field for a custom value.
 */
export function BooleanOrTextField(props: BooleanOrTextFieldProps) {
    const { id, label, control, setValue, booleanName, textName, isTextName, placeholder, errorMsg } = props;

    const switchMode = (toText: boolean) => {
        const options = { shouldDirty: true, shouldValidate: true };
        if (toText) {
            // Start empty instead of prefilling true/false
            setValue(textName, "", { shouldDirty: true });
        } else {
            setValue(booleanName, false, options);
        }
        setValue(isTextName, toText, options);
    };

    const renderToggle = (toText: boolean) => (
        <ExButton
            id={`${id ?? textName}-mode-toggle`}
            isActive={!toText}
            title={toText ? "Use a custom value" : "Use a checkbox"}
            onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                switchMode(toText);
            }}
        >
            <Typography sx={{ textAlign: "center", margin: 0, lineHeight: 1 }} variant="h6">EX</Typography>
        </ExButton>
    );

    return (
        <Controller
            name={isTextName}
            control={control}
            render={({ field: { value: isText } }) =>
                isText ? (
                    <Controller
                        name={textName}
                        control={control}
                        render={({ field }) => (
                            <TextField
                                id={id ?? textName}
                                label={label}
                                labelAdornment={renderToggle(false)}
                                placeholder={placeholder}
                                value={field.value ?? ""}
                                onTextChange={field.onChange}
                                onBlur={field.onBlur}
                                errorMsg={errorMsg}
                            />
                        )}
                    />
                ) : (
                    <Controller
                        name={booleanName}
                        control={control}
                        render={({ field }) => (
                            <CheckBox
                                label={label}
                                labelAdornment={renderToggle(true)}
                                checked={!!field.value}
                                onChange={field.onChange}
                            />
                        )}
                    />
                )
            }
        />
    );
}
