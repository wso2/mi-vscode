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
import * as fs from "fs";
import * as path from "path";
import { XMLParser, XMLBuilder } from "fast-xml-parser";
import { ConsolidatedProjectDetails, ConsolidatedRemoteDeployConfig } from "@wso2/mi-core";

// Read-only parser: returns the convenient object form, used when we only inspect values.
const readOnlyParser = () => new XMLParser({ ignoreAttributes: false, parseTagValue: false });

// Editor parser/builder pair: preserveOrder mode keeps comments and tag order intact for round-trip edits.
const editor = () => ({
    parser: new XMLParser({ ignoreAttributes: false, parseTagValue: false, preserveOrder: true }),
    builder: new XMLBuilder({ ignoreAttributes: false, preserveOrder: true, format: true, indentBy: '    ' }),
});

// In preserveOrder mode each child is a single-key object inside an array.
function findChild(nodes: any[], tag: string): any | undefined {
    return nodes.find((n: any) => n[tag] !== undefined);
}

// Returns the tag's children array, creating an empty one under a new node if absent.
function findOrCreateChild(nodes: any[], tag: string): any[] {
    const node = findChild(nodes, tag);
    if (node) return node[tag];
    const children: any[] = [];
    nodes.push({ [tag]: children });
    return children;
}

function isCarDeployPlugin(pluginNode: any): boolean {
    const children: any[] = Array.isArray(pluginNode.plugin) ? pluginNode.plugin : [];
    return children.some((c: any) =>
        c.artifactId && [c.artifactId].flat().some((v: any) =>
            (v['#text'] ?? v) === 'maven-car-deploy-plugin'
        )
    );
}

function setScalar(nodes: any[], tag: string, value: string): void {
    const node = findChild(nodes, tag);
    if (node) {
        node[tag] = [{ '#text': value }];
    } else {
        nodes.push({ [tag]: [{ '#text': value }] });
    }
}

function findProjectChildren(pom: any[]): any[] | null {
    const projectNode = pom.find((n: any) => n.project !== undefined);
    return projectNode ? projectNode.project : null;
}

function applyDetailsToProperties(propsArr: any[], details: ConsolidatedProjectDetails): void {
    setScalar(propsArr, 'project.runtime.version', details.runtimeVersion);
    setScalar(propsArr, 'car.plugin.version', details.cappBuildPluginVersion);
    setScalar(propsArr, 'dockerfile.base.image', details.dockerBaseImage);
}

// Read–mutate–write helper. Mutator may return false to skip the write (no relevant node to touch).
async function editPom(pomPath: string, mutate: (projectChildren: any[]) => boolean | void): Promise<boolean> {
    try {
        const content = await fs.promises.readFile(pomPath, 'utf-8');
        const { parser, builder } = editor();
        const pom = parser.parse(content);
        const projectChildren = findProjectChildren(pom);
        if (!projectChildren) return false;
        if (mutate(projectChildren) === false) return true;
        await fs.promises.writeFile(pomPath, builder.build(pom), 'utf-8');
        return true;
    } catch {
        return false;
    }
}

export async function readConsolidatedProjectDetails(consolidatedRoot: string): Promise<ConsolidatedProjectDetails | null> {
    const pomPath = path.join(consolidatedRoot, 'pom.xml');
    try {
        const content = await fs.promises.readFile(pomPath, 'utf-8');
        const pom = readOnlyParser().parse(content);
        const proj = pom?.project ?? {};
        const props = proj?.properties ?? {};

        const groupId = String(proj.groupId ?? '');
        const artifactId = String(proj.artifactId ?? '');
        const version = String(proj.version ?? '');

        const context: Record<string, string> = {
            'project.groupId': groupId,
            'project.artifactId': artifactId,
            'project.version': version,
        };
        for (const [k, v] of Object.entries(props)) {
            context[k] = String(v);
        }

        const resolve = (value: string): string => {
            let prev = '';
            let current = value;
            while (current !== prev) {
                prev = current;
                current = current.replace(/\$\{([^}]+)\}/g, (_, key) => context[key] ?? `\${${key}}`);
            }
            return current;
        };

        return {
            groupId,
            artifactId,
            version,
            runtimeVersion: resolve(String(props['project.runtime.version'] ?? '')),
            cappBuildPluginVersion: resolve(String(props['car.plugin.version'] ?? '')),
            dockerBaseImage: resolve(String(props['dockerfile.base.image'] ?? '')),
        };
    } catch {
        return null;
    }
}

