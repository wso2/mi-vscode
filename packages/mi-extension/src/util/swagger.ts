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

import { cloneDeep, isEqual } from "lodash";
import { COMMANDS, SWAGGER_PATH_TEMPLATE, SWAGGER_REL_DIR } from "../constants";
import { SwaggerFromAPIResponse, QueryParamInfo } from "@wso2/mi-core";
import { workspace, window } from "vscode";
import path from "path";
import * as vscode from 'vscode';
import { deleteRegistryResource } from "./fileOperations";
import { MILanguageClient } from "../lang-client/activator";
import { parse, stringify } from "yaml";

const fs = require('fs');

export interface Swagger {
    openapi: string;
    info: {
        title: string;
        description: string;
        version: string;
    };
    servers: {
        url: string;
    }[];
    paths: Record<string, any>;
}

interface SwaggerUtilProps {
    existingSwagger: Swagger;
    generatedSwagger: Swagger;
}

interface Resource {
    path: string;
    methods: string[];
}

interface ResourceInfoResponse {
    added: Resource[];
    removed: Resource[];
    updated: Resource[];
}

/**
 * Checks if two swagger paths are equal
 * @param path1 - Object 1
 * @param path2 - Object 2
 * @param comparisonTemplate - Template with comparison instructions
 * @returns - Equal or not
 */
const isEqualPaths = (
    path1: Record<string, any>,
    path2: Record<string, any>,
    comparisonTemplate: Record<string, any>
): boolean => {
    if (!comparisonTemplate?.body) {
        return true;
    }

    let isEqual = true;
    const keys = Object.keys(comparisonTemplate.body);
    if (comparisonTemplate.type === "array") {
        // Query parameters only exist in the swagger file
        // Their presence/absence must never register as a difference between the API and its swagger.
        const filteredPath1 = Array.isArray(path1) ? path1.filter((item: any) => item?.in !== "query") : path1;
        const filteredPath2 = Array.isArray(path2) ? path2.filter((item: any) => item?.in !== "query") : path2;
        if (Object.keys(filteredPath1).length !== Object.keys(filteredPath2).length) {
            return false;
        }
        if (comparisonTemplate.primaryKey?.length) {
            const primaryKey = comparisonTemplate.primaryKey;
            for (const key in filteredPath2) {
                const obj = filteredPath2[key];
                const index = filteredPath1.findIndex((object: Record<string, any>) => {
                    return primaryKey.every((pk: string) => object[pk] === obj[pk]);
                });

                if (index > -1) {
                    isEqual = isEqualPaths(filteredPath1[index], obj, comparisonTemplate.body["*"]);
                } else {
                    isEqual = false;
                }

                if (!isEqual) {
                    break;
                }
            }
        }
    } else {
        if (keys.length === 1 && keys[0] === "*") {
            // Resource paths/methods are fully derived from the Synapse API, so any added/removed key is a real difference.
            if (Object.keys(path1).length !== Object.keys(path2).length) {
                return false;
            }
            for (const key in path2) {
                if (path1[key]) {
                    isEqual = isEqualPaths(path1[key], path2[key], comparisonTemplate.body["*"]);
                } else {
                    isEqual = false;
                }

                if (!isEqual) {
                    break;
                }
            }
        } else {
            // Only compare fields the template tracks, other OpenAPI-only metadata (tags, summary, ...) is not a difference.
            for (const key in comparisonTemplate.body) {
                const fieldTemplate = comparisonTemplate.body[key];
                if (fieldTemplate.type === "array") {
                    isEqual = isEqualPaths(path1[key] ?? [], path2[key] ?? [], fieldTemplate);
                } else if (path2[key] && path1[key]) {
                    isEqual = isEqualPaths(path1[key], path2[key], fieldTemplate);
                } else if ((!path2[key] && path1[key]) || (path2[key] && !path1[key])) {
                    isEqual = false;
                } else {
                    isEqual = true;
                }

                if (!isEqual) {
                    break;
                }
            }
        }
    }

    return isEqual;
};

export const isEqualSwaggers = (props: SwaggerUtilProps): boolean => {
    const { existingSwagger, generatedSwagger } = props;

    // Only "paths" reflects the Synapse API XML. Other top-level fields are OpenAPI-only metadata and not differences.
    return isEqualPaths(existingSwagger.paths ?? {}, generatedSwagger.paths ?? {}, SWAGGER_PATH_TEMPLATE);
};

// Guard against keys that would reach through the prototype chain instead of setting an own property.
const UNSAFE_OBJECT_KEYS = new Set(["__proto__", "constructor", "prototype"]);

