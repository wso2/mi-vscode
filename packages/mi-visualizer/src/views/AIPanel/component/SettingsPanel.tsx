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

import React, { useEffect, useLayoutEffect, useRef, useState } from "react";
import { Codicon } from "@wso2/ui-toolkit";
import { useMICopilotContext } from "./MICopilotContext";
import type { MainModelPreset, SubModelPreset, ManagedSkillItem } from "@wso2/mi-rpc-client/src/rpc-clients/agent-mode/rpc-client";

interface SettingsPanelProps {
    onClose: () => void;
    /**
     * True when the user pays per-request — own Anthropic key OR AWS Bedrock.
     * Used for cost-vs-quota copy in the high-intelligence warning and sign-out blurb.
     */
    isByok: boolean;
    /**
     * True once the BYOK / Bedrock check has completed. Until then `isByok` is
     * still at its default, so model-switch locking is deferred to avoid briefly
     * showing BYOK users the WSO2 lock state.
     */
    byokResolved: boolean;
    /**
     * True only for AWS Bedrock auth. Gates the Tavily / web-search controls
     * since Bedrock has no first-party web tools.
     */
    isAwsBedrock: boolean;
}

const TAVILY_SIGNUP_URL = 'https://app.tavily.com';
// AWS-procurement path for Tavily: subscribing via Marketplace bills through the
// customer's AWS account but issues the same Tavily API key — drop-in compatible
// with the standard BYOK flow. The Marketplace MCP-container listing is a
// different SKU (not a REST endpoint) and isn't linked here.
const TAVILY_AWS_MARKETPLACE_URL = 'https://aws.amazon.com/marketplace/pp/prodview-myijjwd7qoky4';

const MAIN_AGENT_OPTIONS: { value: MainModelPreset; label: string; model: string; description: string }[] = [
    { value: "sonnet", label: "Normal", model: "Claude Sonnet 4.6", description: "Balanced quality, speed, and quota usage for everyday requests." },
    { value: "opus", label: "High", model: "Claude Opus 4.8", description: "Maximum reasoning capability for complex tasks. Higher quota usage." },
];

const SUB_AGENT_OPTIONS: { value: SubModelPreset; label: string; model: string; description: string }[] = [
    { value: "haiku", label: "Normal", model: "Claude Haiku 4.5", description: "Fast and lightweight for routine sub-agent work." },
    { value: "sonnet", label: "High", model: "Claude Sonnet 4.6", description: "Higher quality sub-agent responses. Moderate quota usage." },
];

const DEFAULT_MAIN: MainModelPreset = "sonnet";
const DEFAULT_SUB: SubModelPreset = "haiku";

/**
 * Settings navigation adapts to the panel width:
 *  - Wide (e.g. Copilot docked as an editor tab): a persistent left nav rail +
 *    content pane, like the Claude desktop app.
 *  - Narrow (docked in the VS Code sidebar, where a rail would be cramped): a
 *    drill-down — the root is a category list; selecting one pushes a full-width
 *    sub-page, and the back arrow walks the stack (sub-page → root → chat).
 */
type SettingsView = "root" | "models" | "skills" | "web" | "account";

/**
 * Container width (px) at or above which we show the nav-rail layout. Below it,
 * the rail + content can't both stay usable, so we fall back to drill-down.
 */
const WIDE_LAYOUT_BREAKPOINT_PX = 560;

const VIEW_TITLES: Record<SettingsView, string> = {
    root: "Settings",
    models: "Models & Thinking",
    skills: "Skills",
    web: "Web Search",
    account: "Account",
};

