/**
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com/) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import * as fs from 'fs';
import * as fsp from 'fs/promises';
import * as os from 'os';
import * as path from 'path';
import { parse as parseYaml } from 'yaml';
import { logDebug, logError, logWarn } from '../../copilot/logger';
import { getGlobalSkillsDir, getGlobalSkillsStatePath, getProjectSkillsStatePath } from '../storage-paths';

export type SkillScope = 'project' | 'user';

/**
 * Agent Skills discovery — Claude Code-style.
 *
 * A skill is a directory containing a `SKILL.md` file with YAML frontmatter
 * (`name` + `description`, plus optional `disable-model-invocation` /
 * `allowed-tools`) followed by a Markdown body. We scan a fixed set of
 * project-level and user-level directories (Claude-compatible, so existing
 * `.claude/skills` skills are picked up), parse just the frontmatter for the
 * catalog, and let the activation path read the body on demand.
 *
 * See: https://agentskills.io/specification
 */

/** A single discovered skill (frontmatter only — body is read on activation). */
export interface SkillCatalogEntry {
    /** Skill name from frontmatter (falls back to the directory name). */
    name: string;
    /** One-line "what + when" description from frontmatter. */
    description: string;
    /** Absolute path to the skill's `SKILL.md`. */
    location: string;
    /** Absolute path to the skill directory (parent of `SKILL.md`). */
    baseDir: string;
    /** Whether the skill was found under a project-level or user-level scope. */
    scope: SkillScope;
    /**
     * User enable/disable state (from the scope's skills-state.json). Disabled
     * skills are excluded by `discoverSkills` (and thus from the catalog, the
     * `skill` tool, `/skill-name`, and autocomplete); they still appear in
     * `discoverManagedSkills` so the settings UI can re-enable them.
     */
    enabled: boolean;
    /**
     * When true the skill is hidden from the model-facing catalog and the
     * `skill` tool enum (it stays user-invocable via `/skill-name`).
     */
    disableModelInvocation: boolean;
    /** Raw `allowed-tools` frontmatter value (informational; not yet enforced). */
    allowedTools?: string;
    /** `SKILL.md` mtime — part of the drift hash so edits trigger re-injection. */
    mtimeMs: number;
    /** `SKILL.md` byte size — part of the drift hash. */
    size: number;
}

/** A discovered skill plus management metadata, for the settings UI. */
export interface ManagedSkillEntry extends SkillCatalogEntry {
    /** True when a higher-precedence skill of the same name shadows this one. */
    shadowed: boolean;
}

export interface DiscoverSkillsOptions {
    /**
     * Whether to scan project-level skill directories. Callers pass
     * `vscode.workspace.isTrusted` so an untrusted cloned repo can't inject
     * skill instructions (skills can ship executable `scripts/`).
     */
    includeProjectScope: boolean;
}

/** Defensive bounds so a pathological tree can't blow up discovery. */
const MAX_SKILLS = 200;
const SKILLS_SUBDIR = path.join('skills');

/**
 * A skill identifier that can be invoked via `/skill-name`. Must stay in sync
 * with the slash parser in `agents/main/agent.ts` (`detectSlashSkillInvocation`)
 * so every discovered skill is actually invocable, and is safe to embed in the
 * `<skill_content name="…">` wrapper (no quotes/angle brackets/spaces).
 */
const SLASH_SAFE_SKILL_NAME = /^[A-Za-z0-9][A-Za-z0-9_-]*$/;

/**
 * Ordered list of skill roots. Order defines collision precedence: the first
 * occurrence of a given name wins, later duplicates are shadowed. Project
 * scopes come before user scopes (project overrides user, per the spec).
 */
function getSkillRoots(projectPath: string, includeProjectScope: boolean): Array<{ dir: string; scope: SkillScope }> {
    const home = os.homedir();
    const roots: Array<{ dir: string; scope: SkillScope }> = [];
    if (includeProjectScope) {
        roots.push({ dir: path.join(projectPath, '.agents', SKILLS_SUBDIR), scope: 'project' });
        roots.push({ dir: path.join(projectPath, '.claude', SKILLS_SUBDIR), scope: 'project' });
    }
    roots.push({ dir: path.join(home, '.agents', SKILLS_SUBDIR), scope: 'user' });
    roots.push({ dir: path.join(home, '.claude', SKILLS_SUBDIR), scope: 'user' });
    roots.push({ dir: getGlobalSkillsDir(), scope: 'user' });
    return roots;
}

/**
 * Split leading YAML frontmatter (between `---` fences) from the Markdown body.
 * Returns `undefined` for the frontmatter when the file has no fenced block.
 */
