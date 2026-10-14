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

// No vscode imports — this module is intentionally VS Code-agnostic so it can
// be reused by a standalone CLI without modification.

import * as fs from 'fs';
import * as path from 'path';
import { XMLParser } from 'fast-xml-parser';
import { Nature, FileInfo, getProjectDetails } from './migrationCore';

export interface MigrationFinding {
    severity: 'breaking' | 'warning';
    category: string;
    message: string;
    filePath?: string;
}

export interface AnalysisContext {
    source: string;
    projectDirsWithType: { projectDir: string; projectType: Nature }[];
    artifactIdToFileInfoMap: Map<string, FileInfo>;
    projectDirToResolvedPomMap: Map<string, string>;
    /** Source MI runtime version (e.g. "4.2.0"). Used to skip rules that don't apply to this migration path. */
    runtimeVersion?: string;
    /** Target MI runtime version to migrate to (e.g. "4.6.0"). Defaults to LATEST_MI_VERSION. */
    targetVersion?: string;
}

/** Project-wide rule — receives the full analysis context. */
type AnalysisRule = (ctx: AnalysisContext) => MigrationFinding[];

/** A single synapse-config XML file as seen by the walker. */
interface SynapseFile {
    filePath: string;
    content: string;
    artifactName: string | null;
}

/**
 * Aggregate rule — called once with ALL synapse files; reports findings as
 * a single entry listing all affected artifacts.
 */
type SynapseAggregateRule = (files: SynapseFile[]) => MigrationFinding[];

// ---------------------------------------------------------------------------
// Connector version requirements
// Add a row here when a connector requires a minimum version for MI 4.x.
// ---------------------------------------------------------------------------

interface ConnectorVersionRequirement {
    namePattern: string;
    breakingBelow: string;
    recommendation: string;
}

const CONNECTOR_VERSION_REQUIREMENTS: ConnectorVersionRequirement[] = [
    {
        namePattern: 'mongodb',
        breakingBelow: '2.0.0',
        recommendation:
            'MongoDB connector 1.x is incompatible with MI 4.x. Replace with MongoDB connector 2.x and update the configuration accordingly.',
    },
    {
        namePattern: 'pcml',
        breakingBelow: '2.0.0',
        recommendation:
            'PCML connector 1.x is not supported in MI. Replace with PCML connector 2.x and update the configuration accordingly.',
    },
];

// Known synapse-config subfolder names that the migration handles.
const KNOWN_SYNAPSE_ARTIFACT_TYPES = new Set([
    'api',
    'endpoints',
    'inbound-endpoints',
    'local-entries',
    'message-processors',
    'message-stores',
    'proxy-services',
    'sequences',
    'tasks',
    'templates',
]);

// language="nashornJs" explicitly targets the Nashorn engine (deprecated from JDK 15+).
const NASHORN_SCRIPT_LANGUAGE = 'nashornjs';

// Before MI 4.3.0, language="js" routed to the Rhino engine (which supports E4X).
// From MI 4.3.0 onward it routes to GraalVM JS, which does not support E4X.
// Users who relied on E4X must change their language attribute to "rhinoJs".
const RHINO_SCRIPT_LANGUAGE = 'js';

// ---------------------------------------------------------------------------
// Rules
// ---------------------------------------------------------------------------

/**
 * Flags CLASS mediator projects. The pom.xml gets synapse-core added
 * automatically, but the version may need manual tuning.
 */
function classMediatorRule(ctx: AnalysisContext): MigrationFinding[] {
    return ctx.projectDirsWithType
        .filter(({ projectType }) => projectType === Nature.CLASS)
        .map(({ projectDir }) => ({
            severity: 'warning' as const,
            category: 'class-mediator',
            message:
                'Class mediator module detected. A synapse-core dependency will be added to pom.xml automatically, but the version may need manual adjustment.',
            filePath: projectDir,
        }));
}

/**
 * Flags nashornJs script mediators. Nashorn was removed from JDK 15+; users
 * on JDK 15 or above must manually deploy the Nashorn runtime JARs into
 * <MI_HOME>/lib to keep these scripts working.
 */
