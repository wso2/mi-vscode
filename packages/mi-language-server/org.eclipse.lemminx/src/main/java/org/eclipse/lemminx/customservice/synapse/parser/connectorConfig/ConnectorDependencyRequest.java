/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import org.eclipse.lemminx.customservice.synapse.pojo.HasProjectUri;

/**
 * Request to retrieve effective connector dependencies.
 * When {@code connectorArtifactId} is null, all connectors in the project are returned.
 */
public class ConnectorDependencyRequest implements HasProjectUri {

    /** Connector Maven artifactId (e.g. "mi-connector-file"). Null means "all connectors". */
    public String connectorArtifactId;
    public String projectUri;

    @Override
    public String getProjectUri() {

        return projectUri;
    }
}
