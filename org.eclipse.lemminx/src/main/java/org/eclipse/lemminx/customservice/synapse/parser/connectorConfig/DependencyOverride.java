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
 * Represents a single dependency override entry in connector-config.json.
 * A match is attempted first by connectionType, then by groupId + artifactId.
 */
public class DependencyOverride {

    public String connectionType;
    public String groupId;
    public String artifactId;
    public String version;
    public Boolean omit;
}