const SettingsPanel: React.FC<SettingsPanelProps> = ({ onClose, isByok, byokResolved, isAwsBedrock }) => {
    const {
        rpcClient,
        modelSettings,
        updateModelSettings,
        isThinkingEnabled,
        setIsThinkingEnabled,
    } = useMICopilotContext();

    // Drill-down stack is one level deep: 'root' or a single category page.
    const [view, setView] = useState<SettingsView>("root");
    const isRoot = view === "root";
    const handleBack = () => (isRoot ? onClose() : setView("root"));

    // Track the panel width to switch between the rail and drill-down layouts.
    // useLayoutEffect measures before paint so the first frame already picks the
    // right layout (no flash of the wrong one).
    const containerRef = useRef<HTMLDivElement>(null);
    const [isWide, setIsWide] = useState(false);
    useLayoutEffect(() => {
        const el = containerRef.current;
        if (!el) { return; }
        const measure = (width: number) => setIsWide(width >= WIDE_LAYOUT_BREAKPOINT_PX);
        measure(el.getBoundingClientRect().width);
        const ro = new ResizeObserver((entries) => {
            for (const entry of entries) { measure(entry.contentRect.width); }
        });
        ro.observe(el);
        return () => ro.disconnect();
    }, []);
    // In the rail layout the nav is always visible, so there is no 'root' page —
    // it collapses onto the first category.
    const activeCategory: Exclude<SettingsView, "root"> = view === "root" ? "models" : view;

    const handleLogout = async () => {
        await rpcClient?.getMiDiagramRpcClient().logoutFromMIAccount();
    };

    const handleResetDefaults = () => {
        updateModelSettings({
            ...modelSettings,
            // When the model controls are disabled — the locked MI Copilot plan, or
            // while plan resolution is still pending — reset must leave the presets
            // as-is, otherwise it's a hidden model-switch path. Only reset what the
            // user can actually control here (thinking, below).
            ...(modelControlsDisabled ? {} : {
                mainModelPreset: DEFAULT_MAIN,
                subModelPreset: DEFAULT_SUB,
                mainModelCustomId: undefined,
                subModelCustomId: undefined,
            }),
        });
        setIsThinkingEnabled(true);
    };

    // ----- Web Search settings -----
    // Tavily key state (Bedrock-only). Loaded once on mount; saved on demand.
    // On Bedrock, the presence of a saved Tavily key IS the "web search enabled" signal.
    const [tavilyKey, setTavilyKey] = useState<string>("");
    const [tavilyDraft, setTavilyDraft] = useState<string>("");
    const [showTavilyKey, setShowTavilyKey] = useState<boolean>(false);
    // True when the user has just toggled the enable switch ON but hasn't saved a key yet.
    const [tavilyInputOpen, setTavilyInputOpen] = useState<boolean>(false);
    const [tavilyStatus, setTavilyStatus] = useState<{ kind: 'idle' | 'saving' | 'saved' | 'error'; message?: string }>({ kind: 'idle' });
    // Serializes Tavily mutations so a save and a toggle-off can't race and leave UI/server out of sync.
    const tavilyMutationLock = useRef(false);

    useEffect(() => {
        let cancelled = false;
        if (!isAwsBedrock || !rpcClient) {
            return;
        }
        (async () => {
            try {
                const stored = await rpcClient.getMiAiPanelRpcClient().getTavilyApiKey();
                if (cancelled) {
                    return;
                }
                const value = stored ?? "";
                setTavilyKey(value);
                setTavilyDraft(value);
            } catch {
                // Failure to load is non-fatal — user can re-enter the key.
            }
        })();
        return () => {
            cancelled = true;
        };
    }, [isAwsBedrock, rpcClient]);

    /**
     * Bedrock-only: enable/disable the entire web-search capability.
     * Toggling ON without a saved key shows the Tavily input.
     * Toggling OFF clears the saved key.
     */
    const handleBedrockWebSearchToggle = async (enabled: boolean) => {
        if (!rpcClient || tavilyMutationLock.current) {
            return;
        }
        // No-op when the toggle already reflects the requested state — clicking
        // "On" while a key is saved should not re-open the input.
        const currentlyOn = !!tavilyKey || tavilyInputOpen;
        if (enabled === currentlyOn) {
            return;
        }
        if (enabled) {
            // Reveal input. If a key is already saved we keep it — the toggle simply
            // reflects the existing enabled state. If not, the user needs to enter one.
            setTavilyInputOpen(true);
            if (!tavilyKey) {
                setTavilyDraft("");
                setTavilyStatus({ kind: 'idle' });
            }
            return;
        }
        // Disabling: drop the saved key (and the approval-skip pref, which becomes meaningless).
        // If no key was ever saved (toggle was on only because the input was open), skip the RPC.
        if (!tavilyKey) {
            setTavilyInputOpen(false);
            setTavilyDraft("");
            setTavilyStatus({ kind: 'idle' });
            return;
        }
        tavilyMutationLock.current = true;
        setTavilyInputOpen(false);
        setTavilyDraft("");
        setTavilyStatus({ kind: 'saving' });
        try {
            const response = await rpcClient.getMiAiPanelRpcClient().setTavilyApiKey({ apiKey: '' });
            if (response.success) {
                setTavilyKey("");
                setTavilyStatus({ kind: 'saved', message: 'Web search disabled.' });
            } else {
                setTavilyStatus({ kind: 'error', message: response.error || 'Failed to disable web search.' });
            }
        } catch (err) {
            setTavilyStatus({ kind: 'error', message: err instanceof Error ? err.message : String(err) });
        } finally {
            tavilyMutationLock.current = false;
        }
    };

    const handleTavilySave = async () => {
        if (!rpcClient || tavilyMutationLock.current) {
            return;
        }
        const trimmed = tavilyDraft.trim();
        if (!trimmed) {
            setTavilyStatus({ kind: 'error', message: 'Enter a Tavily API key or toggle web search off to disable it.' });
            return;
        }
        tavilyMutationLock.current = true;
        setTavilyStatus({ kind: 'saving' });
        try {
            const response = await rpcClient.getMiAiPanelRpcClient().setTavilyApiKey({ apiKey: trimmed });
            if (response.success) {
                setTavilyKey(trimmed);
                setTavilyInputOpen(false);
                setTavilyStatus({ kind: 'saved', message: 'Tavily key saved.' });
            } else {
                setTavilyStatus({ kind: 'error', message: response.error || 'Failed to save Tavily key.' });
            }
        } catch (err) {
            setTavilyStatus({ kind: 'error', message: err instanceof Error ? err.message : String(err) });
        } finally {
            tavilyMutationLock.current = false;
        }
    };

    const handleEditTavilyKey = () => {
        setTavilyDraft(tavilyKey);
        setTavilyInputOpen(true);
        setTavilyStatus({ kind: 'idle' });
    };

    const handleOpenTavilySignup = () => {
        rpcClient?.getMiVisualizerRpcClient().openExternal({ uri: TAVILY_SIGNUP_URL });
    };

    const handleOpenTavilyMarketplace = () => {
        rpcClient?.getMiVisualizerRpcClient().openExternal({ uri: TAVILY_AWS_MARKETPLACE_URL });
    };

    const tavilyDirty = tavilyDraft.trim() !== tavilyKey.trim();
    // Bedrock: web search is "enabled" when a key is saved or the user is in the middle of entering one.
    const isBedrockWebSearchOn = !!tavilyKey || tavilyInputOpen;

    // ----- Skills management -----
    // null = loading; [] = none found. Loaded once on mount, re-fetched after mutations.
    const [managedSkills, setManagedSkills] = useState<ManagedSkillItem[] | null>(null);
    // Keys (`${scope}:${name}`, lowercased) of rows with an in-flight toggle/delete.
    const [busySkills, setBusySkills] = useState<Set<string>>(new Set());

    const skillKey = (s: ManagedSkillItem) => `${s.scope}:${s.name.toLowerCase()}`;
    const setSkillBusy = (key: string, busy: boolean) =>
        setBusySkills((prev) => {
            const next = new Set(prev);
            if (busy) { next.add(key); } else { next.delete(key); }
            return next;
        });

    const refreshManagedSkills = async () => {
        if (!rpcClient) { return; }
        try {
            const res = await rpcClient.getMiAgentPanelRpcClient().listManagedSkills();
            setManagedSkills(res.skills);
        } catch {
            setManagedSkills([]);
        }
    };

    useEffect(() => {
        let cancelled = false;
        if (!rpcClient) { return; }
        (async () => {
            try {
                const res = await rpcClient.getMiAgentPanelRpcClient().listManagedSkills();
                if (!cancelled) { setManagedSkills(res.skills); }
            } catch {
                if (!cancelled) { setManagedSkills([]); }
            }
        })();
        return () => { cancelled = true; };
    }, [rpcClient]);

    const handleToggleSkill = async (skill: ManagedSkillItem, enabled: boolean) => {
        if (!rpcClient) { return; }
        const key = skillKey(skill);
        // Optimistic update.
        setManagedSkills((prev) => prev?.map((s) =>
            s.scope === skill.scope && s.name === skill.name ? { ...s, enabled } : s) ?? prev);
        setSkillBusy(key, true);
        try {
            const res = await rpcClient.getMiAgentPanelRpcClient().setSkillEnabled({ name: skill.name, scope: skill.scope, enabled });
            if (!res.success) { throw new Error(res.error || 'failed'); }
        } catch {
            // Revert on failure.
            setManagedSkills((prev) => prev?.map((s) =>
                s.scope === skill.scope && s.name === skill.name ? { ...s, enabled: !enabled } : s) ?? prev);
        } finally {
            setSkillBusy(key, false);
        }
    };

    const handleDeleteSkill = async (skill: ManagedSkillItem) => {
        if (!rpcClient) { return; }
        const key = skillKey(skill);
        setSkillBusy(key, true);
        try {
            const res = await rpcClient.getMiAgentPanelRpcClient().deleteSkill({ name: skill.name, scope: skill.scope });
            if (res.success && res.deleted) {
                // Re-fetch so any previously-shadowed skill of the same name re-appears correctly.
                await refreshManagedSkills();
            }
        } catch {
            // Non-fatal; leave the row in place.
        } finally {
            setSkillBusy(key, false);
        }
    };

    // WSO2 (MI Copilot / MI_INTEL) login is the only non-BYOK method. The MI Copilot
    // proxy manages the model set (and blocks Opus), so model switching is locked here.
    // Gate on byokResolved so we don't lock (or flash the lock note) before the auth
    // method is known — isByok resolves asynchronously and starts false.
    const isMiCopilotPlan = byokResolved && !isByok;
    // While the auth method is still being resolved, keep the model controls
    // neutral — disabled, but without the WSO2 lock note — so neither plan can
    // toggle a preset (or see the high-intelligence warning) before we know which
    // applies. Once resolved, isMiCopilotPlan drives the lock as usual.
    const isPlanResolutionPending = !byokResolved;
    const modelControlsDisabled = isMiCopilotPlan || isPlanResolutionPending;

    const modelSettingsAreDefault =
        modelSettings.mainModelPreset === DEFAULT_MAIN &&
        modelSettings.subModelPreset === DEFAULT_SUB &&
        !modelSettings.mainModelCustomId &&
        !modelSettings.subModelCustomId;
    // When model controls are disabled (locked plan or pending resolution) the
    // presets aren't user-controllable, so a carried-over preset must not keep the
    // reset button active — clicking it would rewrite the (locked) presets, a
    // hidden model-switch path. There, "default" depends only on the thinking toggle.
    const isDefault = (modelControlsDisabled || modelSettingsAreDefault) && isThinkingEnabled;

    // On the WSO2 plan the main agent can't use Opus (proxy-blocked), so always show
    // the non-Opus default — even if an 'opus' preset carried over from a prior BYOK
    // session (the backend clamps the actual model to match). The sub-agent presets
    // (Haiku/Sonnet) are both allowed on the plan, so its switch is locked but still
    // reflects the model actually in use.
    const effectiveMainPreset = isMiCopilotPlan ? DEFAULT_MAIN : modelSettings.mainModelPreset;
    const currentMainOption = MAIN_AGENT_OPTIONS.find(o => o.value === effectiveMainPreset) || MAIN_AGENT_OPTIONS[0];
    const currentSubOption = SUB_AGENT_OPTIONS.find(o => o.value === modelSettings.subModelPreset) || SUB_AGENT_OPTIONS[0];

    // In the rail layout the header back arrow always returns to chat (the rail
    // handles category switching); in drill-down it walks the stack.
    const headerTitle = isWide ? "Settings" : VIEW_TITLES[view];
    const headerBackLabel = isWide || isRoot ? "Back to chat" : "Back to settings";
    const onHeaderBack = isWide ? onClose : handleBack;

    // --- Category bodies. Each is self-contained (owns its own spacing) so it can
    // render either inside a drill-down sub-page or inside the rail content pane. ---

    const renderModels = () => (
        <div className="space-y-6">
            {/* WSO2 plan: model selection is managed by the proxy and locked here. */}
            {isMiCopilotPlan && (
                <div
                    className="flex items-start gap-1.5 px-3 py-2 rounded-md"
                    style={{
                        fontSize: "11px",
                        lineHeight: 1.4,
                        color: "var(--vscode-foreground)",
                        backgroundColor: "color-mix(in srgb, var(--vscode-foreground) 6%, transparent)",
                        border: "1px solid var(--vscode-panel-border)",
                    }}
                >
                    <span className="shrink-0 mt-px" style={{ color: "var(--vscode-editorInfo-foreground, #3794ff)" }}>
                        <Codicon name="lock" />
                    </span>
                    <span>
                        Sign in with your own Anthropic API key or AWS Bedrock to change models.
                    </span>
                </div>
            )}

            {/* Main Agent Intelligence */}
            <SettingsSection title="Main Agent Intelligence">
                <ToggleGroup
                    options={MAIN_AGENT_OPTIONS.map(o => o.label)}
                    selected={currentMainOption.label}
                    disabled={modelControlsDisabled}
                    onSelect={(label) => {
                        const option = MAIN_AGENT_OPTIONS.find(o => o.label === label);
                        if (option) {
                            updateModelSettings({ ...modelSettings, mainModelPreset: option.value });
                        }
                    }}
                />
                <div className="mt-2 space-y-0.5">
                    <p className="text-[11px]" style={{ color: "var(--vscode-descriptionForeground)" }}>
                        {currentMainOption.description}
                    </p>
                    <p className="text-[11px] font-medium" style={{ color: "var(--vscode-foreground)", opacity: 0.7 }}>
                        Uses {currentMainOption.model}
                    </p>
                </div>
            </SettingsSection>

            {/* Sub-Agent Intelligence */}
            <SettingsSection title="Sub-Agent Intelligence">
                <ToggleGroup
                    options={SUB_AGENT_OPTIONS.map(o => o.label)}
                    selected={currentSubOption.label}
                    disabled={modelControlsDisabled}
                    onSelect={(label) => {
                        const option = SUB_AGENT_OPTIONS.find(o => o.label === label);
                        if (option) {
                            updateModelSettings({ ...modelSettings, subModelPreset: option.value });
                        }
                    }}
                />
                <div className="mt-2 space-y-0.5">
                    <p className="text-[11px]" style={{ color: "var(--vscode-descriptionForeground)" }}>
                        {currentSubOption.description}
                    </p>
                    <p className="text-[11px] font-medium" style={{ color: "var(--vscode-foreground)", opacity: 0.7 }}>
                        Uses {currentSubOption.model}
                    </p>
                </div>
            </SettingsSection>

            {/* High intelligence warning — not shown on the WSO2 plan (model is
                locked to the plan default) nor while plan resolution is pending. */}
            {byokResolved && !isMiCopilotPlan && (modelSettings.mainModelPreset === "opus" || modelSettings.subModelPreset === "sonnet") && (
                <InfoNote
                    icon="info"
                    variant="info"
                    text={isByok
                        ? "High intelligence can increase API cost and latency."
                        : "High intelligence uses free quota faster and may hit usage limits sooner."}
                />
            )}

            {/* Thinking Mode */}
            <SettingsSection title="Thinking Mode">
                <div className="flex items-center justify-between">
                    <span className="text-[13px]" style={{ color: "var(--vscode-foreground)" }}>
                        Adaptive Thinking
                    </span>
                    <ToggleGroup
                        options={["Off", "On"]}
                        selected={isThinkingEnabled ? "On" : "Off"}
                        onSelect={(label) => setIsThinkingEnabled(label === "On")}
                        compact
                    />
                </div>
                {isThinkingEnabled && (
                    <InfoNote
                        icon="info"
                        variant="info"
                        text="Disable if the agent overthinks or feels too slow on simple requests."
                    />
                )}
            </SettingsSection>

            {/* Reset is scoped to model & thinking settings, so it lives on this page. */}
            <div className="pt-3 flex justify-end" style={{ borderTop: "1px solid var(--vscode-panel-border)" }}>
                <button
                    className="flex items-center gap-1 text-[11px] font-medium transition-colors"
                    style={{
                        color: isDefault ? "var(--vscode-descriptionForeground)" : "var(--vscode-textLink-foreground)",
                        opacity: isDefault ? 0.5 : 1,
                        cursor: isDefault ? "default" : "pointer",
                        background: "transparent",
                        border: "none",
                    }}
                    onClick={isDefault ? undefined : handleResetDefaults}
                    disabled={isDefault}
                >
                    <Codicon name="discard" />
                    Reset to defaults
                </button>
            </div>
        </div>
    );

    const renderSkills = () =>
        managedSkills === null ? (
            <p className="text-[11px]" style={{ color: "var(--vscode-descriptionForeground)" }}>
                Loading skills…
            </p>
        ) : managedSkills.length === 0 ? (
            <InfoNote
                icon="info"
                variant="info"
                text="No skills found. Add a SKILL.md under .agents/skills or .claude/skills (project), or ~/.wso2-mi/skills (global)."
            />
        ) : (
            <div className="space-y-4">
                {(["project", "user"] as const).map((scope) => {
                    const group = managedSkills.filter((s) => s.scope === scope);
                    if (group.length === 0) {
                        return null;
                    }
                    return (
                        <div key={scope} className="space-y-1">
                            <p
                                className="text-[11px] font-medium"
                                style={{ color: "var(--vscode-foreground)", opacity: 0.7 }}
                            >
                                {scope === "project" ? "Project" : "Global"}
                            </p>
                            {scope === "user" && (
                                <p className="text-[10px]" style={{ color: "var(--vscode-descriptionForeground)" }}>
                                    Global skills are off by default — enable the ones you want to use.
                                </p>
                            )}
                            {group.map((skill, i) => (
                                <SkillRow
                                    key={`${skill.scope}:${skill.name}:${i}`}
                                    skill={skill}
                                    busy={busySkills.has(`${skill.scope}:${skill.name.toLowerCase()}`)}
                                    onToggle={(enabled) => handleToggleSkill(skill, enabled)}
                                    onDelete={() => handleDeleteSkill(skill)}
                                />
                            ))}
                        </div>
                    );
                })}
            </div>
        );

    const renderWeb = () => (
        <div className="space-y-3">
            {/* Bedrock: enable toggle (gated on Tavily key). */}
            <div className="flex items-center justify-between">
                <div className="pr-3">
                    <p className="text-[13px]" style={{ color: "var(--vscode-foreground)" }}>
                        Enable web search
                    </p>
                    <p className="text-[11px] mt-0.5" style={{ color: "var(--vscode-descriptionForeground)" }}>
                        AWS Bedrock has no built-in web tools. Provide a Tavily API key to enable web_search and web_fetch.
                    </p>
                </div>
                <ToggleGroup
                    options={["Off", "On"]}
                    selected={isBedrockWebSearchOn ? "On" : "Off"}
                    onSelect={(label) => handleBedrockWebSearchToggle(label === "On")}
                    compact
                    disabled={tavilyStatus.kind === 'saving'}
                />
            </div>

            {tavilyInputOpen && (
                <div className="space-y-2">
                    <div className="flex items-center gap-2">
                        <input
                            type={showTavilyKey ? "text" : "password"}
                            value={tavilyDraft}
                            onChange={(e) => {
                                setTavilyDraft(e.target.value);
                                if (tavilyStatus.kind !== 'idle') {
                                    setTavilyStatus({ kind: 'idle' });
                                }
                            }}
                            aria-label="Tavily API key"
                            placeholder="tvly-..."
                            className="flex-1 px-2 py-1 text-[12px] rounded-md"
                            style={{
                                backgroundColor: "var(--vscode-input-background)",
                                color: "var(--vscode-input-foreground)",
                                border: "1px solid var(--vscode-input-border)",
                                outline: "none",
                            }}
                            spellCheck={false}
                            autoComplete="off"
                            autoFocus
                        />
                        <button
                            type="button"
                            onClick={() => setShowTavilyKey((v) => !v)}
                            className="flex items-center justify-center w-7 h-7 rounded-md"
                            style={{
                                background: "transparent",
                                color: "var(--vscode-descriptionForeground)",
                                border: "1px solid transparent",
                                cursor: "pointer",
                            }}
                            aria-label={showTavilyKey ? "Hide key" : "Show key"}
                            title={showTavilyKey ? "Hide key" : "Show key"}
                        >
                            <Codicon name={showTavilyKey ? "eye-closed" : "eye"} />
                        </button>
                        <button
                            type="button"
                            onClick={handleTavilySave}
                            disabled={!tavilyDirty || tavilyStatus.kind === 'saving' || !tavilyDraft.trim()}
                            className="px-3 py-1 rounded-md text-xs font-medium transition-colors"
                            style={{
                                backgroundColor: tavilyDirty && tavilyDraft.trim() ? "var(--vscode-button-background)" : "var(--vscode-button-secondaryBackground)",
                                color: tavilyDirty && tavilyDraft.trim() ? "var(--vscode-button-foreground)" : "var(--vscode-button-secondaryForeground)",
                                border: "none",
                                cursor: tavilyDirty && tavilyDraft.trim() && tavilyStatus.kind !== 'saving' ? "pointer" : "not-allowed",
                                opacity: tavilyDirty && tavilyDraft.trim() ? 1 : 0.6,
                            }}
                        >
                            {tavilyStatus.kind === 'saving' ? 'Saving…' : 'Save'}
                        </button>
                    </div>
                </div>
            )}

            {!tavilyInputOpen && tavilyKey && (
                <div className="flex items-center justify-between">
                    <p className="text-[11px]" style={{ color: "var(--vscode-descriptionForeground)" }}>
                        Tavily key saved.
                    </p>
                    <button
                        type="button"
                        onClick={handleEditTavilyKey}
                        className="text-[11px] inline-flex items-center gap-1 transition-colors"
                        style={{
                            color: "var(--vscode-textLink-foreground)",
                            background: "transparent",
                            border: "none",
                            padding: 0,
                            cursor: "pointer",
                        }}
                    >
                        <Codicon name="edit" />
                        Edit key
                    </button>
                </div>
            )}

            {tavilyStatus.kind === 'saved' && (
                <p className="text-[11px]" style={{ color: "var(--vscode-descriptionForeground)" }}>
                    {tavilyStatus.message}
                </p>
            )}
            {tavilyStatus.kind === 'error' && (
                <p className="text-[11px]" style={{ color: "var(--vscode-errorForeground)" }}>
                    {tavilyStatus.message}
                </p>
            )}

            <button
                type="button"
                onClick={handleOpenTavilySignup}
                className="text-[11px] inline-flex items-center gap-1 transition-colors"
                style={{
                    color: "var(--vscode-textLink-foreground)",
                    background: "transparent",
                    border: "none",
                    padding: 0,
                    cursor: "pointer",
                }}
            >
                <Codicon name="link-external" />
                Get a free Tavily API key
            </button>
            <button
                type="button"
                onClick={handleOpenTavilyMarketplace}
                className="text-[11px] inline-flex items-center gap-1 transition-colors"
                style={{
                    color: "var(--vscode-textLink-foreground)",
                    background: "transparent",
                    border: "none",
                    padding: 0,
                    cursor: "pointer",
                }}
                title="Subscribe to Tavily Enterprise via AWS Marketplace and bill through your AWS account. Paste the issued API key above."
            >
                <Codicon name="link-external" />
                Or subscribe via AWS Marketplace
            </button>
        </div>
    );

    // Auth method shown on the Account page. Derived from the same signals as the
    // header AuthProviderChip: isByok = own Anthropic key OR Bedrock; isAwsBedrock
    // narrows to Bedrock. Neither = signed in via WSO2 (free with quota).
    const authMethod = isAwsBedrock
        ? {
            icon: "cloud",
            label: "AWS Bedrock",
            detail: "Using your AWS Bedrock credentials — usage is billed to your AWS account.",
        }
        : isByok
            ? {
                icon: "key",
                label: "Anthropic API key",
                detail: "Using your own Anthropic API key — usage is billed to you.",
            }
            : {
                icon: "account",
                label: "WSO2 (MI Copilot)",
                detail: "Signed in via WSO2 — free with a usage quota.",
            };

    const renderAccount = () => (
        <div className="space-y-5">
            {/* How the user is authenticated */}
            <div>
                <h3
                    className="text-[11px] font-medium uppercase tracking-wider mb-2"
                    style={{ color: "var(--vscode-descriptionForeground)" }}
                >
                    Signed in with
                </h3>
                <div
                    className="flex items-center gap-3 p-3 rounded-md"
                    style={{ border: "1px solid var(--vscode-panel-border)" }}
                >
                    <span
                        className="shrink-0 flex items-center justify-center rounded-md"
                        style={{
                            width: 32,
                            height: 32,
                            backgroundColor: "var(--vscode-badge-background)",
                            color: "var(--vscode-badge-foreground)",
                        }}
                    >
                        <Codicon name={authMethod.icon} />
                    </span>
                    <div className="min-w-0">
                        <p className="text-[13px] font-medium" style={{ color: "var(--vscode-foreground)" }}>
                            {authMethod.label}
                        </p>
                        <p className="text-[11px] mt-0.5" style={{ color: "var(--vscode-descriptionForeground)" }}>
                            {authMethod.detail}
                        </p>
                    </div>
                </div>
            </div>

            {/* Sign out */}
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-[13px]" style={{ color: "var(--vscode-foreground)" }}>Sign out</p>
                    <p className="text-[11px] mt-0.5" style={{ color: "var(--vscode-descriptionForeground)" }}>
                        {isByok
                            ? "Clear MI Copilot credentials stored by this extension"
                            : "Sign out of MI Copilot while staying signed in to the WSO2 platform"}
                    </p>
                </div>
                <button
                    className="flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-medium shrink-0 ml-4 transition-colors"
                    style={{
                        color: "var(--vscode-errorForeground)",
                        backgroundColor: "var(--vscode-inputValidation-errorBackground)",
                        border: "1px solid var(--vscode-inputValidation-errorBorder)",
                    }}
                    onClick={handleLogout}
                >
                    <Codicon name="sign-out" />
                    Sign out
                </button>
            </div>
        </div>
    );

    const categoryContent = (category: Exclude<SettingsView, "root">) => {
        switch (category) {
            case "models": return renderModels();
            case "skills": return renderSkills();
            case "web": return isAwsBedrock ? renderWeb() : null;
            case "account": return renderAccount();
        }
    };

    return (
        <div ref={containerRef} className="flex flex-col h-full" style={{ backgroundColor: "var(--vscode-sideBar-background)" }}>
            {/* Header */}
            <div
                className="flex items-center gap-2 px-4 py-3 shrink-0"
                style={{ borderBottom: "1px solid var(--vscode-panel-border)" }}
            >
                <button
                    onClick={onHeaderBack}
                    className="flex items-center justify-center w-7 h-7 rounded-md transition-colors"
                    style={{ color: "var(--vscode-foreground)" }}
                    onMouseEnter={(e) => {
                        (e.currentTarget as HTMLElement).style.backgroundColor = "var(--vscode-list-hoverBackground)";
                    }}
                    onMouseLeave={(e) => {
                        (e.currentTarget as HTMLElement).style.backgroundColor = "transparent";
                    }}
                    title={headerBackLabel}
                    aria-label={headerBackLabel}
                >
                    <Codicon name="arrow-left" />
                </button>
                <span className="text-sm font-semibold" style={{ color: "var(--vscode-foreground)" }}>
                    {headerTitle}
                </span>
            </div>

            {isWide ? (
                /* Wide: persistent nav rail + content pane (Claude-desktop style). */
                <div className="flex flex-1 min-h-0">
                    <nav
                        className="shrink-0 overflow-y-auto p-2 space-y-0.5"
                        style={{ width: 184, borderRight: "1px solid var(--vscode-panel-border)" }}
                    >
                        <CategoryRow
                            icon="hubot"
                            label="Models & Thinking"
                            active={activeCategory === "models"}
                            showChevron={false}
                            onClick={() => setView("models")}
                        />
                        <CategoryRow
                            icon="extensions"
                            label="Skills"
                            badge={managedSkills?.length ?? null}
                            active={activeCategory === "skills"}
                            showChevron={false}
                            onClick={() => setView("skills")}
                        />
                        {isAwsBedrock && (
                            <CategoryRow
                                icon="search"
                                label="Web Search"
                                active={activeCategory === "web"}
                                showChevron={false}
                                onClick={() => setView("web")}
                            />
                        )}
                        <CategoryRow
                            icon="account"
                            label="Account"
                            active={activeCategory === "account"}
                            showChevron={false}
                            onClick={() => setView("account")}
                        />
                    </nav>
                    <div className="flex-1 overflow-y-auto p-5 min-w-0">
                        {categoryContent(activeCategory)}
                    </div>
                </div>
            ) : view === "root" ? (
                /* Narrow root: category list that pushes a sub-page. */
                <div className="flex-1 overflow-y-auto p-3">
                    <div className="space-y-0.5">
                        <CategoryRow icon="hubot" label="Models & Thinking" onClick={() => setView("models")} />
                        <CategoryRow
                            icon="extensions"
                            label="Skills"
                            badge={managedSkills?.length ?? null}
                            onClick={() => setView("skills")}
                        />
                        {isAwsBedrock && (
                            <CategoryRow icon="search" label="Web Search" onClick={() => setView("web")} />
                        )}
                        <CategoryRow icon="account" label="Account" onClick={() => setView("account")} />
                    </div>
                    <p className="text-[11px] px-3 mt-4" style={{ color: "var(--vscode-descriptionForeground)" }}>
                        Settings persist across sessions
                    </p>
                </div>
            ) : (
                /* Narrow sub-page: the selected category, full width. */
                <div className="flex-1 overflow-y-auto p-5">
                    {categoryContent(activeCategory)}
                </div>
            )}
        </div>
    );
};

