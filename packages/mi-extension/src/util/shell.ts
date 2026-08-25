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

/**
 * Quotes a value so the shell receives it as a single argument.
 *
 * A command string handed to {@code child_process.spawn(..., { shell: true })} is re-parsed by
 * the shell, so a path holding a space - {@code C:\Users\jane doe\...} - is split and the shell
 * reports only its first fragment as missing. Every server path, script path and VM argument
 * must therefore go through this function on its way into a shell command.
 */
export function escapeShellArg(value: string): string {
    if (process.platform === 'win32') {
        // cmd.exe expands %VAR% even inside double quotes and offers no escape for it, so a value
        // carrying '%' (or a control character) cannot be passed through the shell safely at all.
        if (/[%\x00-\x1f]/.test(value)) {
            throw new Error(`Argument cannot be safely passed to the Windows shell ` +
                `(contains '%' or control characters): ${value}`);
        }
        return `"${value.replace(/"/g, '""')}"`;
    }
    return `'${value.replace(/'/g, "'\\''")}'`;
}