// Serializes read-modify-write cycles per swagger file, so concurrent writers (query-param
// edits, auto-regeneration on API save) can't silently clobber each other.
const swaggerFileLocks = new Map<string, Promise<unknown>>();

export function withSwaggerFileLock<T>(swaggerPath: string, action: () => Promise<T>): Promise<T> {
    const previous = swaggerFileLocks.get(swaggerPath) ?? Promise.resolve();
    const next = previous.then(action, action);
    swaggerFileLocks.set(swaggerPath, next.catch(() => undefined));
    return next;
}

/**
 * Merges swagger resources and methods.
 * @param oldObj - Existing resources object
 * @param newObj - Generated resources object
 * @param mergeTemplate - Template with merging instructionss
 * @returns - Merged resources object
 */
const recursivePathMerge = (
    oldObj: Record<string, any>,
    newObj: Record<string, any>,
    mergeTemplate: Record<string, any>
): Record<string, any> => {
    if (!mergeTemplate?.body) {
        return newObj;
    }

    const keys = Object.keys(mergeTemplate.body);

    if (mergeTemplate.type === "array") {
        // Resource/method presence is derived from the Synapse API, so the generated side is authoritative.
        const result = cloneDeep(newObj);
        if (mergeTemplate.primaryKey?.length) {
            const primaryKey = mergeTemplate.primaryKey;
            for (const key in newObj) {
                if (UNSAFE_OBJECT_KEYS.has(key)) {
                    continue;
                }
                const obj = newObj[key];
                const index = oldObj.findIndex((object: Record<string, any>) => {
                    return primaryKey.every((pk: string) => object[pk] === obj[pk]);
                });

                if (index > -1) {
                    result[key] = recursivePathMerge(oldObj[index], obj, mergeTemplate.body["*"]);
                }
            }
        }
        return result;
    }

    if (keys.length === 1 && keys[0] === "*") {
        // resources/methods are authoritative from the generated swagger.
        const result = cloneDeep(newObj);
        for (const key in newObj) {
            if (UNSAFE_OBJECT_KEYS.has(key)) {
                continue;
            }
            if (oldObj[key]) {
                result[key] = recursivePathMerge(oldObj[key], newObj[key], mergeTemplate.body["*"]);
            }
        }
        return result;
    }

    // Start from the existing operation so OpenAPI-only metadata survives, then sync just the tracked fields.
    const result = cloneDeep(oldObj);
    for (const key in mergeTemplate.body) {
        if (UNSAFE_OBJECT_KEYS.has(key)) {
            continue;
        }
        if (newObj[key] && oldObj[key]) {
            result[key] = recursivePathMerge(oldObj[key], newObj[key], mergeTemplate.body[key]);
        } else if (newObj[key] && !oldObj[key]) {
            result[key] = newObj[key];
        } else if (!newObj[key] && mergeTemplate.body[key]?.type === "array") {
            // Missing in the generated swagger means empty (e.g. all path params removed), so drop the old one.
            // Query params are added back later by mergeGeneratedSwagger.
            delete result[key];
        }
    }
    return result;
};

export const mergeSwaggers = (props: SwaggerUtilProps): Swagger => {
    const { existingSwagger, generatedSwagger } = props;

    return {
        ...generatedSwagger,
        paths: recursivePathMerge(existingSwagger.paths, generatedSwagger.paths, SWAGGER_PATH_TEMPLATE),
    };
};

export const getResourceInfo = (props: SwaggerUtilProps): ResourceInfoResponse => {
    const { existingSwagger, generatedSwagger } = props;
    const added: Resource[] = [];
    const removed: Resource[] = [];
    const updated: Resource[] = [];

    // Find newly added resources
    for (const resource in existingSwagger.paths) {
        if (!generatedSwagger.paths?.[resource]) {
            added.push({
                path: resource,
                methods: Object.keys(existingSwagger.paths[resource] ?? {}).map((method) => method.toUpperCase()),
            });
        } else {
            for (const method in existingSwagger.paths[resource]) {
                if (generatedSwagger.paths[resource][method]) {
                    updated.push({
                        path: resource,
                        methods: Object.keys(existingSwagger.paths[resource]).map((method) => method.toUpperCase()),
                    });
                }
            }
        }
    }

    // Find removed resources
    for (const resource in generatedSwagger.paths) {
        if (!existingSwagger.paths?.[resource]) {
            removed.push({
                path: resource,
                methods: Object.keys(generatedSwagger.paths[resource]).map((method) => method.toUpperCase()),
            });
        }
    }

    return { added, removed, updated };
};

/**
 * Extracts the "in: query" parameters of every path/method in a parsed swagger document,
 * keyed by resource path and lowercase HTTP method.
 */