function scriptMediatorNashornRule(files: SynapseFile[]): MigrationFinding[] {
    const affected = files
        .filter(f => extractScriptLanguages(f.content).some(l => l.toLowerCase() === NASHORN_SCRIPT_LANGUAGE))
        .map(f => f.artifactName ?? path.basename(f.filePath));
    if (affected.length === 0) return [];
    return [{
        severity: 'warning',
        category: 'script-mediator-nashorn',
        message: `Script mediator with language="nashornJs" detected in artifact(s): ${affected.join(', ')}. Nashorn is removed from JDK 15 and above. It is recommended to migrate the script to be compatible with the GraalVM JS engine (language="js"). If migration is not feasible, manually copy nashorn-core-15.4.jar and asm-util-9.5.jar to <MI_HOME>/lib.`,
    }];
}

/**
 * Warns about script mediators using language="js", which routed to Rhino
 * before MI 4.3.0 but routes to GraalVM JS from MI 4.3.0 onward. GraalVM JS
 * does not support E4X — scripts relying on E4X must switch to "rhinoJs".
 */
function scriptMediatorGraalVmRule(files: SynapseFile[]): MigrationFinding[] {
    const affected = files
        .filter(f => extractScriptLanguages(f.content).some(l => l.toLowerCase() === RHINO_SCRIPT_LANGUAGE))
        .map(f => f.artifactName ?? path.basename(f.filePath));
    if (affected.length === 0) return [];
    return [{
        severity: 'warning',
        category: 'script-mediator-rhino',
        message: `Script mediator with language="js" detected in artifact(s): ${affected.join(', ')}. Before MI 4.3.0 this used the Rhino engine (which supports E4X); from MI 4.3.0 onward it uses GraalVM JS, which does not support E4X. It is recommended to migrate the script to be compatible with the GraalVM JS engine. If migration is not feasible and the script uses E4X syntax, update the language attribute to "rhinoJs" to continue using the Rhino engine.`,
    }];
}

/**
 * Flags connector artifacts whose version is below the required minimum
 * for MI 4.x compatibility.
 */
function connectorVersionRule(ctx: AnalysisContext): MigrationFinding[] {
    const findings: MigrationFinding[] = [];

    // IS workspace: CONNECTOR project entries carry version via connector.xml
    for (const [, fileInfo] of ctx.artifactIdToFileInfoMap) {
        if (fileInfo.projectType !== Nature.CONNECTOR || !fileInfo.artifact) continue;

        const name: string = fileInfo.artifact['@_name'] ?? '';
        const version: string = fileInfo.artifact['@_version'] ?? '';
        if (!name || !version) continue;

        for (const rule of CONNECTOR_VERSION_REQUIREMENTS) {
            if (!name.toLowerCase().includes(rule.namePattern)) continue;
            if (isVersionBelow(version, rule.breakingBelow)) {
                findings.push({
                    severity: 'breaking',
                    category: 'connector-version',
                    message: `${name} v${version} detected. ${rule.recommendation}`,
                    filePath: fileInfo.path,
                });
            }
        }
    }

    // VS Code MI project: connectors are Maven dependencies — check pom.xml
    const parser = new XMLParser({ ignoreAttributes: false, parseTagValue: false });
    for (const { projectDir, projectType } of ctx.projectDirsWithType) {
        if (projectType !== Nature.MI_ESB) continue;

        let pomContent = ctx.projectDirToResolvedPomMap.get(projectDir) ?? '';
        if (!pomContent) {
            const pomPath = path.join(projectDir, 'pom.xml');
            if (!fs.existsSync(pomPath)) continue;
            try { pomContent = fs.readFileSync(pomPath, 'utf-8'); } catch { continue; }
        }

        const parsed = parser.parse(pomContent);
        const rawDeps = parsed?.project?.dependencies?.dependency;
        if (!rawDeps) continue;
        const deps: unknown[] = Array.isArray(rawDeps) ? rawDeps : [rawDeps];

        for (const dep of deps) {
            const artifactId: string = (dep as Record<string, string>)?.artifactId ?? '';
            const version: string = (dep as Record<string, string>)?.version ?? '';
            if (!artifactId || !version) continue;
            for (const rule of CONNECTOR_VERSION_REQUIREMENTS) {
                if (!artifactId.toLowerCase().includes(rule.namePattern)) continue;
                if (isVersionBelow(version, rule.breakingBelow)) {
                    findings.push({
                        severity: 'breaking',
                        category: 'connector-version',
                        message: `${artifactId} v${version} detected. ${rule.recommendation}`,
                        filePath: path.join(projectDir, 'pom.xml'),
                    });
                }
            }
        }

        // Manually added connector zips (src/main/wso2mi/resources/connectors/)
        const connectorZipDir = path.join(projectDir, 'src', 'main', 'wso2mi', 'resources', 'connectors');
        if (fs.existsSync(connectorZipDir)) {
            try {
                for (const entry of fs.readdirSync(connectorZipDir)) {
                    if (!entry.endsWith('.zip')) continue;
                    const zipName = entry.slice(0, -4);
                    // Take the last version-like token from the filename (e.g. mongodb-connector-1.0.0 → 1.0.0)
                    const versions = Array.from(zipName.matchAll(/(\d+\.\d+(?:\.\d+)*)/g));
                    if (versions.length === 0) continue;
                    const version = versions[versions.length - 1][1];
                    for (const rule of CONNECTOR_VERSION_REQUIREMENTS) {
                        if (!zipName.toLowerCase().includes(rule.namePattern)) continue;
                        if (isVersionBelow(version, rule.breakingBelow)) {
                            findings.push({
                                severity: 'breaking',
                                category: 'connector-version',
                                message: `${zipName} v${version} detected. ${rule.recommendation}`,
                                filePath: path.join(connectorZipDir, entry),
                            });
                        }
                    }
                }
            } catch { /* skip unreadable */ }
        }
    }

    return findings;
}

