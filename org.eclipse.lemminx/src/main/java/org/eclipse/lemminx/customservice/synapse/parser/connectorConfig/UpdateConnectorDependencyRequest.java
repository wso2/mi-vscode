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
 * Request to add or update a dependency override in connector-config.json.
 * Fields that are null leave the corresponding values unchanged (or unset for new entries).
 */
public class UpdateConnectorDependencyRequest {

    /** Required. Connector Maven artifactId (e.g. "mi-connector-file"). */
    public String connectorArtifactId;

    /** Matches by connectionType when non-null. */
    public String connectionType;

    /** Override groupId. Null means "no groupId override". */
    public String groupId;

    /** Override artifactId. Null means "no artifactId override". */
    public String artifactId;

    /** Override version. Null means "no version override". */
    public String version;

    /** When true, excludes this dependency from the CAR. */
    public Boolean omit;
}
