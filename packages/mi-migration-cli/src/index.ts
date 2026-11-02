#!/usr/bin/env node
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

import * as path from 'path';
import * as fs from 'fs';
import * as os from 'os';
import * as readline from 'readline';
import { Command, Option } from 'commander';
import {
    importProjects,
    generateArtifactIdToFileInfoMap,
    getProjectDirectoriesWithType,
    generateProjectDirToResolvedPomMap,
    analyzeProject,
    getProjectDetails,
    findMultiModuleProjectsInWorkspaceDir,
    copyMavenWrapper,
    MigrationHost,
    MigrationConfigKey,
    MigrationFinding,
    LATEST_MI_VERSION,
    Nature,
} from '@wso2/mi-migration-core';

// ---------------------------------------------------------------------------
// SEA asset extraction
// ---------------------------------------------------------------------------

let _seaTempDir: string | undefined;

/**
 * Extracts all embedded SEA assets (resources/) to a temp directory on first
 * call, then returns that directory path on subsequent calls.
 * The temp directory is removed when the process exits.
 */
function extractSeaAssets(): string {
    if (_seaTempDir) return _seaTempDir;
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const sea = require('node:sea') as {
        getRawAsset(key: string): ArrayBuffer;
        getAsset(key: string, encoding: 'utf8'): string;
    };
    const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'mi-migrate-'));
    process.on('exit', () => {
        try { fs.rmSync(tempDir, { recursive: true, force: true }); } catch { /* best-effort */ }
    });
    const manifest: string[] = JSON.parse(sea.getAsset('__asset_manifest__', 'utf8'));
    for (const key of manifest) {
        const dest = path.join(tempDir, key);
        fs.mkdirSync(path.dirname(dest), { recursive: true });
        fs.writeFileSync(dest, Buffer.from(sea.getRawAsset(key)));
        if (path.basename(key) === 'mvnw') fs.chmodSync(dest, 0o755);
    }
    _seaTempDir = tempDir;
    return tempDir;
}

// ---------------------------------------------------------------------------
// CLI MigrationHost implementation
// ---------------------------------------------------------------------------

interface CliOptions {
    yes: boolean;
    failOnBreaking: boolean;
    targetVersion: string;
    javaHome?: string;
    logFile?: string;
    format: 'text' | 'json';
    output?: string;
}

function setupFileLogging(logFile: string) {
    const append = (...args: unknown[]) => {
        try {
            const line = `[${new Date().toISOString()}] ${args.map(a => String(a)).join(' ')}`;
            fs.appendFileSync(logFile, line + '\n', 'utf-8');
        } catch { /* best-effort */ }
    };
    const _log = console.log.bind(console);
    const _warn = console.warn.bind(console);
    const _error = console.error.bind(console);
    console.log = (...args) => { _log(...args); append(...args); };
    console.warn = (...args) => { _warn(...args); append(...args); };
    console.error = (...args) => { _error(...args); append(...args); };
}

