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
 * Test explorer run and debug related funtions.
 */

import { Uri, CancellationToken, TestItem, TestItemCollection, TestMessage, TestRunProfileKind, TestRunRequest, window, MarkdownString, TestRun, OutputChannel, workspace } from "vscode";

import { discoverTests, gatherTestItems } from "./discover";
import { testController } from "./activator";
import path = require("path");
import { getProjectRoot } from "./helper";
import { getServerPath, setJavaHomeInEnvironmentAndPath, promptAndWriteCipherToolPassword, killProcessByPort } from "../debugger/debugHelper";
import { checkMissingConfigurablesAndPrompt } from "../debugger/activate";
import { TestResumeGroup } from "@wso2/mi-core";
import { TestRunnerConfig } from "./config";
import { ChildProcess } from "child_process";
import treeKill = require("tree-kill");
import { normalize } from "upath";
import { COMMANDS, MVN_COMMANDS } from "../constants";
import { loadEnvVariables } from "../debugger/tasks";
import { escapeShellArg } from "../util/shellEscapeUtils";
const fs = require('fs');
const child_process = require('child_process');
const readline = require('readline');

enum EXEC_ARG {
    TESTS = '--tests',
    COVERAGE = '--code-coverage'
}
enum TEST_STATUS {
    PASSED = 'PASSED',
    FAILED = 'FAILED'
}
const TEST_RESULTS_PATH = path.join("target", "unit-test-report.json").toString();

// run tests.
export function runHandler(request: TestRunRequest, cancellation: CancellationToken) {
    const queue: { test: TestItem; data: any; }[] = [];
    const run = testController.createTestRun(request);

    discoverTests(request, request.include ?? gatherTestItems(testController.items), queue).then(runTestQueue);

    async function runTestQueue() {

        if (queue.length === 0) {
            run.end();
            window.showErrorMessage("No tests found.");
            return;
        }

        if (request.profile?.kind == TestRunProfileKind.Debug) {
            window.showErrorMessage("Test debugging is not yet supported.");
            run.end();
            return;
        }

        if (request.profile?.kind != TestRunProfileKind.Run) {
            run.end();
            return;
        }

        // Group queued tests by their project root so consolidated/multi-project
        // workspaces run every project's tests instead of only the first one's.
        const projectGroups = new Map<string, { test: TestItem; data: any; }[]>();
        for (const item of queue) {
            const projectRoot = getProjectRoot(Uri.file(item.test.id));
            if (!projectRoot) {
                run.failed(item.test, new TestMessage("Project root not found."));
                continue;
            }
            const group = projectGroups.get(projectRoot) ?? [];
            group.push(item);
            projectGroups.set(projectRoot, group);
        }

        if (projectGroups.size === 0) {
            run.end();
            window.showErrorMessage("Project root not found.");
            return;
        }

        const resumeGroups: TestResumeGroup[] = [...projectGroups.entries()].map(([projectRoot, projectQueue]) => ({
            projectRoot,
            testIds: projectQueue.map(({ test }) => test.id),
            triggerID: request?.include?.find(
                (item) => getProjectRoot(Uri.file(item.id)) === projectRoot)?.id ?? ""
        }));

        // Check all projects up front in one popup
        const shouldProceed = await checkMissingConfigurablesAndPrompt(
            [...projectGroups.keys()],
            COMMANDS.RESUME_TEST_RUN,
            [resumeGroups]
        );
        if (!shouldProceed) {
            run.end();
            return;
        }

        // Run each project's test cycle sequentially (each project spins up its
        // own MI test server on the same port, so they cannot overlap).
        try {
            for (const { projectRoot, triggerID } of resumeGroups) {
                await runProjectTests(run, projectRoot, projectGroups.get(projectRoot)!, triggerID);
            }
        } finally {
            run.end();
        }
    }
}