export const extractQueryParams = (swagger: Swagger): Record<string, Record<string, QueryParamInfo[]>> => {
    const result: Record<string, Record<string, QueryParamInfo[]>> = {};
    for (const resourcePath in swagger.paths ?? {}) {
        const methods = swagger.paths[resourcePath] ?? {};
        for (const method in methods) {
            const queryParams: QueryParamInfo[] = (methods[method]?.parameters ?? [])
                .filter((param: any) => param.in === "query")
                .map((param: any) => ({ name: param.name, required: !!param.required }));
            if (queryParams.length > 0) {
                result[resourcePath] = result[resourcePath] ?? {};
                result[resourcePath][method] = queryParams;
            }
        }
    }
    return result;
};

const HTTP_METHODS = new Set(["get", "put", "post", "delete", "options", "head", "patch", "trace"]);

const parameterKey = (param: any): string => param?.$ref ?? `${param?.in}:${param?.name}`;

/**
 * Copies path-level parameters onto an operation, as the language server does when it regenerates the
 * swagger. Parameters the operation already declares take precedence.
 */
const withPathLevelParameters = (operation: any, pathParameters: any): any => {
    if (!Array.isArray(pathParameters) || pathParameters.length === 0) {
        return operation;
    }
    const ownParameters: any[] = operation.parameters ?? [];
    const ownKeys = new Set(ownParameters.map(parameterKey));
    const inherited = pathParameters.filter((param: any) => !ownKeys.has(parameterKey(param)));
    return inherited.length > 0 ? { ...operation, parameters: [...inherited, ...ownParameters] } : operation;
};

/**
 * Moves the given methods of a resource to its new path key (e.g. after a uri-template change),
 * preserving their operation data and path-level parameters. Other methods sharing the old path stay
 * there, and operations already at the new path are kept.
 */
const renameResourcePathInSwagger = (
        swagger: any,
        oldResourcePath: string,
        newResourcePath: string,
        methods: string[]): boolean => {
    const oldPathItem = swagger.paths?.[oldResourcePath];
    if (oldResourcePath === newResourcePath || !oldPathItem) {
        return false;
    }
    const newPathItem = swagger.paths[newResourcePath] ?? {};
    let moved = false;
    for (const method of methods) {
        if (oldPathItem[method] && !newPathItem[method]) {
            newPathItem[method] = withPathLevelParameters(oldPathItem[method], oldPathItem.parameters);
            delete oldPathItem[method];
            moved = true;
        }
    }
    if (!moved) {
        return false;
    }
    swagger.paths[newResourcePath] = newPathItem;
    if (!Object.keys(oldPathItem).some((key) => HTTP_METHODS.has(key))) {
        // Nothing left at the old path, so carry over its other fields (summary, description, etc.).
        // Its parameters were already copied onto the moved operations.
        for (const [key, value] of Object.entries(oldPathItem)) {
            if (key !== "parameters" && !UNSAFE_OBJECT_KEYS.has(key) && newPathItem[key] === undefined) {
                newPathItem[key] = value;
            }
        }
        delete swagger.paths[oldResourcePath];
    }
    return true;
};

/**
 * Renames a resource's path key in the given swagger YAML. Returns the updated YAML, or undefined
 * if there was nothing to rename.
 */
export const renameResourcePathInSwaggerYaml = (
        existingSwaggerYaml: string,
        oldResourcePath: string,
        newResourcePath: string,
        methods: string[]): string | undefined => {
    const swagger = parse(existingSwaggerYaml);
    if (!renameResourcePathInSwagger(swagger, oldResourcePath, newResourcePath, methods)) {
        return undefined;
    }
    return stringify(swagger, { aliasDuplicateObjects: false });
};

/**
 * Builds a query parameter from an existing one (if any), keeping its OpenAPI-only fields.
 */
const buildQueryParameter = (param: QueryParamInfo, existingParam?: any): any => ({
    ...existingParam,
    name: param.name,
    in: "query",
    required: param.required,
    schema: existingParam?.schema ?? { type: "string" },
});

/**
 * Replaces the "in: query" parameters of the given resource path/methods with the provided list,
 * leaving path/body parameters and every other field of the swagger document untouched. Query
 * parameters that already exist keep their other fields (description, schema, etc.).
 */
