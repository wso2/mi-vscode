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
import * as path from 'path';
import { tool } from 'ai';
import { z } from 'zod';
import { logDebug } from '../../copilot/logger';
import { SkillExecuteFn, ToolResult } from './types';
import { SkillCatalogEntry, findSkillByName, splitFrontmatter } from './skill_discovery';

/**
 * Agent Skills activation — the Claude Code-style `skill` tool plus the shared
 * `readAndFormatSkill` helper used both by the tool (model-driven activation)
 * and by the `/skill-name` user-invocation path in agent.ts.
 *
 * Progressive disclosure: the catalog (name + description) lives in a
 * `# Available Skills` reminder; this module loads the full `SKILL.md` body on
 * activation, strips frontmatter, and enumerates (but does not read) bundled
 * resources so the model can `file_read` them on demand.
 */

/** Body size cap — matches AGENTS_MD_MAX_BYTES so a huge SKILL.md can't flood context. */
export const SKILL_MD_MAX_BYTES = 30 * 1024;

const MAX_RESOURCE_ENTRIES = 50;
const MAX_RESOURCE_DEPTH = 2;

export interface FormattedSkill {
    /** The full `<skill_content>…</skill_content>` block to surface to the model. */
    content: string;
    /** True when the body was cut at SKILL_MD_MAX_BYTES. */
    truncated: boolean;
}

/**
 * Enumerate bundled resource files under a skill directory (excluding
 * `SKILL.md`). Listing only — never reads contents. Depth- and count-bounded.
 */
function listSkillResources(baseDir: string): { files: string[]; truncated: boolean } {
    const files: string[] = [];
    let truncated = false;

    const walk = (dir: string, depth: number): void => {
        if (files.length >= MAX_RESOURCE_ENTRIES) {
            truncated = true;
            return;
        }
        let entries: fs.Dirent[];
        try {
            entries = fs.readdirSync(dir, { withFileTypes: true });
        } catch {
            return;
        }
        for (const entry of entries) {
            if (files.length >= MAX_RESOURCE_ENTRIES) {
                truncated = true;
                return;
            }
            if (entry.name.startsWith('.') || entry.name === 'node_modules') {
                continue;
            }
            const full = path.join(dir, entry.name);
            if (entry.isDirectory()) {
                if (depth < MAX_RESOURCE_DEPTH) {
                    walk(full, depth + 1);
                }
            } else if (entry.isFile()) {
                const rel = path.relative(baseDir, full).split(path.sep).join('/');
                if (rel === 'SKILL.md') {
                    continue;
                }
                files.push(rel);
            }
        }
    };

    walk(baseDir, 0);
    files.sort();
    return { files, truncated };
}

/**
 * Read a skill's `SKILL.md`, strip frontmatter, substitute `$ARGUMENTS`, cap the
 * body, and wrap it in identifying tags with the skill directory and a resource
 * listing. Shared by the `skill` tool and the `/skill-name` activation path so
 * both produce identical output. Throws on read failure (caller handles it).
 */
export function readAndFormatSkill(entry: SkillCatalogEntry, args?: string): FormattedSkill {
    const raw = fs.readFileSync(entry.location, 'utf8');
    const { body } = splitFrontmatter(raw);
    let text = body.trim();

    if (args !== undefined) {
        // Claude Code-style argument substitution.
        text = text.split('$ARGUMENTS').join(args);
    }

    let truncated = false;
    const buf = Buffer.from(text, 'utf8');
    if (buf.byteLength > SKILL_MD_MAX_BYTES) {
        text = buf.subarray(0, SKILL_MD_MAX_BYTES).toString('utf8');
        truncated = true;
    }

    const lines: string[] = [`<skill_content name="${entry.name}">`, text, ''];
    lines.push(`Skill directory: ${entry.baseDir}`);
    lines.push('Relative paths in this skill are relative to the skill directory. Read bundled resources on demand with file_read (use absolute paths).');
    if (entry.allowedTools) {
        lines.push(`Allowed tools (declared by the skill; informational): ${entry.allowedTools}`);
    }
    if (truncated) {
        lines.push(`[skill body truncated at ${Math.round(SKILL_MD_MAX_BYTES / 1024)} KB — read the rest on demand with file_read on ${entry.location}]`);
    }

    const { files, truncated: resourcesTruncated } = listSkillResources(entry.baseDir);
    if (files.length > 0) {
        lines.push('', '<skill_resources>');
        for (const file of files) {
            lines.push(`  <file>${file}</file>`);
        }
        if (resourcesTruncated) {
            lines.push('  <!-- resource listing truncated -->');
        }
        lines.push('</skill_resources>');
    }
    lines.push('</skill_content>');

    return { content: lines.join('\n'), truncated };
}

