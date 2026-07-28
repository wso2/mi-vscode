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

import { RelativePattern, TestItem, TestItemCollection, TestRunRequest, workspace } from "vscode";
import { createProjectNode, createTests, resetTestStateNodes, testController } from "./activator";

export const testFileMatchPattern = '**/src/test/**/*.xml';

/**
 * Add test items to an queue to run.
 * 
 * @param request test run request
 * @param tests test tree items
 * @param queue queue to add tests
 */
export async function discoverTests(request: TestRunRequest, tests: Iterable<TestItem>, queue: { test: TestItem; data: any }[]) {
    for (const test of tests) {
        if (request.exclude && request.exclude.includes(test)) {
            continue;
        }

        if (test.canResolveChildren) {
            await discoverTests(request, gatherTestItems(test.children), queue);
        } else {
            queue.push({ test: test, data: null });
        }
    }
}

/**
 * Get test items from test collection.
 * 
 * @param collection test item collection
 * @returns test items array
 */
export function gatherTestItems(collection: TestItemCollection) {
    const items: TestItem[] = [];
    collection.forEach(item => items.push(item));
    return items;
}

export async function createTestsForAllFiles() {
    if (!workspace.workspaceFolders) {
        return;
    }

    // clear current test items and node-id context state
    testController.items.forEach(item => {
        testController.items.delete(item.id);
    });
    await resetTestStateNodes();

    for (const workspaceFolder of workspace.workspaceFolders) {
        // Always surface the project as a top-level node, even when it has no
        // tests yet (mirrors the mock-services tree).
        await createProjectNode(workspaceFolder.uri);

        const pattern = new RelativePattern(workspaceFolder, testFileMatchPattern);
        const files = await workspace.findFiles(pattern, '**/resources/**');
        for (const fileX of files) {
            await createTests(fileX);
        }
    }
}