/**
 * Flags artifact types inside ESB projects that are not in the known
 * synapse-config folder list — these will be silently skipped during migration.
 */
function unknownArtifactTypeRule(ctx: AnalysisContext): MigrationFinding[] {
    const findings: MigrationFinding[] = [];

    for (const { projectDir, projectType } of ctx.projectDirsWithType) {
        if (projectType !== Nature.ESB) continue;

        const synapseConfigDir = path.join(projectDir, 'src', 'main', 'synapse-config');
        if (!fs.existsSync(synapseConfigDir)) continue;

        try {
            const entries = fs.readdirSync(synapseConfigDir, { withFileTypes: true });
            for (const entry of entries) {
                if (entry.isDirectory() && !KNOWN_SYNAPSE_ARTIFACT_TYPES.has(entry.name)) {
                    findings.push({
                        severity: 'warning',
                        category: 'unknown-artifact-type',
                        message: `Artifact folder "${entry.name}" is not recognised by the migration tool and will be skipped.`,
                        filePath: path.join(synapseConfigDir, entry.name),
                    });
                }
            }
        } catch {
            // skip unreadable dirs
        }
    }

    return findings;
}

/**
 * Flags artifacts referenced in a DISTRIBUTION project's pom.xml that cannot
 * be resolved in the artifact map — they will not be migrated.
 */
function missingDependencyRule(ctx: AnalysisContext): MigrationFinding[] {
    const findings: MigrationFinding[] = [];
    const parser = new XMLParser({ ignoreAttributes: false, parseTagValue: false });

    for (const { projectDir, projectType } of ctx.projectDirsWithType) {
        if (projectType !== Nature.DISTRIBUTION) continue;

        const pomPath = path.join(projectDir, 'pom.xml');
        if (!fs.existsSync(pomPath)) continue;

        let pomContent = ctx.projectDirToResolvedPomMap.get(projectDir) ?? '';
        if (!pomContent) {
            try {
                pomContent = fs.readFileSync(pomPath, 'utf-8');
            } catch {
                continue;
            }
        }

        const parsed = parser.parse(pomContent);
        const rawDeps = parsed?.project?.dependencies?.dependency;
        if (!rawDeps) continue;

        const deps: any[] = Array.isArray(rawDeps) ? rawDeps : [rawDeps];
        for (const dep of deps) {
            const depId = `${dep.groupId}:${dep.artifactId}:${dep.version}`;
            if (!ctx.artifactIdToFileInfoMap.has(depId)) {
                findings.push({
                    severity: 'breaking',
                    category: 'missing-dependency',
                    message: `Dependency "${depId}" declared in the CApp pom.xml was not found in the project. It will not be migrated.`,
                    filePath: pomPath,
                });
            }
        }
    }

    return findings;
}