function createHost(opts: CliOptions): MigrationHost {
    if (opts.format === 'json') {
        console.log = (...args: unknown[]) => process.stderr.write(args.map(String).join(' ') + '\n');
    }
    if (opts.logFile) {
        setupFileLogging(opts.logFile);
    }

    return {
        notify(level, message, _modal?: boolean) {
            const prefix = level === 'error' ? '[ERROR]' : level === 'warn' ? '[WARN]' : '[INFO]';
            const line = `${prefix} ${message}`;
            if (level === 'error') {
                console.error(line);
            } else if (level === 'warn') {
                console.warn(line);
            } else {
                (opts.format === 'json' ? process.stderr : process.stdout).write(line + '\n');
            }
            return Promise.resolve();
        },

        getResourcePath(relative) {
            const envOverride = process.env['MI_MIGRATE_RESOURCES_DIR'];
            if (envOverride) return path.join(envOverride, relative);

            try {
                // eslint-disable-next-line @typescript-eslint/no-require-imports
                const sea = require('node:sea') as { isSea?: () => boolean };
                if (typeof sea.isSea === 'function' && sea.isSea()) {
                    return path.join(extractSeaAssets(), relative);
                }
            } catch { /* not running as SEA */ }

            // Non-SEA: walk up from the script directory to find a resources/ folder
            const mainFile = require.main?.filename ?? process.argv[1];
            const scriptDir = path.dirname(path.resolve(mainFile));
            let dir = scriptDir;
            for (let i = 0; i < 5; i++) {
                const candidate = path.join(dir, 'resources');
                if (fs.existsSync(candidate)) return path.join(candidate, relative);
                dir = path.dirname(dir);
            }

            throw new Error(
                `Cannot locate resources directory (searched up from ${scriptDir}).\n` +
                `Set MI_MIGRATE_RESOURCES_DIR=/path/to/resources to override.`
            );
        },

        getConfig(key: MigrationConfigKey): boolean {
            switch (key) {
                case 'useLocalMaven': return true;
                case 'useDefaultMavenForMigration': return true;
                case 'convertToConsolidated': return false;
            }
        },

        getJavaHome(): string | undefined {
            return opts.javaHome;
        },

        getTargetVersion(_sourceVersion?: string): string {
            return opts.targetVersion;
        },

        async confirmProceed(message): Promise<boolean> {
            if (opts.yes) return true;

            process.stdout.write(`\n${message}\n\nProceed? [y/N] `);
            return new Promise((resolve) => {
                const rl = readline.createInterface({
                    input: process.stdin,
                    output: process.stdout,
                    terminal: false,
                });
                rl.once('line', (answer) => {
                    resolve(answer.trim().toLowerCase() === 'y');
                    rl.close();
                });
                rl.once('close', () => resolve(false));
            });
        },
    };
}

// ---------------------------------------------------------------------------
// Project / workspace detection
// ---------------------------------------------------------------------------

/**
 * Returns the list of IS project directories rooted at `absSource`.
 * If `absSource` is itself an IS project, returns `[absSource]`.
 * Otherwise treats it as a workspace and scans for sub-projects.
 * Falls back to `[absSource]` if nothing is found (unknown structure).
 */
async function resolveProjectDirs(absSource: string): Promise<string[]> {
    // Single IS project — has a valid pom.xml with project metadata
    const { projectName } = getProjectDetails(absSource);
    if (projectName) return [absSource];

    // VS Code MI project root: contains a sub-module with src/main/wso2mi/
    // (the root may have no pom.xml of its own; analyze it as a single unit)
    try {
        for (const entry of fs.readdirSync(absSource, { withFileTypes: true })) {
            if (!entry.isDirectory()) continue;
            if (fs.existsSync(path.join(absSource, entry.name, 'src', 'main', 'wso2mi'))) {
                return [absSource];
            }
        }
    } catch { /* unreadable — fall through */ }

    // Workspace mode: findMultiModuleProjectsInWorkspaceDir stops recursing when it
    // encounters any .project file (even the Eclipse workspace-root one). Fall back to
    // scanning immediate child directories for IS projects when it returns nothing.
    const found = await findMultiModuleProjectsInWorkspaceDir(absSource);
    if (found.length > 0) return found;

    const children: string[] = [];
    try {
        for (const entry of fs.readdirSync(absSource, { withFileTypes: true })) {
            if (!entry.isDirectory()) continue;
            const childDir = path.join(absSource, entry.name);
            const { projectName: childName } = getProjectDetails(childDir);
            if (childName) children.push(childDir);
        }
    } catch { /* unreadable — fall through */ }

    return children.length > 0 ? children : [absSource];
}

// ---------------------------------------------------------------------------
// Report helpers (extended for workspace mode)
// ---------------------------------------------------------------------------

interface TaggedFinding extends MigrationFinding {
    projectLabel?: string;
}