export async function runProjectTests(run: TestRun, projectRoot: string, projectQueue: { test: TestItem; data: any; }[], triggerID: string) {
    let stopTestServer: (() => void) | undefined;
    const startTime = Date.now();

    run.appendOutput(`Start running tests\r\n`);

    let testNames = "";
    // mark tests as running in test explorer
    for (const { test, } of projectQueue) {
        testNames = testNames == "" ? test.label : `${testNames},${test.label}`;
        markStatusAsRunning(run, test);
    }

    const failProjectTests = () => {
        const EndTime = Date.now();
        const timeElapsed = (EndTime - startTime) / projectQueue.length;
        for (const { test, } of projectQueue) {
            const testMessage: TestMessage = new TestMessage("Command failed");
            run.failed(test, testMessage, timeElapsed);
        }
    };

    try {
        const serverPath = await getServerPath(projectRoot);
        if (!serverPath) {
            window.showErrorMessage("MI server path not found");
            failProjectTests();
            return;
        }

        const printer = (line: string, isError: boolean) => {
            printToOutput(run, line, isError);
        }

        // compile project
        await compileProject(projectRoot, printer);

        // execute test
        run.appendOutput(`Starting MI test server\r\n`);
        const { cp } = await startTestServer(serverPath, projectRoot, printer);
        stopTestServer = () => {
            treeKill(cp.pid!, 'SIGKILL');
        }

        run.appendOutput("\x1b[32m================== MI test server started ==================\x1b[0m\r\n");
        run.appendOutput(`Running tests ${testNames}\r\n`);

        await runTests(testNames, projectRoot, triggerID, printer);
        const EndTime = Date.now();
        const timeElapsed = (EndTime - startTime) / projectQueue.length;

        // reading test results
        const testsJson: JSON | undefined = await readJsonFile(path.join(projectRoot, TEST_RESULTS_PATH).toString());
        if (!testsJson) {
            for (const { test, } of projectQueue) {
                const testMessage: TestMessage = new TestMessage("Command failed");
                run.failed(test, testMessage, timeElapsed);
            }
            window.showErrorMessage("Test results not found.");
        } else {

            for (const { test, } of projectQueue) {
                let testResults;
                let testCases;
                if (test.id.endsWith(".xml")) {
                    const id = normalize(test.id);

                    testResults = Object.entries(testsJson).find(([key]) => normalize(key) === id)?.[1];
                    testCases = test.children;
                } else {
                    const strs = test.id.split("/");
                    strs.pop();
                    const suiteName = strs.join("/");
                    testResults = testsJson[suiteName];
                    testCases = [[test.id, test]]
                }

                if (!testResults) {
                    const testMessage: TestMessage = new TestMessage("Test result not found");
                    run.failed(test, testMessage, timeElapsed);
                    continue;
                }

                const mediationStatus = testResults["mediationStatus"];
                const deploymentStatus = testResults["deploymentStatus"];
                const testCasesResults = testResults["testCases"];

                if (deploymentStatus === TEST_STATUS.PASSED && mediationStatus === TEST_STATUS.PASSED) {

                    for (const testCase of testCases) {
                        const testCaseItem = testCase[1];
                        const testCaseName = testCaseItem.label;
                        const testCaseResult = testCasesResults.find((testCaseResult: any) => testCaseResult["testCaseName"] === testCaseName);
                        if (testCaseResult) {
                            const mediationStatus = testCaseResult["mediationStatus"];
                            const assertionStatus = testCaseResult["assertionStatus"];
                            if (mediationStatus === TEST_STATUS.PASSED && assertionStatus === TEST_STATUS.PASSED) {
                                run.passed(testCaseItem, timeElapsed);
                            } else {
                                let message: TestMessage;
                                if (assertionStatus === TEST_STATUS.FAILED) {
                                    const failureAssertions = testCaseResult["failureAssertions"];
                                    const table = new MarkdownString();
                                    table.appendMarkdown(`| Test Case | Assert Expression | Failure Message |\n`);
                                    table.appendMarkdown(`| --- | --- | --- |\n`);
                                    for (const assertion of failureAssertions) {
                                        const actualValue = assertion["actual"];
                                        const expectedValue = assertion["expected"];
                                        const failureMessage = `Expected: ${expectedValue}, Actual: ${actualValue}`
                                        table.appendMarkdown(`| ${testCaseName} | ${assertion["assertionExpression"]} | ${failureMessage} |\n`);
                                    }
                                    message = new TestMessage(table);
                                } else {
                                    message = new TestMessage("Test mediation failed");
                                }
                                run.failed(testCaseItem, message, timeElapsed);
                            }
                        } else {
                            const testMessage: TestMessage = new TestMessage("Test result not found");
                            run.failed(testCaseItem, testMessage, timeElapsed);
                        }
                    }
                    // run.passed(test, timeElapsed);
                } else {
                    // test failed
                    const testMessage: TestMessage = new TestMessage("Mediation failed");
                    run.failed(test, testMessage, timeElapsed);
                }
            }
        }
    } catch (error: any) {
        // exception.
        window.showErrorMessage(`Error: ${error}`);
        String(error).split('\n').forEach((line) => {
            printToOutput(run, line, true);
        });
        failProjectTests();
    } finally {
        run.appendOutput(`Test running finished\r\n`);
        if (stopTestServer) {
            stopTestServer();
        }
    }
}