// --- Helper Components ---

/**
 * Category entry — doubles as a drill-down list row (chevron, navigates on click)
 * and a nav-rail item (`active` highlight, `showChevron={false}`).
 */
function CategoryRow({
    icon,
    label,
    badge,
    onClick,
    active = false,
    showChevron = true,
}: {
    icon: string;
    label: string;
    /** Optional count shown as a pill (e.g. number of skills). null/0 hides it. */
    badge?: number | null;
    onClick: () => void;
    /** Highlight as the selected item (rail layout). */
    active?: boolean;
    /** Trailing chevron — shown in the drill-down list, hidden in the rail. */
    showChevron?: boolean;
}) {
    const [hover, setHover] = useState(false);
    const backgroundColor = active
        ? "var(--vscode-list-activeSelectionBackground)"
        : hover
            ? "var(--vscode-list-hoverBackground)"
            : "transparent";
    const color = active ? "var(--vscode-list-activeSelectionForeground)" : "var(--vscode-foreground)";
    return (
        <button
            type="button"
            onClick={onClick}
            aria-current={active ? "page" : undefined}
            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-md transition-colors text-left"
            style={{ backgroundColor, color, border: "none", cursor: "pointer" }}
            onMouseEnter={() => setHover(true)}
            onMouseLeave={() => setHover(false)}
        >
            <span className="shrink-0 flex items-center justify-center" style={{ width: 18, opacity: active ? 1 : 0.85 }}>
                <Codicon name={icon} />
            </span>
            <span className="flex-1 text-[13px] font-medium truncate">{label}</span>
            {badge != null && badge > 0 && (
                <span
                    className="text-[10px] px-1.5 py-px rounded-full shrink-0"
                    style={{ color: "var(--vscode-badge-foreground)", backgroundColor: "var(--vscode-badge-background)" }}
                >
                    {badge}
                </span>
            )}
            {showChevron && (
                <span className="shrink-0" style={{ opacity: 0.5 }}>
                    <Codicon name="chevron-right" />
                </span>
            )}
        </button>
    );
}