/**
 * Build the `skill` tool's execute. `skills` should be the model-invocable set
 * (callers filter out `disable-model-invocation`). `activatedSkills` is a
 * per-run set used to dedup re-activations within a session.
 */
export function createSkillExecute(skills: SkillCatalogEntry[], activatedSkills: Set<string>): SkillExecuteFn {
    return async ({ skill, args }): Promise<ToolResult> => {
        const name = (skill || '').trim();
        if (!name) {
            return {
                success: false,
                message: 'Missing required parameter: skill (the exact skill name).',
                error: 'Error: Missing skill name',
            };
        }

        // `skills` is the FULL discovered set, so a user-only skill resolves here
        // (rather than looking like an unknown name) and can be reported precisely.
        const entry = findSkillByName(skills, name);
        if (!entry) {
            // Only advertise model-invocable names — user-only skills stay hidden.
            const available = skills.filter((s) => !s.disableModelInvocation).map((s) => s.name).join(', ');
            return {
                success: false,
                message: `Unknown skill '${name}'. Available skills: ${available || '(none)'}.`,
                error: 'Error: Unknown skill',
            };
        }

        const key = entry.name.toLowerCase();
        if (activatedSkills.has(key)) {
            return {
                success: true,
                message: `Skill '${entry.name}' is already active in this session — its instructions are already in your context above. Follow them; do not re-activate. Bundled files can be read with file_read against ${entry.baseDir}.`,
            };
        }

        // User-only skill (`disable-model-invocation`): it exists but you may not
        // activate it — only the user can, explicitly. Don't load it; tell the
        // model to ask the user to invoke it via `/skill-name`.
        if (entry.disableModelInvocation) {
            return {
                success: false,
                message: `Skill '${entry.name}' is user-invocable only and cannot be activated with the skill tool. If its instructions would help, ask the user to invoke it themselves by typing "/${entry.name}" at the START of their next message (optionally followed by arguments). Do not call the skill tool for it again.`,
                error: 'SKILL_USER_ONLY',
            };
        }

        try {
            const formatted = readAndFormatSkill(entry, args);
            activatedSkills.add(key);
            logDebug(`[Skills] Activated skill '${entry.name}' from ${entry.location}`);
            return { success: true, message: formatted.content };
        } catch (error) {
            return {
                success: false,
                message: `Failed to read skill '${entry.name}' at ${entry.location}: ${error instanceof Error ? error.message : String(error)}`,
                error: 'Error: Skill read failed',
            };
        }
    };
}

/**
 * Build the `skill` tool. Mirrors Claude Code's `Skill` tool: the `skill`
 * parameter is a plain string — NOT an enum of discovered names — so the tool
 * schema is constant regardless of which skills are enabled. Toggling skills
 * therefore only changes the drift-aware `# Available Skills` reminder, never
 * the cached tools prefix. The name is validated at execute time (see
 * createSkillExecute); valid names are conveyed via that reminder.
 */
export function createSkillTool(execute: SkillExecuteFn) {
    const skillInputSchema = z.object({
        skill: z.string().describe('The exact name of a skill from the "# Available Skills" reminder (no leading slash). Do not guess names.'),
        args: z.string().optional().describe('Optional arguments forwarded to the skill (substituted for $ARGUMENTS in the skill body).'),
    });

    return (tool as any)({
        description: `Execute a skill to load specialized, task-specific instructions into your context.

When the user's task matches an available skill, use this tool to invoke it. When the user references a "slash command" or "/<something>", they are referring to a skill — use this tool to invoke it.

How to invoke:
- Set \`skill\` to the exact name of an available skill (no leading slash).
- Set \`args\` to pass optional arguments (substituted for $ARGUMENTS in the skill body).

Important:
- Available skills are listed in the \`# Available Skills\` <system-reminder>. Only invoke a skill that appears there, or one the user explicitly typed as \`/<name>\`. Never guess or invent a skill name.
- When a skill matches the user's request, invoke it before producing other output about the task, then follow the loaded instructions.
- Do not re-activate a skill already active this session — its instructions are already in your context. Bundled files a skill references (scripts/references/assets) are read on demand with file_read against the skill directory.`,
        inputSchema: skillInputSchema,
        execute,
    });
}
