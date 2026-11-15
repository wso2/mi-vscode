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

import * as vscode from 'vscode';
import { AI_COPILOT_DISABLED_BY_CONFIG } from '@wso2/mi-core';

export const COPILOT_DISABLED_SETTING = 'disableWSO2IntegratorCopilot';
export const COPILOT_DISABLED_SETTING_FULL_KEY = `MI.${COPILOT_DISABLED_SETTING}`;

export function isCopilotDisabled(): boolean {
    return vscode.workspace.getConfiguration('MI').get<boolean>(COPILOT_DISABLED_SETTING, false);
}

/**
 * Throws an error if copilot is disabled via settings. This is called at every entry point that reaches the model,
 * so the setting is enforced on the extension side regardless of what the UI shows.
 */
export function assertCopilotEnabled(): void {
    if (isCopilotDisabled()) {
        throw new Error(AI_COPILOT_DISABLED_BY_CONFIG);
    }
}