function SettingsSection({ title, children }: { title: string; children: React.ReactNode }) {
    return (
        <div className="space-y-3">
            <h3
                className="text-[11px] font-medium uppercase tracking-wider"
                style={{ color: "var(--vscode-descriptionForeground)" }}
            >
                {title}
            </h3>
            {children}
        </div>
    );
}

function ToggleGroup({
    options,
    selected,
    onSelect,
    compact = false,
    disabled = false,
}: {
    options: string[];
    selected: string;
    onSelect: (value: string) => void;
    compact?: boolean;
    disabled?: boolean;
}) {
    return (
        <div
            className="flex rounded-lg p-0.5"
            style={{
                backgroundColor: "var(--vscode-input-background)",
                border: "1px solid var(--vscode-input-border)",
                opacity: disabled ? 0.6 : 1,
            }}
        >
            {options.map((option) => {
                const isSelected = option === selected;
                return (
                    <button
                        key={option}
                        type="button"
                        onClick={() => onSelect(option)}
                        aria-pressed={isSelected}
                        disabled={disabled}
                        className={`${compact ? "px-3 py-1" : "flex-1 px-3 py-1.5"} rounded-md text-xs font-medium transition-all`}
                        style={{
                            backgroundColor: isSelected ? "var(--vscode-button-background)" : "transparent",
                            color: isSelected ? "var(--vscode-button-foreground)" : "var(--vscode-foreground)",
                            boxShadow: isSelected ? "0 1px 2px rgba(0,0,0,0.1)" : "none",
                            cursor: disabled ? "not-allowed" : "pointer",
                        }}
                    >
                        {option}{isSelected ? " •" : ""}
                    </button>
                );
            })}
        </div>
    );
}