function printTextReport(findings: TaggedFinding[]) {
    if (findings.length === 0) {
        console.log('\nNo issues found.');
        return;
    }
    const breaking = findings.filter(f => f.severity === 'breaking');
    const warnings = findings.filter(f => f.severity === 'warning');
    console.log(`\nPre-migration analysis: ${breaking.length} breaking, ${warnings.length} warning(s)\n`);

    const fmt = (f: TaggedFinding) => {
        const prefix = f.projectLabel ? `[${f.projectLabel}] ` : '';
        console.log(`  [${f.category}] ${prefix}${f.message}`);
        if (f.filePath) console.log(`    → ${f.filePath}`);
    };
    if (breaking.length > 0) {
        console.log('== Breaking Changes ==');
        breaking.forEach(fmt);
    }
    if (warnings.length > 0) {
        if (breaking.length > 0) console.log('');
        console.log('== Warnings ==');
        warnings.forEach(fmt);
    }
}

function printJsonReport(findings: TaggedFinding[]) {
    process.stdout.write(JSON.stringify({ findings }, null, 2) + '\n');
}

const BACKUP_DIR = '.backup';
const MIGRATION_LOG_FILE = 'migration.log';

/**
 * Appends a timestamped line to a project's .backup/migration.log in the same
 * format the migration core uses, so post-migration CLI actions are captured there.
 */
function appendToProjectMigrationLog(projectDir: string, level: 'INFO' | 'WARN' | 'ERROR', message: string) {
    try {
        const logPath = path.join(projectDir, BACKUP_DIR, MIGRATION_LOG_FILE);
        if (!fs.existsSync(path.dirname(logPath))) return;
        const line = `[${new Date().toISOString()}] [${level}] ${message}\n`;
        fs.appendFileSync(logPath, line, 'utf-8');
    } catch { /* best-effort */ }
}

function writeAnalysisSummary(
    projectDir: string,
    projectName: string,
    findings: MigrationFinding[],
    targetVersion: string,
): string {
    const breaking = findings.filter(f => f.severity === 'breaking');
    const warnings  = findings.filter(f => f.severity === 'warning');
    const now = new Date().toISOString().replace('T', ' ').slice(0, 19) + ' UTC';

    const lines: string[] = [
        `# Analysis Summary — ${projectName}`,
        '',
        `**Generated:** ${now}  `,
        `**Target MI version:** ${targetVersion}  `,
        `**Project:** ${projectDir}  `,
        '',
        '---',
        '',
    ];

    const fmtSection = (title: string, items: MigrationFinding[]) => {
        lines.push(`## ${title} (${items.length})`);
        lines.push('');
        if (items.length === 0) {
            lines.push('_None_');
        } else {
            for (const f of items) {
                lines.push(`### \`[${f.category}]\``);
                lines.push('');
                lines.push(f.message);
                if (f.filePath) lines.push(`  \n_File:_ \`${f.filePath}\``);
                lines.push('');
            }
        }
    };

    fmtSection('Breaking Changes', breaking);
    lines.push('');
    fmtSection('Warnings', warnings);
    lines.push('');
    lines.push('---');
    lines.push('_Generated by mi-migrate CLI_');

    const outPath = path.join(projectDir, 'analysis-summary.md');
    fs.writeFileSync(outPath, lines.join('\n'), 'utf-8');
    return outPath;
}

// ---------------------------------------------------------------------------
// Commands
// ---------------------------------------------------------------------------

async function analyzeDir(projectDir: string, host: MigrationHost, opts: CliOptions): Promise<TaggedFinding[]> {
    const projectDirsWithType = await getProjectDirectoriesWithType(projectDir);

    // If no sub-projects were detected, check whether projectDir itself is a
    // VS Code MI configs project (src/main/wso2mi/ lives directly under it).
    if (projectDirsWithType.length === 0 &&
            fs.existsSync(path.join(projectDir, 'src', 'main', 'wso2mi'))) {
        projectDirsWithType.push({ projectDir, projectType: Nature.MI_ESB });
    }

    const projectDirToResolvedPomMap = await generateProjectDirToResolvedPomMap(projectDir, host);
    const artifactIdToFileInfoMap = generateArtifactIdToFileInfoMap(projectDirToResolvedPomMap, projectDirsWithType);
    return analyzeProject({
        source: projectDir,
        projectDirsWithType,
        artifactIdToFileInfoMap,
        projectDirToResolvedPomMap,
        targetVersion: opts.targetVersion,
    });
}