function markStatusAsRunning(run: TestRun, test: TestItem) {
    run.started(test);
    if (test.children) {
        for (const child of test.children) {
            markStatusAsRunning(run, child[1]);
        }
    }
}

export function findTestItemById(collection: TestItemCollection, id: string): TestItem | undefined {
    const direct = collection.get(id);
    if (direct) {
        return direct;
    }
    for (const [, item] of collection) {
        const found = findTestItemById(item.children, id);
        if (found) {
            return found;
        }
    }
    return undefined;
}

// Handler for COMMANDS.RESUME_TEST_RUN. Resumes every paused project sequentially.
export async function resumeProjectTestRun(resumeGroups: TestResumeGroup[]): Promise<void> {
    const allItems: TestItem[] = [];
    const perProject: { projectRoot: string; projectQueue: { test: TestItem; data: any }[]; triggerID: string }[] = [];

    for (const { projectRoot, testIds, triggerID } of resumeGroups) {
        const items = testIds
            .map(id => findTestItemById(testController.items, id))
            .filter((item): item is TestItem => !!item);
        if (items.length === 0) {
            continue;
        }
        allItems.push(...items);
        perProject.push({ projectRoot, projectQueue: items.map(test => ({ test, data: null })), triggerID });
    }

    if (allItems.length === 0) {
        window.showErrorMessage("Could not resume the test run: the selected tests were not found.");
        return;
    }

    const run = testController.createTestRun(new TestRunRequest(allItems));
    (async () => {
        try {
            for (const { projectRoot, projectQueue, triggerID } of perProject) {
                await runProjectTests(run, projectRoot, projectQueue, triggerID);
            }
        } finally {
            run.end();
        }
    })().catch(error => window.showErrorMessage(`Error running tests: ${error}`));
}

/**
 * Start test server.
 * @returns server output
 */