/**
 * Flags subdirectories that have no recognised project nature and will be
 * silently ignored during migration.
 */
function unrecognizedProjectTypeRule(ctx: AnalysisContext): MigrationFinding[] {
    // Rule only makes sense at workspace/multi-module level. If no sub-projects were
    // detected, ctx.source is itself a leaf project — its own subdirs are not "projects".
    if (ctx.projectDirsWithType.length === 0) return [];

    // VS Code MI projects don't use Eclipse workspace layout — composite exporters
    // and other module dirs won't have .project files, so skip the rule entirely.
    if (ctx.projectDirsWithType.some(p => p.projectType === Nature.MI_ESB)) return [];

    const knownDirs = new Set(ctx.projectDirsWithType.map(({ projectDir }) => projectDir));
    const findings: MigrationFinding[] = [];

    try {
        const entries = fs.readdirSync(ctx.source, { withFileTypes: true });
        for (const entry of entries) {
            if (!entry.isDirectory()) continue;
            const fullPath = path.join(ctx.source, entry.name);
            if (['.backup', '.git', '.vscode', '.mvn', '.metadata', '.tmp'].includes(entry.name)) continue;
            if (!knownDirs.has(fullPath)) {
                // Maven modules (have pom.xml) are intentional — just not a recognised IS type.
                if (fs.existsSync(path.join(fullPath, 'pom.xml'))) continue;
                findings.push({
                    severity: 'warning',
                    category: 'unrecognized-project',
                    message: `Directory "${entry.name}" has no recognised project nature and will be ignored during migration.`,
                    filePath: fullPath,
                });
            }
        }
    } catch {
        // skip unreadable root
    }

    return findings;
}

/**
 * Flags artifacts that exist in the source but are not referenced by any
 * DISTRIBUTION (CApp) project — they will be written to
 * skipped-files-during-migration.txt but not migrated.
 */
function skippedArtifactsRule(ctx: AnalysisContext): MigrationFinding[] {
    const distributionPomDeps = new Set<string>();
    const parser = new XMLParser({ ignoreAttributes: false, parseTagValue: false });

    for (const { projectDir, projectType } of ctx.projectDirsWithType) {
        if (projectType !== Nature.DISTRIBUTION) continue;

        const pomPath = path.join(projectDir, 'pom.xml');
        if (!fs.existsSync(pomPath)) continue;

        let pomContent = ctx.projectDirToResolvedPomMap.get(projectDir) ?? '';
        if (!pomContent) {
            try { pomContent = fs.readFileSync(pomPath, 'utf-8'); } catch { continue; }
        }

        const parsed = parser.parse(pomContent);
        const rawDeps = parsed?.project?.dependencies?.dependency;
        if (!rawDeps) continue;

        const deps: any[] = Array.isArray(rawDeps) ? rawDeps : [rawDeps];
        for (const dep of deps) {
            distributionPomDeps.add(`${dep.groupId}:${dep.artifactId}:${dep.version}`);
        }
    }

    const skipped: string[] = [];
    for (const [artifactId, fileInfo] of ctx.artifactIdToFileInfoMap) {
        if (fileInfo.artifact && !distributionPomDeps.has(artifactId)) {
            skipped.push(fileInfo.path);
        }
    }

    if (skipped.length === 0) return [];

    return [{
        severity: 'warning',
        category: 'skipped-artifacts',
        message: `${skipped.length} artifact(s) are not referenced by any CApp and will not be migrated. They will be listed in .backup/skipped-files-during-migration.txt.`,
    }];
}

/**
 * Flags local transport URIs (local://...) which are not supported in MI.
 * Example fix: local://services/SecondProxy → http://localhost:8290/services/SecondProxy
 */
/**
 * Flags proxy services that have no explicit `transports` attribute. In older
 * ESB versions the default included local transport as a listener; MI does not
 * support local transport, so HTTP/HTTPS must be declared explicitly.
 */
