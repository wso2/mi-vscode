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

import * as path from "path";
import { commands, Uri, window } from "vscode";
import { CreateMiProjectRequest, CreateMiProjectResponse } from "@wso2/mi-core";
import { COMMANDS } from "../constants";

/**
 * Creates an MI project via the project-explorer command (the same flow the
 * WSO2 Integrator delegated to before the form moved here), then opens it.
 *
 * Returns the created project's `filePath`, or an empty `filePath` when the
 * command produced no result (non-throwing failure).
 */
export async function createMiProject(params: CreateMiProjectRequest): Promise<CreateMiProjectResponse> {
    try {
        const miCommandParams = {
            name: params.name,
            path: path.join(params.directory, params.name),
            scope: "user",
            open: params.open,
            miVersion: params.miVersion,
            isConsolidatedProject: params.isConsolidatedProject ?? false,
            subProjects: params.subProjects ?? [],
            groupId: params.groupID ?? "com.microintegrator.projects",
            artifactId: params.artifactID ?? params.name,
            version: params.version ?? "1.0.0",
        };
        const result = await commands.executeCommand(COMMANDS.CREATE_PROJECT_COMMAND, miCommandParams);
        if (result) {
            const response = result as CreateMiProjectResponse;
            await commands.executeCommand("vscode.openFolder", Uri.file(path.resolve(response.filePath)));
            return response;
        }
        return { filePath: "" };
    } catch (error) {
        const errorMessage = error instanceof Error ? error.message : String(error);
        window.showErrorMessage(`Failed to create MI project: ${errorMessage}`);
        throw error;
    }
}