async function startTestServer(serverPath: string, projectRoot: string, printToOutput?: (line: string, isError: boolean) => void): Promise<{ cp: ChildProcess }> {
    return new Promise<{ cp: ChildProcess }>(async (resolve, reject) => {
        try {
            const filePath = path.resolve(projectRoot, '.env');
            if (fs.existsSync(filePath)) {
                loadEnvVariables(filePath)
            }

            // Clear any leftover server from a previous run still bound to this port.
            await killProcessByPort(TestRunnerConfig.getServerPort());

            const passwordWritten = await promptAndWriteCipherToolPassword(serverPath);
            if (!passwordWritten) {
                reject('Server startup cancelled: cipher tool decrypt password was not provided.');
                return;
            }

            const scriptFile = process.platform === "win32" ? "micro-integrator.bat" : "micro-integrator.sh";
            const server = path.join(serverPath, "bin", scriptFile);

            const serverCommand = `${escapeShellArg(server)} -DsynapseTest`;

            let serverStarted = false;

            const printer = (line: string, isError: boolean) => {
                if (!serverStarted && printToOutput) {
                    printToOutput(line, isError);
                }
            }

            const cp = runCommand(serverCommand, projectRoot, onData, onError, undefined, printer);

            function onData(data: string) {
                if (data.includes("WSO2 Micro Integrator started in")) {
                    serverStarted = true;
                    resolve({ cp });
                }
                if (data.includes("Address already in use")) {
                    reject("Port already in use. Please stop the server and try again or update the port in the settings.");
                }
            }
            function onError(data: string) {
                window.showErrorMessage(data);
                reject(data);
            }

        } catch (error) {
            reject(error instanceof Error ? error.message : String(error));
        }
    });
}

function printToOutput(runner: TestRun, line: string, isError: boolean = false) {
    if (isError) {
        runner.appendOutput(`\x1b[31m${line}\x1b[0m\r\n`); // Print in red color
    } else {
        runner.appendOutput(`${line}\r\n`);
    }
}

async function compileProject(projectRoot: string, printToOutput?: (line: string, isError: boolean) => void): Promise<void> {
    return new Promise<void>(async (resolve, reject) => {
        const config = workspace.getConfiguration('MI', Uri.file(projectRoot));
        const mvnCmd = config.get("useLocalMaven") ? "mvn" : (process.platform === "win32" ?
            MVN_COMMANDS.MVN_WRAPPER_WIN_COMMAND : MVN_COMMANDS.MVN_WRAPPER_COMMAND);
        const testRunCmd = mvnCmd + MVN_COMMANDS.COMPILE_COMMAND;

        let finished = false;
        const onData = (data: string) => {
            if (data.includes("BUILD SUCCESS")) {
                finished = true;
                resolve();
            }
        }
        const onError = (data: string) => {
            window.showErrorMessage(data);
            reject(data);
        }
        const onClose = (code: number) => {
            if (code !== 0 && !finished) {
                reject("Project build failed");
            }
        }

        try {
            runCommand(testRunCmd, projectRoot, onData, onError, onClose, printToOutput);
        } catch (error) {
            reject(error instanceof Error ? error.message : String(error));
        }
    });
}

async function runTests(testNames: string, projectRoot: string, triggerId: string, printToOutput?: (line: string, isError: boolean) => void): Promise<void> {
    return new Promise<void>(async (resolve, reject) => {
        const config = workspace.getConfiguration('MI', Uri.file(projectRoot));
        const mvnCmd = config.get("useLocalMaven") ? "mvn" : (process.platform === "win32" ?
            MVN_COMMANDS.MVN_WRAPPER_WIN_COMMAND : MVN_COMMANDS.MVN_WRAPPER_COMMAND);
        const testLevel = triggerId.endsWith(".xml") ? "unitTest" : triggerId.includes(".xml") ? "testCase" : "testSuite";

        let testRunCmd: string;
        try {
            const basicTestCmd = `${mvnCmd + MVN_COMMANDS.TEST_COMMAND} -DtestServerHost=${escapeShellArg(TestRunnerConfig.getHost())} -DtestServerPort=${TestRunnerConfig.getServerPort()} -P test`;

            testRunCmd = basicTestCmd;
            switch (testLevel) {
                case "unitTest":
                    testRunCmd = basicTestCmd + ` -DtestFile=${escapeShellArg(path.basename(triggerId))}`;
                    break;
                case "testCase":
                    testRunCmd = basicTestCmd + ` -DtestFile=${escapeShellArg(path.basename(path.dirname(triggerId)))} -DtestCaseName=${escapeShellArg(testNames)}`;
                    break;
            }
        } catch (error) {
            reject(`Test run failed: ${error instanceof Error ? error.message : error}`);
            return;
        }

        const onData = (data: string) => { }
        const onError = (data: string) => { }

        const onClose = (code: number) => {
            resolve();
        }

        try {
            runCommand(testRunCmd, projectRoot, onData, onError, onClose, printToOutput);
        } catch (error) {
            reject(error instanceof Error ? error.message : String(error));
        }
    });
}