async function runAnalyze(source: string, opts: CliOptions) {
    const host = createHost(opts);
    const absSource = path.resolve(source);
    const progress = (msg: string) => (opts.format === 'json' ? process.stderr : process.stdout).write(msg + '\n');

    if (!fs.existsSync(absSource)) {
        console.error(`[ERROR] Source directory does not exist: ${absSource}`);
        process.exit(1);
    }

    const projectDirs = await resolveProjectDirs(absSource);
    const isWorkspace = projectDirs.length > 1 || projectDirs[0] !== absSource;

    if (isWorkspace) {
        progress(`Workspace: ${absSource} (${projectDirs.length} project(s) found)`);
    } else {
        const { projectName } = getProjectDetails(absSource);
        const hasMiSubmodule = (() => {
            try {
                for (const entry of fs.readdirSync(absSource, { withFileTypes: true })) {
                    if (entry.isDirectory() && fs.existsSync(path.join(absSource, entry.name, 'src', 'main', 'wso2mi')))
                        return true;
                }
            } catch { /* ignore */ }
            return false;
        })();
        if (!projectName && !hasMiSubmodule) {
            console.error('[ERROR] Source does not appear to be a valid Integration project (no pom.xml with project details found).');
            process.exit(1);
        }
        progress(`Analyzing: ${absSource}`);
    }

    const allFindings: TaggedFinding[] = [];
    const summaryPaths: string[] = [];
    for (const dir of projectDirs) {
        const findings = await analyzeDir(dir, host, opts);
        const label = getProjectDetails(dir).projectName ?? path.basename(dir);
        if (isWorkspace) {
            allFindings.push(...findings.map(f => ({ ...f, projectLabel: label })));
        } else {
            allFindings.push(...findings);
        }
        try {
            const summaryPath = writeAnalysisSummary(dir, label, findings, opts.targetVersion);
            summaryPaths.push(summaryPath);
        } catch (err) {
            console.warn(`[WARN] Could not write analysis-summary.md for ${label}: ${err}`);
        }
    }

    if ((opts.format ?? 'text') === 'json') {
        printJsonReport(allFindings);
    } else {
        printTextReport(allFindings);
    }

    if (summaryPaths.length > 0) {
        progress('\nAnalysis summary written to:');
        for (const p of summaryPaths) progress(`  ${p}`);
    }

    const hasBreaking = allFindings.some(f => f.severity === 'breaking');
    if (opts.failOnBreaking && hasBreaking) {
        process.exit(2);
    }
}

function copyDirSync(src: string, dest: string) {
    fs.mkdirSync(dest, { recursive: true });
    for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
        const srcPath = path.join(src, entry.name);
        const destPath = path.join(dest, entry.name);
        if (entry.isDirectory()) {
            copyDirSync(srcPath, destPath);
        } else {
            fs.copyFileSync(srcPath, destPath);
        }
    }
}