export const updateQueryParamsInSwagger = (
        existingSwaggerYaml: string,
        resourcePath: string,
        methods: string[],
        queryParams: QueryParamInfo[],
        onlyUpdateExisting: boolean = false): string => {
    const swagger = parse(existingSwaggerYaml);
    swagger.paths = swagger.paths ?? {};
    if (onlyUpdateExisting && !swagger.paths[resourcePath]) {
        return existingSwaggerYaml;
    }
    swagger.paths[resourcePath] = swagger.paths[resourcePath] ?? {};

    for (const method of methods) {
        if (onlyUpdateExisting && !swagger.paths[resourcePath][method]) {
            continue;
        }
        const operation = swagger.paths[resourcePath][method] ?? { responses: { default: { description: "Default response" } } };
        const parameters: any[] = operation.parameters ?? [];
        const remainingParams = parameters.filter((param: any) => param.in !== "query");
        const existingQueryParams = new Map(parameters
            .filter((param: any) => param.in === "query")
            .map((param: any) => [param.name, param]));
        const mergedParams = [
            ...remainingParams,
            ...queryParams.map((param) => buildQueryParameter(param, existingQueryParams.get(param.name))),
        ];
        if (mergedParams.length > 0) {
            operation.parameters = mergedParams;
        } else {
            delete operation.parameters;
        }
        swagger.paths[resourcePath][method] = operation;
    }

    return stringify(swagger, { aliasDuplicateObjects: false });
};

/**
 * Merges a freshly generated swagger into an existing one, preserving query params and other
 * OpenAPI-only content that only the existing file has.
 */
export const mergeGeneratedSwagger = (existingSwaggerYaml: string, generatedSwaggerYaml: string): string => {
    const parsedExistingSwagger = parse(existingSwaggerYaml);
    const queryParams = extractQueryParams(parsedExistingSwagger);

    const mergedContent = mergeSwaggers({
        existingSwagger: parsedExistingSwagger,
        generatedSwagger: parse(generatedSwaggerYaml),
    });
    let yamlContent = stringify(mergedContent, { aliasDuplicateObjects: false });

    // Synapse API XML has no query param concept, so mergeSwaggers cannot carry them over.
    // Setting onlyUpdateExisting=true so resources/methods that mergeSwaggers already dropped aren't resurrected.
    for (const [resourcePath, methodMap] of Object.entries(queryParams)) {
        for (const [method, methodQueryParams] of Object.entries(methodMap)) {
            yamlContent = updateQueryParamsInSwagger(yamlContent, resourcePath, [method], methodQueryParams, true);
        }
    }
    return yamlContent;
};

export type UnsavedSwaggerResolution = "clean" | "saved" | "discarded" | "cancelled";

// Swagger files saved by resolveUnsavedSwaggerChanges, whose save-triggered API comparison is skipped.
export const swaggerSavesSkippingComparison = new Set<string>();

/**
 * Swagger writers work on the file on disk. If it is open with unsaved changes, asks the user to save or
 * discard them first. Dismissing the dialog saves them, unless the caller can cancel its change.
 */
export async function resolveUnsavedSwaggerChanges(
        swaggerPath: string,
        canCancel: boolean = false): Promise<UnsavedSwaggerResolution> {
    const document = workspace.textDocuments.find((doc) => doc.uri.fsPath === swaggerPath);
    if (!document?.isDirty) {
        return "clean";
    }
    const selection = await window.showWarningMessage(
        `${path.basename(swaggerPath)} has unsaved changes. Save or discard them to update it with the API changes.`,
        { modal: true },
        "Save",
        "Discard"
    );
    if (selection === "Discard") {
        // Revert only works on the active editor.
        await window.showTextDocument(document);
        await vscode.commands.executeCommand("workbench.action.files.revert");
        return document.isDirty ? "cancelled" : "discarded";
    }
    if (!selection && canCancel) {
        return "cancelled";
    }
    // The comparison would run before the API change is applied; the caller's update syncs them instead.
    swaggerSavesSkippingComparison.add(swaggerPath);
    if (!(await document.save())) {
        swaggerSavesSkippingComparison.delete(swaggerPath);
        return "cancelled";
    }
    return "saved";
}

/**
 * Copies the "in: query" parameters from a source swagger definition (e.g. an imported OpenAPI
 * spec) into the generated swagger file for the given API, leaving all other fields untouched.
 */