function SkillTag({ text, title }: { text: string; title: string }) {
    return (
        <span
            title={title}
            className="text-[9px] uppercase tracking-wide px-1 py-px rounded shrink-0"
            style={{
                color: "var(--vscode-badge-foreground)",
                backgroundColor: "var(--vscode-badge-background)",
                opacity: 0.8,
            }}
        >
            {text}
        </span>
    );
}

function SkillRow({
    skill,
    busy,
    onToggle,
    onDelete,
}: {
    skill: ManagedSkillItem;
    busy: boolean;
    onToggle: (enabled: boolean) => void;
    onDelete: () => void;
}) {
    const [hover, setHover] = useState(false);
    return (
        <div
            className="flex items-center justify-between gap-2 rounded-md"
            style={{
                padding: "6px 8px",
                backgroundColor: hover ? "var(--vscode-list-hoverBackground)" : "transparent",
            }}
            onMouseEnter={() => setHover(true)}
            onMouseLeave={() => setHover(false)}
        >
            <div className="min-w-0 pr-2">
                <div className="flex items-center gap-1.5">
                    <span
                        className="text-[13px] font-medium truncate"
                        style={{ color: "var(--vscode-foreground)", opacity: skill.enabled ? 1 : 0.55 }}
                    >
                        {skill.name}
                    </span>
                    {skill.disableModelInvocation && (
                        <SkillTag text="model-hidden" title="The skill's SKILL.md sets disable-model-invocation: the model won't auto-invoke it, but you can still run it with /skill-name." />
                    )}
                    {skill.shadowed && (
                        <SkillTag text="shadowed" title="Another skill with the same name takes precedence; this one isn't used by the agent." />
                    )}
                </div>
                <p className="text-[11px] truncate" style={{ color: "var(--vscode-descriptionForeground)" }}>
                    {skill.description}
                </p>
            </div>
            <div className="flex items-center gap-1.5 shrink-0">
                <ToggleGroup
                    options={["Off", "On"]}
                    selected={skill.enabled ? "On" : "Off"}
                    onSelect={(label) => onToggle(label === "On")}
                    compact
                    disabled={busy}
                />
                <button
                    type="button"
                    onClick={onDelete}
                    disabled={busy}
                    title="Delete skill"
                    aria-label={`Delete skill ${skill.name}`}
                    className="flex items-center justify-center w-6 h-6 rounded-md transition-colors"
                    style={{
                        color: "var(--vscode-descriptionForeground)",
                        background: "transparent",
                        border: "none",
                        cursor: busy ? "not-allowed" : "pointer",
                        opacity: busy ? 0.5 : (hover ? 1 : 0.6),
                    }}
                    onMouseEnter={(e) => {
                        (e.currentTarget as HTMLElement).style.color = "var(--vscode-errorForeground)";
                    }}
                    onMouseLeave={(e) => {
                        (e.currentTarget as HTMLElement).style.color = "var(--vscode-descriptionForeground)";
                    }}
                >
                    <Codicon name="trash" />
                </button>
            </div>
        </div>
    );
}

function InfoNote({ icon, variant, text }: { icon: string; variant: "warning" | "info"; text: string }) {
    const color = variant === "warning"
        ? "var(--vscode-editorWarning-foreground, #cca700)"
        : "var(--vscode-editorInfo-foreground, #3794ff)";
    return (
        <div className="flex items-start gap-1.5 -mt-2" style={{ fontSize: "11px", lineHeight: 1.4, color }}>
            <span className="shrink-0 mt-px"><Codicon name={icon} /></span>
            <span>{text}</span>
        </div>
    );
}

export default SettingsPanel;