function localTransportListenerProxyRule(files: SynapseFile[]): MigrationFinding[] {
    const affected: string[] = [];
    for (const { content, artifactName, filePath } of files) {
        const proxyTagMatch = content.match(/<proxy\b([^>]*)>/i);
        if (!proxyTagMatch) continue;
        const attrs = proxyTagMatch[1];
        const transportsMatch = attrs.match(/\btransports\s*=\s*["']([^"']+)["']/i);
        const isLocalOnly = !transportsMatch || transportsMatch[1].trim().toLowerCase() === 'local';
        if (isLocalOnly) {
            affected.push(artifactName ?? path.basename(filePath));
        }
    }
    if (affected.length === 0) return [];
    return [{
        severity: 'breaking',
        category: 'local-transport-listener-proxy',
        message: `Proxy service(s) without an explicit transports attribute detected: ${affected.join(', ')}. In MI, local transport is not supported. Explicitly add the HTTP/HTTPS transports (e.g. transports="http https") to ensure the proxy service is accessible.`,
    }];
}

/**
 * Flags artifacts that contain a Data Mapper mediator. From MI 4.2.0, the Saxon
 * version was upgraded, requiring the reserved namespaces in the default XSLT
 * stylesheet to be updated. The Data Mapper Configuration Migration Tool
 * provided by WSO2 Support must be used to migrate these configurations.
 */
function dataMapperSaxonRule(files: SynapseFile[]): MigrationFinding[] {
    const affected = files
        .filter(f => /<datamapper\b/i.test(f.content))
        .map(f => f.artifactName ?? path.basename(f.filePath));
    if (affected.length === 0) return [];
    return [{
        severity: 'breaking',
        category: 'data-mapper-saxon',
        message: `Data Mapper configuration(s) detected in artifact(s): ${affected.join(', ')}. Starting from MI 4.2.0, the Saxon version has been upgraded, requiring the reserved namespaces in the default XSLT stylesheet to be updated. Use the Data Mapper Configuration Migration Tool provided by WSO2 Support to migrate them.`,
    }];
}

function localTransportRule(files: SynapseFile[]): MigrationFinding[] {
    const LOCAL_TRANSPORT_PATTERN = /uri\s*=\s*["'](local:\/\/[^"']+)["']/gi;
    const affected: string[] = [];
    for (const { content, artifactName, filePath } of files) {
        LOCAL_TRANSPORT_PATTERN.lastIndex = 0;
        if (LOCAL_TRANSPORT_PATTERN.test(content)) {
            affected.push(artifactName ?? path.basename(filePath));
        }
    }
    if (affected.length === 0) return [];
    return [{
        severity: 'breaking',
        category: 'local-transport',
        message: `Local transport URIs (local://...) are not supported in MI. Found in artifact(s): ${affected.join(', ')}. Replace each local:// URI with an HTTP/HTTPS endpoint pointing to localhost (e.g. http://localhost:8290/services/...).`,
    }];
}

/**
 * Walks synapse-config XML files across all ESB projects once, reads each
 * file a single time, then passes the full set to every aggregate rule.
 */
function runSynapseFileRules(ctx: AnalysisContext, rules: SynapseAggregateRule[]): MigrationFinding[] {
    const allFiles: SynapseFile[] = [];

    for (const { projectDir, projectType } of ctx.projectDirsWithType) {
        if (projectType !== Nature.ESB && projectType !== Nature.MI_ESB) continue;
        const synapseConfigDir = projectType === Nature.MI_ESB
            ? path.join(projectDir, 'src', 'main', 'wso2mi', 'artifacts')
            : path.join(projectDir, 'src', 'main', 'synapse-config');
        if (!fs.existsSync(synapseConfigDir)) continue;
        for (const filePath of walkXmlFiles(synapseConfigDir)) {
            try {
                const content = fs.readFileSync(filePath, 'utf-8');
                allFiles.push({ filePath, content, artifactName: extractArtifactName(content) });
            } catch {
                // skip unreadable files
            }
        }
    }

    return rules.flatMap(rule => rule(allFiles));
}

/**
 * Runs all analysis rules against the project context and returns a flat list
 * of findings ordered by severity (breaking first).
 */
/**
 * Returns true when the project's source runtime version is strictly before
 * the given threshold (e.g. "4.2.0"). Rules that represent changes introduced
 * at `threshold` should only fire when this returns true.
 * If `runtimeVersion` is unknown, the rule is included (safe default).
 */