export async function writeConsolidatedProjectDetails(
    consolidatedRoot: string,
    details: ConsolidatedProjectDetails,
    subprojectPaths: string[],
): Promise<boolean> {
    const rootPomPath = path.join(consolidatedRoot, 'pom.xml');
    const rootWriteSucceeded = await editPom(rootPomPath, (projectChildren) => {
        setScalar(projectChildren, 'groupId', details.groupId);
        setScalar(projectChildren, 'artifactId', details.artifactId);
        setScalar(projectChildren, 'version', details.version);
        const propsNode = findChild(projectChildren, 'properties');
        if (propsNode) applyDetailsToProperties(propsNode.properties, details);
    });
    if (!rootWriteSucceeded) return false;

    let allSubprojectWritesSucceeded = true;
    for (const subprojectPath of subprojectPaths) {
        const pomPath = path.join(subprojectPath, 'pom.xml');
        if (!fs.existsSync(pomPath)) continue;
        const subprojectWriteSucceeded = await editPom(pomPath, (projectChildren) => {
            const parentNode = findChild(projectChildren, 'parent');
            if (parentNode) {
                setScalar(parentNode.parent, 'groupId', details.groupId);
                setScalar(parentNode.parent, 'artifactId', details.artifactId);
                setScalar(parentNode.parent, 'version', details.version);
            }
            const propsNode = findChild(projectChildren, 'properties');
            if (propsNode) applyDetailsToProperties(propsNode.properties, details);
        });
        if (!subprojectWriteSucceeded) allSubprojectWritesSucceeded = false;
    }
    return allSubprojectWritesSucceeded;
}

export async function readConsolidatedRemoteDeployConfig(consolidatedRoot: string): Promise<ConsolidatedRemoteDeployConfig | null> {
    const pomPath = path.join(consolidatedRoot, 'pom.xml');
    try {
        const content = await fs.promises.readFile(pomPath, 'utf-8');
        const pom = readOnlyParser().parse(content);
        const proj = pom?.project ?? {};
        const props = proj?.properties ?? {};
        const isEnabled = String(props['is.remote.deployment.enabled'] ?? 'false').toLowerCase() === 'true';

        const plugins = [proj?.build?.pluginManagement?.plugins?.plugin ?? []].flat();
        const carDeployPlugin = plugins.find((p: any) =>
            String(p?.artifactId ?? '') === 'maven-car-deploy-plugin'
        );

        if (!carDeployPlugin) return null;

        const server = [carDeployPlugin?.configuration?.carbonServers?.CarbonServer ?? []].flat()[0] ?? {};
        return {
            serverUrl: String(server?.serverUrl ?? ''),
            username: String(server?.userName ?? ''),
            password: String(server?.password ?? ''),
            truststorePath: String(server?.trustStorePath ?? ''),
            truststorePassword: String(server?.trustStorePassword ?? ''),
            truststoreType: String(server?.trustStoreType ?? ''),
            serverType: String(server?.serverType ?? ''),
            isEnabled,
        };
    } catch {
        return null;
    }
}