async function runMigrate(source: string, opts: CliOptions) {
    const absSource = path.resolve(source);

    if (!fs.existsSync(absSource)) {
        console.error(`[ERROR] Source directory does not exist: ${absSource}`);
        process.exit(1);
    }

    // Resolve output directory
    const outDir = opts.output
        ? path.resolve(opts.output)
        : path.join(path.dirname(absSource), path.basename(absSource) + '-migrated');

    if (fs.existsSync(outDir)) {
        console.error(`[ERROR] Output directory already exists: ${outDir}\nDelete it or specify a different --output path.`);
        process.exit(1);
    }

    const rel = path.relative(absSource, outDir);
    const isOutside = rel === '..' || rel.startsWith('..' + path.sep);
    if (!isOutside && !path.isAbsolute(rel)) {
        console.error(`[ERROR] Output directory cannot be equal to or inside the source directory: ${outDir}`);
        process.exit(1);
    }

    // Analyze source before copying — user gets feedback without any disk writes
    const srcProjectDirs = await resolveProjectDirs(absSource);
    const isWorkspace = srcProjectDirs.length > 1 || srcProjectDirs[0] !== absSource;

    if (isWorkspace) {
        console.log(`Workspace detected: ${srcProjectDirs.length} project(s) to migrate`);
    } else {
        // resolveProjectDirs falls back to [absSource] for unknown structures;
        // warn early if the source doesn't look like a valid IS project.
        const { projectName } = getProjectDetails(absSource);
        const hasMiSubmodule = (() => {
            try {
                for (const entry of fs.readdirSync(absSource, { withFileTypes: true })) {
                    if (entry.isDirectory() && fs.existsSync(path.join(absSource, entry.name, 'src', 'main', 'wso2mi')))
                        return true;
                }
            } catch { /* ignore */ }
            return false;
        })();
        if (!projectName && !hasMiSubmodule) {
            console.error('[ERROR] Source does not appear to be a valid Integration Studio project (no pom.xml with project details found).');
            process.exit(1);
        }
    }

    const host = createHost(opts);

    // Pre-migration analysis: one combined pass, one report, one prompt
    const perProjectFindings = new Map<string, MigrationFinding[]>();
    const allFindings: TaggedFinding[] = [];
    for (const dir of srcProjectDirs) {
        const findings = await analyzeDir(dir, host, opts);
        perProjectFindings.set(dir, findings);
        if (isWorkspace) {
            const label = getProjectDetails(dir).projectName ?? path.basename(dir);
            allFindings.push(...findings.map(f => ({ ...f, projectLabel: label })));
        } else {
            allFindings.push(...findings);
        }
    }
    if (allFindings.length > 0) {
        printTextReport(allFindings);
        const breaking = allFindings.filter(f => f.severity === 'breaking');
        const question = breaking.length > 0
            ? `Found ${breaking.length} breaking change(s) and ${allFindings.length - breaking.length} warning(s).\n\nAfter migration completes, an analysis-summary.md will be written to each project directory listing any items that need attention in VS Code. Proceed with migration?`
            : `Found ${allFindings.length} warning(s).\n\nAfter migration completes, an analysis-summary.md will be written to each project directory listing any items that need attention in VS Code. Proceed with migration?`;
        const shouldProceed = await host.confirmProceed(question);
        if (!shouldProceed) {
            console.log('Migration cancelled.');
            process.exit(0);
        }
    }

    // Copy source to output only after confirmation
    console.log(`Source:  ${absSource}`);
    console.log(`Output:  ${outDir}`);
    console.log('Copying source to output directory...');
    try {
        copyDirSync(absSource, outDir);
    } catch (err) {
        try { fs.rmSync(outDir, { recursive: true, force: true }); } catch { /* best-effort */ }
        throw err;
    }

    // Map source project dirs to their counterparts in outDir
    const projectDirs = srcProjectDirs.map(dir => path.join(outDir, path.relative(absSource, dir)));

    const requests = projectDirs.map(dir => ({ source: dir, directory: dir }));
    const result = await importProjects(requests, host);

    if (result.filePaths.length > 0) {
        console.log('\nMigrated project(s):');
        result.filePaths.forEach(p => {
            console.log(`  ${p}`);
            for (const dir of projectDirs) {
                appendToProjectMigrationLog(dir, 'INFO', `Migrated project output: ${p}`);
            }
        });

        // Copy Maven wrapper into each migrated project so it is immediately usable
        for (const p of result.filePaths) {
            try {
                await copyMavenWrapper(host.getResourcePath('maven-wrapper'), p, host.getConfig('useDefaultMavenForMigration'));
                const msg = `Maven wrapper copied to: ${p}`;
                console.log(`[INFO] ${msg}`);
                for (const dir of projectDirs) {
                    appendToProjectMigrationLog(dir, 'INFO', msg);
                }
            } catch (err) {
                const msg = `Could not copy Maven wrapper to ${p}: ${err}`;
                console.warn(`[WARN] ${msg}`);
                for (const dir of projectDirs) {
                    appendToProjectMigrationLog(dir, 'WARN', msg);
                }
            }
        }
    } else {
        console.error('[ERROR] Migration produced no output.');
        try { fs.rmSync(outDir, { recursive: true, force: true }); } catch { /* best-effort */ }
        process.exit(1);
    }

    // Write per-project analysis summaries to the migrated output directories
    const summaryPaths: string[] = [];
    for (let i = 0; i < srcProjectDirs.length; i++) {
        const outDirProject = projectDirs[i];
        const findings = perProjectFindings.get(srcProjectDirs[i]) ?? [];
        const label = getProjectDetails(outDirProject).projectName ?? path.basename(outDirProject);
        try {
            const summaryPath = writeAnalysisSummary(outDirProject, label, findings, opts.targetVersion);
            summaryPaths.push(summaryPath);
            const msg = `Analysis summary written: ${summaryPath}`;
            console.log(`[INFO] ${msg}`);
            appendToProjectMigrationLog(outDirProject, 'INFO', msg);
        } catch (err) {
            const msg = `Could not write analysis-summary.md for ${label}: ${err}`;
            console.warn(`[WARN] ${msg}`);
            appendToProjectMigrationLog(outDirProject, 'WARN', msg);
        }
    }
    if (summaryPaths.length > 0) {
        console.log('\nReview the analysis summary for items to address in VS Code:');
        for (const p of summaryPaths) console.log(`  ${p}`);
    }

    if (result.failedSources.length > 0) {
        console.error(`[ERROR] ${result.failedSources.length} project(s) failed to migrate:`);
        for (const s of result.failedSources) console.error(`  ${s}`);
        process.exitCode = 1;
    }
}