export function splitFrontmatter(raw: string): { frontmatter: string | undefined; body: string } {
    // Tolerate a leading BOM / blank lines before the opening fence.
    const match = raw.match(/^﻿?\s*---\r?\n([\s\S]*?)\r?\n---[ \t]*\r?\n?/);
    if (!match) {
        return { frontmatter: undefined, body: raw };
    }
    return { frontmatter: match[1], body: raw.slice(match[0].length) };
}

/**
 * Best-effort fixer for the most common cross-client YAML mistake: an unquoted
 * scalar containing a colon (e.g. `description: Use when: the user asks`). Wrap
 * such top-level values in double quotes before a retry parse.
 */
function quoteRiskyScalars(frontmatter: string): string {
    return frontmatter
        .split('\n')
        .map((line) => {
            const m = line.match(/^([A-Za-z0-9_-]+):[ \t]+(.+?)[ \t]*$/);
            if (!m) {
                return line;
            }
            const [, key, value] = m;
            // Leave already-quoted values, block scalars, and flow collections alone.
            if (/^["'[{>|]/.test(value)) {
                return line;
            }
            if (value.includes(': ') || /:$/.test(value) || value.includes(' #')) {
                return `${key}: "${value.replace(/"/g, '\\"')}"`;
            }
            return line;
        })
        .join('\n');
}

function parseFrontmatter(frontmatter: string): Record<string, unknown> | undefined {
    try {
        const parsed = parseYaml(frontmatter);
        return parsed && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : undefined;
    } catch {
        try {
            const parsed = parseYaml(quoteRiskyScalars(frontmatter));
            return parsed && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : undefined;
        } catch {
            return undefined;
        }
    }
}

function asString(value: unknown): string | undefined {
    if (typeof value === 'string') {
        return value;
    }
    if (typeof value === 'number' || typeof value === 'boolean') {
        return String(value);
    }
    return undefined;
}

function asBoolean(value: unknown): boolean {
    if (typeof value === 'boolean') {
        return value;
    }
    if (typeof value === 'string') {
        return value.trim().toLowerCase() === 'true';
    }
    return false;
}

/**
 * Parse one `SKILL.md`. Lenient per the spec: warn-but-load on cosmetic issues
 * (name/dir mismatch, over-length name); skip only when the description is
 * missing/empty or the frontmatter is unparseable.
 */
function readSkill(skillMdPath: string, dirName: string, scope: SkillScope): SkillCatalogEntry | undefined {
    let stat: fs.Stats;
    try {
        stat = fs.statSync(skillMdPath);
        if (!stat.isFile()) {
            return undefined;
        }
    } catch {
        return undefined;
    }

    let raw: string;
    try {
        raw = fs.readFileSync(skillMdPath, 'utf8');
    } catch (error) {
        logDebug(`[Skills] Failed to read ${skillMdPath}: ${error instanceof Error ? error.message : String(error)}`);
        return undefined;
    }

    const { frontmatter } = splitFrontmatter(raw);
    if (frontmatter === undefined) {
        logWarn(`[Skills] Skipping ${skillMdPath}: no YAML frontmatter found.`);
        return undefined;
    }

    const meta = parseFrontmatter(frontmatter);
    if (!meta) {
        logWarn(`[Skills] Skipping ${skillMdPath}: frontmatter is not valid YAML.`);
        return undefined;
    }

    const description = asString(meta['description'])?.trim();
    if (!description) {
        logWarn(`[Skills] Skipping ${skillMdPath}: missing required 'description'.`);
        return undefined;
    }

    let name = asString(meta['name'])?.trim();
    if (!name) {
        // Lenient: fall back to the directory name when 'name' is absent.
        name = dirName;
        logWarn(`[Skills] ${skillMdPath}: missing 'name', falling back to directory name '${dirName}'.`);
    } else if (name !== dirName) {
        logWarn(`[Skills] ${skillMdPath}: name '${name}' does not match directory '${dirName}' (loading anyway).`);
    }
    if (name.length > 64) {
        logWarn(`[Skills] ${skillMdPath}: name exceeds 64 characters (loading anyway).`);
    }
    // The name must be invocable via `/skill-name` and safe to embed in the
    // `<skill_content name="…">` wrapper. If the frontmatter name isn't
    // slash-safe, fall back to the directory name; skip the skill entirely when
    // neither is a valid identifier.
    if (!SLASH_SAFE_SKILL_NAME.test(name)) {
        if (name !== dirName && SLASH_SAFE_SKILL_NAME.test(dirName)) {
            logWarn(`[Skills] ${skillMdPath}: name '${name}' is not a valid skill identifier ([A-Za-z0-9_-]); using directory name '${dirName}'.`);
            name = dirName;
        } else {
            logWarn(`[Skills] Skipping ${skillMdPath}: skill name '${name}' is not a valid identifier ([A-Za-z0-9_-], starting alphanumeric) and cannot be invoked via /skill-name.`);
            return undefined;
        }
    }

    return {
        name,
        description,
        location: skillMdPath,
        baseDir: path.dirname(skillMdPath),
        scope,
        enabled: true, // overridden by scanAllSkills from the scope's disabled set
        disableModelInvocation: asBoolean(meta['disable-model-invocation']),
        allowedTools: asString(meta['allowed-tools'])?.trim() || undefined,
        mtimeMs: stat.mtimeMs,
        size: stat.size,
    };
}

// ============================================================================
// Enable/disable state (per-scope skills-state.json)
// ============================================================================

/**
 * Default enabled state per scope. Project skills are committed/local and
 * intentional, so they're ON unless explicitly disabled. Global (user-scope)
 * skills — which include skills picked up from `~/.claude/skills` — are OFF by
 * default so they don't silently apply to every project; the user opts each one
 * in via Settings.
 */
const DEFAULT_ENABLED_BY_SCOPE: Record<SkillScope, boolean> = {
    project: true,
    user: false,
};

function getSkillsStatePath(scope: SkillScope, projectPath: string): string {
    return scope === 'project' ? getProjectSkillsStatePath(projectPath) : getGlobalSkillsStatePath();
}

/**
 * Parse a skills-state file's contents into an explicit name→enabled map
 * (lowercased keys). Skills absent from the map fall back to the scope default.
 * Accepts the current `{ states: { name: bool } }` shape and the legacy
 * `{ disabled: string[] }` denylist (mapped to explicit `false`).
 */
function parseSkillStates(parsed: unknown): Record<string, boolean> {
    const states: Record<string, boolean> = {};
    const obj = parsed as { states?: unknown; disabled?: unknown } | null;
    if (obj?.states && typeof obj.states === 'object') {
        for (const [k, v] of Object.entries(obj.states as Record<string, unknown>)) {
            if (typeof v === 'boolean') {
                states[k.toLowerCase()] = v;
            }
        }
    } else if (Array.isArray(obj?.disabled)) {
        for (const n of obj.disabled as unknown[]) {
            if (typeof n === 'string') {
                states[n.toLowerCase()] = false;
            }
        }
    }
    return states;
}

/** Sync read of a scope's explicit enable/disable overrides. Used by discovery. */
function readSkillStates(scope: SkillScope, projectPath: string): Record<string, boolean> {
    try {
        const raw = fs.readFileSync(getSkillsStatePath(scope, projectPath), 'utf8');
        return parseSkillStates(JSON.parse(raw));
    } catch (error) {
        if ((error as NodeJS.ErrnoException).code !== 'ENOENT') {
            logDebug(`[Skills] Failed to read skills-state (${scope}): ${error instanceof Error ? error.message : String(error)}`);
        }
        return {};
    }
}

/** Resolve a skill's effective enabled state from the scope default + overrides. */
function resolveEnabled(scope: SkillScope, name: string, states: Record<string, boolean>): boolean {
    const explicit = states[name.toLowerCase()];
    return explicit !== undefined ? explicit : DEFAULT_ENABLED_BY_SCOPE[scope];
}

// Serializes read-modify-write cycles per state file so overlapping enable/
// disable actions can't clobber each other (both starting from the same
// snapshot, the later write silently dropping the earlier change).
const skillStateWriteChains = new Map<string, Promise<void>>();

/** Async, per-file-serialized read-modify-write of a scope's overrides file. */
async function mutateSkillStates(
    projectPath: string,
    scope: SkillScope,
    mutate: (states: Record<string, boolean>) => void,
): Promise<void> {
    const file = getSkillsStatePath(scope, projectPath);
    // Chain onto any in-flight write to the same file. `.catch` so a prior
    // failure doesn't wedge every subsequent update.
    const run = (skillStateWriteChains.get(file) ?? Promise.resolve())
        .catch(() => { /* prior failure is logged by its own caller */ })
        .then(async () => {
            let states: Record<string, boolean> = {};
            try {
                states = parseSkillStates(JSON.parse(await fsp.readFile(file, 'utf8')));
            } catch (error) {
                if ((error as NodeJS.ErrnoException).code !== 'ENOENT') {
                    logError('[Skills] Failed to read skills-state for update', error);
                }
            }
            mutate(states);
            await fsp.mkdir(path.dirname(file), { recursive: true });
            await fsp.writeFile(
                file,
                JSON.stringify({ states, updatedAt: new Date().toISOString() }, null, 2),
                'utf8',
            );
        });
    skillStateWriteChains.set(file, run);
    try {
        await run;
    } finally {
        // Drop the chain entry once quiescent so the map doesn't grow unbounded.
        if (skillStateWriteChains.get(file) === run) {
            skillStateWriteChains.delete(file);
        }
    }
}

/**
 * Persist an explicit enable/disable override for a skill. The skill's home
 * scope decides the file: `user` → global, `project` → per-project (per-user,
 * uncommitted).
 */
export async function setSkillEnabledState(
    projectPath: string,
    scope: SkillScope,
    name: string,
    enabled: boolean,
): Promise<void> {
    await mutateSkillStates(projectPath, scope, (states) => {
        states[name.trim().toLowerCase()] = enabled;
    });
}

/** Remove any explicit override so the skill reverts to its scope default. */
export async function clearSkillState(projectPath: string, scope: SkillScope, name: string): Promise<void> {
    await mutateSkillStates(projectPath, scope, (states) => {
        delete states[name.trim().toLowerCase()];
    });
}

/**
 * Scan every skill root for `projectPath` and return the raw entries in
 * precedence order (project before user; no name dedup), each stamped with its
 * `enabled` state from the scope's disabled set. Never throws.
 */
function scanAllSkills(projectPath: string, opts: DiscoverSkillsOptions): SkillCatalogEntry[] {
    const statesByScope: Record<SkillScope, Record<string, boolean>> = {
        project: readSkillStates('project', projectPath),
        user: readSkillStates('user', projectPath),
    };
    const all: SkillCatalogEntry[] = [];

    for (const { dir, scope } of getSkillRoots(projectPath, opts.includeProjectScope)) {
        if (all.length >= MAX_SKILLS) {
            break;
        }
        let entries: fs.Dirent[];
        try {
            if (!fs.existsSync(dir)) {
                continue;
            }
            entries = fs.readdirSync(dir, { withFileTypes: true });
        } catch (error) {
            logDebug(`[Skills] Failed to scan ${dir}: ${error instanceof Error ? error.message : String(error)}`);
            continue;
        }

        for (const entry of entries) {
            if (!entry.isDirectory() || entry.name.startsWith('.') || entry.name === 'node_modules') {
                continue;
            }
            const skillMdPath = path.join(dir, entry.name, 'SKILL.md');
            const skill = readSkill(skillMdPath, entry.name, scope);
            if (!skill) {
                continue;
            }
            skill.enabled = resolveEnabled(scope, skill.name, statesByScope[scope]);
            all.push(skill);
            if (all.length >= MAX_SKILLS) {
                logWarn(`[Skills] Reached the ${MAX_SKILLS}-skill cap; further skills ignored.`);
                break;
            }
        }
    }

    return all;
}

/**
 * Discover the skills the agent should see for `projectPath`: enabled only,
 * name-deduped (project overrides user; first-found wins), sorted by name.
 * Disabled skills drop out here, so the catalog, the `skill` tool, `/skill-name`,
 * and the autocomplete all exclude them. Never throws.
 */
export function discoverSkills(projectPath: string, opts: DiscoverSkillsOptions): SkillCatalogEntry[] {
    const byName = new Map<string, SkillCatalogEntry>();
    for (const skill of scanAllSkills(projectPath, opts)) {
        if (!skill.enabled) {
            continue;
        }
        const key = skill.name.toLowerCase();
        const existing = byName.get(key);
        if (existing) {
            logWarn(`[Skills] '${skill.name}' at ${skill.location} is shadowed by ${existing.location} (higher precedence).`);
            continue;
        }
        byName.set(key, skill);
    }
    return Array.from(byName.values()).sort((a, b) => a.name.localeCompare(b.name));
}

/**
 * List ALL discovered skills (enabled and disabled, including shadowed
 * duplicates) for the settings UI. Each entry carries its `enabled` state and a
 * `shadowed` flag (a higher-precedence skill of the same name exists). Sorted
 * by name. Never throws.
 */
export function discoverManagedSkills(projectPath: string, opts: DiscoverSkillsOptions): ManagedSkillEntry[] {
    const seen = new Set<string>();
    const result: ManagedSkillEntry[] = [];
    for (const skill of scanAllSkills(projectPath, opts)) {
        const key = skill.name.toLowerCase();
        const shadowed = seen.has(key);
        seen.add(key);
        result.push({ ...skill, shadowed });
    }
    return result.sort((a, b) => a.name.localeCompare(b.name));
}

/** Convenience: case-insensitive lookup by name within a discovered list. */
export function findSkillByName(skills: SkillCatalogEntry[], name: string): SkillCatalogEntry | undefined {
    const target = name.trim().toLowerCase();
    return skills.find((s) => s.name.toLowerCase() === target);
}