export async function writeConsolidatedRemoteDeployConfig(
    consolidatedRoot: string,
    config: ConsolidatedRemoteDeployConfig,
    subprojectPaths: string[],
): Promise<boolean> {
    const rootPomPath = path.join(consolidatedRoot, 'pom.xml');
    const rootWriteSucceeded = await editPom(rootPomPath, (projectChildren) => {
        const propsNode = findChild(projectChildren, 'properties');
        if (propsNode) {
            const propsArr: any[] = propsNode.properties;
            const existing = propsArr.find((n: any) => n['is.remote.deployment.enabled'] !== undefined);
            if (existing) {
                existing['is.remote.deployment.enabled'] = [{ '#text': String(config.isEnabled) }];
            } else {
                propsArr.push({ 'is.remote.deployment.enabled': [{ '#text': String(config.isEnabled) }] });
            }
        }

        // Only touch the maven-car-deploy-plugin entry, so any other build configuration
        // a user has added to the root pom (other plugins, pluginManagement entries, etc.) survives.
        if (config.isEnabled) {
            const serverChildren: any[] = [
                { userName: [{ '#text': config.username }] },
                { password: [{ '#text': config.password }] },
                { serverUrl: [{ '#text': config.serverUrl }] },
            ];
            if (config.truststorePath) serverChildren.push({ trustStorePath: [{ '#text': config.truststorePath }] });
            if (config.truststorePassword) serverChildren.push({ trustStorePassword: [{ '#text': config.truststorePassword }] });
            if (config.truststoreType) serverChildren.push({ trustStoreType: [{ '#text': config.truststoreType }] });
            if (config.serverType) serverChildren.push({ serverType: [{ '#text': config.serverType }] });

            const newPlugin = {
                plugin: [
                    { groupId: [{ '#text': 'org.wso2.maven' }] },
                    { artifactId: [{ '#text': 'maven-car-deploy-plugin' }] },
                    { version: [{ '#text': '5.2.44' }] },
                    { configuration: [{ carbonServers: [{ CarbonServer: serverChildren }] }] },
                    {
                        executions: [{
                            execution: [
                                { phase: [{ '#text': 'deploy' }] },
                                { goals: [{ goal: [{ '#text': 'deploy-car' }] }] },
                            ]
                        }]
                    },
                ]
            };

            const buildChildren = findOrCreateChild(projectChildren, 'build');
            const pluginManagementChildren = findOrCreateChild(buildChildren, 'pluginManagement');
            const plugins = findOrCreateChild(pluginManagementChildren, 'plugins');
            const existingIdx = plugins.findIndex((n: any) => n.plugin && isCarDeployPlugin(n));
            if (existingIdx >= 0) {
                plugins[existingIdx] = newPlugin;
            } else {
                plugins.push(newPlugin);
            }
        } else {
            const buildNode = findChild(projectChildren, 'build');
            const pluginManagementNode = buildNode && findChild(buildNode.build, 'pluginManagement');
            const pluginsNode = pluginManagementNode && findChild(pluginManagementNode.pluginManagement, 'plugins');
            if (pluginsNode) {
                pluginsNode.plugins = (pluginsNode.plugins as any[]).filter((n: any) => !(n.plugin && isCarDeployPlugin(n)));
            }
        }
    });
    if (!rootWriteSucceeded) return false;

    return cascadeDeployPluginToSubprojects(subprojectPaths, config.isEnabled);
}

async function cascadeDeployPluginToSubprojects(subprojectPaths: string[], enable: boolean): Promise<boolean> {
    const minimalPlugin = {
        plugin: [
            { groupId: [{ '#text': 'org.wso2.maven' }] },
            { artifactId: [{ '#text': 'maven-car-deploy-plugin' }] },
        ]
    };

    let allSubprojectWritesSucceeded = true;
    for (const subprojectPath of subprojectPaths) {
        if (path.basename(subprojectPath) === 'docker-build') continue;
        const pomPath = path.join(subprojectPath, 'pom.xml');
        if (!fs.existsSync(pomPath)) continue;
        const subprojectWriteSucceeded = await editPom(pomPath, (projectChildren) => {
            if (enable) {
                const buildChildren = findOrCreateChild(projectChildren, 'build');
                const plugins = findOrCreateChild(buildChildren, 'plugins');
                const alreadyExists = plugins.some((n: any) => n.plugin && isCarDeployPlugin(n));
                if (!alreadyExists) {
                    plugins.push(minimalPlugin);
                }
            } else {
                const buildNode = findChild(projectChildren, 'build');
                if (!buildNode) return false;
                const pluginsNode = findChild(buildNode.build as any[], 'plugins');
                if (!pluginsNode) return false;
                pluginsNode.plugins = (pluginsNode.plugins as any[]).filter((n: any) => !(n.plugin && isCarDeployPlugin(n)));
            }
        });
        if (!subprojectWriteSucceeded) allSubprojectWritesSucceeded = false;
    }
    return allSubprojectWritesSucceeded;
}