// ---------------------------------------------------------------------------
// CLI definition
// ---------------------------------------------------------------------------

const program = new Command();

program
    .name('mi-migrate')
    .description('Migrate WSO2 Integration Studio projects to MI VS Code format')
    .version('1.0.0');

// Options shared by both commands
const commonOptions = [
    new Option('--target-version <version>', 'Target MI runtime version').default(LATEST_MI_VERSION),
    new Option('--java-home <path>', 'Override JAVA_HOME for Maven invocations'),
    new Option('--log-file <path>', 'Append log output to this file'),
];

function addCommonOptions(cmd: Command) {
    commonOptions.forEach(opt => cmd.addOption(opt));
    return cmd;
}

addCommonOptions(
    program
        .command('migrate <source>')
        .description('Migrate an Integration Studio project at <source>')
        .addOption(new Option('-y, --yes', 'Non-interactive: assume yes to all prompts').default(false))
        .addOption(new Option('-o, --output <path>', 'Output directory for the migrated project (default: <source>-migrated)'))
).action(async (source: string, cmdOpts: CliOptions) => {
    try {
        await runMigrate(source, cmdOpts);
    } catch (err) {
        console.error('[ERROR] Migration failed:', err instanceof Error ? err.message : err);
        process.exit(1);
    }
});

addCommonOptions(
    program
        .command('analyze <source>')
        .description('Analyze an Integration Studio project for migration issues without migrating')
        .addOption(new Option('--fail-on-breaking', 'Exit with code 2 if breaking changes are found').default(false))
        .addOption(new Option('--format <fmt>', 'Output format: text or json').choices(['text', 'json']).default('text'))
).action(async (source: string, cmdOpts: CliOptions) => {
    try {
        await runAnalyze(source, cmdOpts);
    } catch (err) {
        console.error('[ERROR] Analysis failed:', err instanceof Error ? err.message : err);
        process.exit(1);
    }
});

program.parse(process.argv);