export async function copyQueryParamsFromSource(apiPath: string, sourceSwaggerPath: string): Promise<void> {
    if (!fs.existsSync(sourceSwaggerPath)) {
        return;
    }
    const sourceQueryParams = extractQueryParams(parse(fs.readFileSync(sourceSwaggerPath, 'utf8')));
    if (Object.keys(sourceQueryParams).length === 0) {
        return;
    }

    const projectUri = workspace.getWorkspaceFolder(vscode.Uri.file(apiPath))?.uri.fsPath;
    if (!projectUri) {
        return;
    }
    const swaggerPath = path.join(projectUri, SWAGGER_REL_DIR, `${path.basename(apiPath, ".xml")}.yaml`);
    if (!fs.existsSync(swaggerPath) || (await resolveUnsavedSwaggerChanges(swaggerPath)) === "cancelled") {
        return;
    }

    let swaggerContent = fs.readFileSync(swaggerPath, 'utf-8');
    for (const [resourcePath, methodMap] of Object.entries(sourceQueryParams)) {
        for (const [method, queryParams] of Object.entries(methodMap)) {
            swaggerContent = updateQueryParamsInSwagger(swaggerContent, resourcePath, [method], queryParams, true);
        }
    }
    fs.writeFileSync(swaggerPath, swaggerContent);
}

const mergeWithGeneratedSwagger = async (apiPath: string, projectUri: string, swaggerPath: string): Promise<string | undefined> => {
    const existingSwaggerYaml = fs.existsSync(swaggerPath) ? fs.readFileSync(swaggerPath, 'utf-8') : undefined;
    const langClient = await MILanguageClient.getInstance(projectUri);
    const response = await langClient.swaggerFromAPI({ apiPath: apiPath, ...(existingSwaggerYaml && { swaggerPath: swaggerPath }) });
    const freshlyGeneratedSwagger = response.swagger;
    return existingSwaggerYaml
        ? mergeGeneratedSwagger(existingSwaggerYaml, freshlyGeneratedSwagger)
        : freshlyGeneratedSwagger;
};

/**
 * Core of generateSwagger, without acquiring the file lock. Only call from within an action
 * already passed to withSwaggerFileLock for the same swaggerPath; otherwise call generateSwagger().
 */
export async function generateSwaggerCore(apiPath: string, projectUri: string, swaggerPath: string): Promise<string | undefined> {
    const dirPath = path.dirname(swaggerPath);
    if (!fs.existsSync(dirPath)) {
        fs.mkdirSync(dirPath, { recursive: true });
    }
    let merged = await mergeWithGeneratedSwagger(apiPath, projectUri, swaggerPath);
    const existingSwaggerYaml = fs.existsSync(swaggerPath) ? fs.readFileSync(swaggerPath, 'utf-8') : undefined;
    // Most API saves (e.g. mediator edits) leave the definition as is; no need to prompt for those.
    if (merged && existingSwaggerYaml && isEqual(parse(merged), parse(existingSwaggerYaml))) {
        return merged;
    }
    const resolution = await resolveUnsavedSwaggerChanges(swaggerPath);
    if (resolution === "cancelled") {
        return undefined;
    }
    if (resolution === "saved") {
        // The saved editor content is now the existing definition to merge into.
        merged = await mergeWithGeneratedSwagger(apiPath, projectUri, swaggerPath);
    }
    if (merged) {
        fs.writeFileSync(swaggerPath, merged);
    }
    return merged;
}

export function generateSwagger(apiPath: string): Promise<SwaggerFromAPIResponse> {
    return new Promise(async (resolve) => {
        const projectUri = workspace.getWorkspaceFolder(vscode.Uri.file(apiPath))?.uri.fsPath;
        if (!projectUri) {
            resolve({ generatedSwagger: undefined });
            return;
        }
        const swaggerPath = path.join(projectUri, SWAGGER_REL_DIR, path.basename(apiPath, path.extname(apiPath)) + '.yaml');
        // Runs on every API save, racing query-param edits to the same file; the lock
        // serializes them so neither reads a stale write.
        const generatedSwagger = await withSwaggerFileLock(swaggerPath, () => generateSwaggerCore(apiPath, projectUri, swaggerPath));
        resolve({ generatedSwagger });
    });
}

export function deleteSwagger(apiPath: string) {
    const projectRoot = workspace.getWorkspaceFolder(vscode.Uri.file(apiPath))?.uri.fsPath;
    if (!projectRoot) {
        return;
    }
    const swaggerDir = path.join(projectRoot!, SWAGGER_REL_DIR);
    const swaggerFilePath = path.join(swaggerDir, path.basename(apiPath, path.extname(apiPath)) + '.yaml');
    if (fs.existsSync(swaggerFilePath)) {
        window.showInformationMessage(`API file ${path.basename(apiPath)} has been deleted. Do you want to delete the related Swagger file?`, 'Yes', 'No').then(async answer => {
            if (answer === 'Yes') {
                await deleteRegistryResource(swaggerFilePath);
                window.showInformationMessage(`Swagger file ${path.basename(swaggerFilePath)} has been deleted.`);
                vscode.commands.executeCommand(COMMANDS.REFRESH_COMMAND);
            }
        });
    }
}
