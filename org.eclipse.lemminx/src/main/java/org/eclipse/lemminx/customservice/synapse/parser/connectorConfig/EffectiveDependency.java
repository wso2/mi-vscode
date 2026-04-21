/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     WSO2 LLC - support for WSO2 Micro Integrator Configuration
 */

package org.eclipse.lemminx.customservice.synapse.parser.connectorConfig;

/**
 * The merged view of a single connector dependency — descriptor.yml defaults overlaid with
 * any user overrides from connector-config.json. This is what the VS Code extension renders
 * in the dependency management UI.
 */
public class EffectiveDependency {

    /** Connection type from descriptor.yml (may be null for non-connection-typed deps). */
    public String connectionType;

    /** Effective groupId (override if set, else descriptor.yml default). */
    public String groupId;

    /** Effective artifactId (override if set, else descriptor.yml default). */
    public String artifactId;

    /** The version declared in descriptor.yml (the connector's tested/recommended version). */
    public String defaultVersion;

    /**
     * The version from connector-config.json override, or null if no version override is set.
     * When non-null, this is what the build will use instead of defaultVersion.
     */
    public String overriddenVersion;

    /** True when this dependency is excluded from the CAR via connector-config.json. */
    public boolean omit;

    /**
     * True when any field (version, groupId, artifactId, or omit) differs from the descriptor.yml
     * default — i.e., when the user has an active override for this dependency.
     */
    public boolean isOverridden;
}
