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
package org.eclipse.lemminx.customservice.synapse.parser;

import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.List;

public class PluginDetails {
    private Node miContainerPluginVersion;
    private Node unitTestPluginVersion;
    private Node projectBuildPluginVersion;
    private List<Range> ranges;

    PluginDetails() {
        ranges = new ArrayList<>();
    }

    public void setMiContainerPluginVersion(Node miContainerPluginVersion) {
        this.miContainerPluginVersion = miContainerPluginVersion;
    }

    public void setUnitTestPluginVersion(Node unitTestPluginVersion) {
        this.unitTestPluginVersion = unitTestPluginVersion;
    }

    public void setProjectBuildPluginVersion(String pluginVersion, Range range) {
        ranges.add(range);
        this.projectBuildPluginVersion = new Node(pluginVersion, Either.forRight(ranges));
    }

    public void initialiseRanges() {
        this.ranges = new ArrayList<>();
    }

    public Node getProjectBuildPluginVersion() {
        return projectBuildPluginVersion;
    }

    public Node getMiContainerPluginVersion() {
        return miContainerPluginVersion;
    }

    public Node getUnitTestPluginVersion() {
        return unitTestPluginVersion;
    }
}