function isBeforeVersion(runtimeVersion: string | undefined, threshold: string): boolean {
    if (!runtimeVersion) return true;
    const parse = (v: string) => v.split('.').map(n => parseInt(n, 10) || 0);
    const [ra, rb, rc] = parse(runtimeVersion);
    const [ta, tb, tc] = parse(threshold);
    if (ra !== ta) return ra < ta;
    if (rb !== tb) return rb < tb;
    return rc < tc;
}

export function analyzeProject(ctx: AnalysisContext): MigrationFinding[] {
    const runtimeVersion = ctx.runtimeVersion ?? getProjectDetails(ctx.source).runtimeVersion;
    const targetVersion = ctx.targetVersion;

    // Returns true when this migration crosses `threshold` — i.e. the source is
    // before the threshold and the target is at or after it.
    const crossesBoundary = (threshold: string) =>
        isBeforeVersion(runtimeVersion, threshold) && !isBeforeVersion(targetVersion, threshold);

    const projectRules: AnalysisRule[] = [
        classMediatorRule,
        connectorVersionRule,
        unknownArtifactTypeRule,
        missingDependencyRule,
        unrecognizedProjectTypeRule,
        skippedArtifactsRule,
    ];

    const synapseRules: SynapseAggregateRule[] = [
        scriptMediatorNashornRule,
        // language="js" routed to GraalVM instead of Rhino from MI 4.3.0
        ...(crossesBoundary('4.3.0') ? [scriptMediatorGraalVmRule] : []),
        localTransportRule,
        localTransportListenerProxyRule,
        // Saxon upgrade introduced in MI 4.2.0
        ...(crossesBoundary('4.2.0') ? [dataMapperSaxonRule] : []),
    ];

    const all = [
        ...projectRules.flatMap(rule => rule(ctx)),
        ...runSynapseFileRules(ctx, synapseRules),
    ];

    // Sort: breaking first, then warnings
    return all.sort((a, b) => {
        if (a.severity === b.severity) return 0;
        return a.severity === 'breaking' ? -1 : 1;
    });
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function walkXmlFiles(dir: string): string[] {
    const results: string[] = [];
    try {
        for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
            const fullPath = path.join(dir, entry.name);
            if (entry.isDirectory()) {
                results.push(...walkXmlFiles(fullPath));
            } else if (entry.name.endsWith('.xml')) {
                results.push(fullPath);
            }
        }
    } catch {
        // skip unreadable
    }
    return results;
}

/**
 * Returns all `language` attribute values from every `<script>` tag in the
 * XML content. A file may contain multiple script mediators.
 */
function extractScriptLanguages(xmlContent: string): string[] {
    const results: string[] = [];
    const pattern = /<script\b[^>]*\blanguage\s*=\s*["']([^"']+)["']/gi;
    let m: RegExpExecArray | null;
    while ((m = pattern.exec(xmlContent)) !== null) {
        results.push(m[1]);
    }
    return results;
}

/**
 * Extracts the artifact name from a synapse config XML file.
 * Most artifacts expose a `name` attribute on the root element;
 * local entries use `key` instead.
 */
function extractArtifactName(xmlContent: string): string | null {
    const nameMatch = xmlContent.match(/<(?:api|sequence|proxy|endpoint|inboundEndpoint|messageStore|messageProcessor|task|template)\b[^>]*\bname\s*=\s*["']([^"']+)["']/i);
    if (nameMatch) return nameMatch[1];
    const keyMatch = xmlContent.match(/<localEntry\b[^>]*\bkey\s*=\s*["']([^"']+)["']/i);
    return keyMatch ? keyMatch[1] : null;
}

/**
 * Returns true when `version` is strictly below `threshold`.
 * Compares major, minor, patch numerically.
 */
function isVersionBelow(version: string, threshold: string): boolean {
    const parse = (v: string) => v.split('.').map(s => parseInt(s, 10) || 0);
    const [maj, min, pat] = parse(version);
    const [tMaj, tMin, tPat] = parse(threshold);
    if (maj !== tMaj) return maj < tMaj;
    if (min !== tMin) return min < tMin;
    return pat < tPat;
}