/**
 * Run terminal command.
 * @param command Command to run.
 * @param pathToRun Path to execute the command.
 * @param returnData Indicates whether to return the stdout
 */
export function runCommand(command, pathToRun?: string,
    onData?: (data: string) => void,
    onError?: (data: string) => void,
    onClose?: (code: number) => void,
    printToOutput?: (line: string, isError: boolean) => void): ChildProcess {
    try {
        if (pathToRun) {
            command = `cd ${escapeShellArg(pathToRun)} && ${command}`
        }
        const envVariables = {
            ...process.env,
            ...(pathToRun ? setJavaHomeInEnvironmentAndPath(pathToRun) : {})
        };
        const cp = child_process.spawn(command, [], { shell: true, env: envVariables });

        if (typeof onData === 'function') {
            cp.stdout.setEncoding('utf8');
            const rl = readline.createInterface({ input: cp.stdout });

            let foundError = false;
            rl.on('line', (line) => {
                if (line.includes("] ERROR ") || line.includes("[error]")) {
                    foundError = true;
                } else if (line.includes("]  INFO ") || line.includes("[INFO]")) {
                    foundError = false;
                }

                if (printToOutput) {
                    printToOutput(line, foundError);
                }
                onData(line);
            });
        }

        if (typeof onError === 'function') {
            cp.stderr.setEncoding('utf8');
            let errorData = '';
            cp.stderr.on('data', (data) => {
                errorData += data;
            });
            cp.stderr.on('end', () => onError(errorData));
        }

        cp.on('error', (data: string) => {
            data.split('\n').forEach((line) => {
                if (printToOutput) {
                    printToOutput(line, true);
                }
            });
            if (onError) {
                onError(data);
            }
        });

        if (typeof onClose === 'function') {
            cp.on('close', onClose);
        }

        return cp;
    } catch (error: any) {
        console.error('Failed to spawn process:', error);
        if (onError) {
            onError(error.message);
        }
        throw error;
    }
}

export function runBasicCommand(
    command: string,
    cwd: string,
    onData?: (data: string) => void,
    onError?: (data: string) => void,
    onComplete?: () => void,
    outputChannel?: OutputChannel
) {

    if (cwd) {
        command = `cd ${escapeShellArg(cwd)} && ${command}`
    }
    const proc = child_process.spawn(command, [], { shell: true });

    if (outputChannel) {
        outputChannel.show(true);
        outputChannel.appendLine(`Running: ${command}`);
    }

    proc.stdout?.on('data', (data: string) => {
        const text = data.toString();
        outputChannel?.append(text);
        onData?.(data);
    });

    proc.stderr?.on('data', (data: string) => {
        const text = data.toString();
        outputChannel?.append(text);
        onError?.(data);
    });

    proc.on('close', (code) => {
        outputChannel?.appendLine(`\nProcess exited with code ${code}`);
        onComplete?.();
    });
}

/** 
 * Read test json output.
 * @param file File path of the json.
 */
export async function readJsonFile(path: string): Promise<JSON | undefined> {
    try {
        let rawdata = fs.readFileSync(path);
        return JSON.parse(rawdata);
    } catch {
        return undefined;
    }
}

/**
 * Start debugging
 */
export async function startDebugging(uri: Uri, testDebug: boolean, args: any[])
    : Promise<void> {
    throw new Error("Method not implemented.");
}
