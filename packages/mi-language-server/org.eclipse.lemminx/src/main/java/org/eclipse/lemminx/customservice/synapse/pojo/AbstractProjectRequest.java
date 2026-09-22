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

package org.eclipse.lemminx.customservice.synapse.pojo;

/**
 * Base class that declares the public, optional {@code projectUri} field once for every {@code synapse/*} request, so all such requests share one JSON-deserializable shape instead of repeating it.
 */
public abstract class AbstractProjectRequest implements HasProjectUri {

    public String projectUri;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProjectUri() {

        return projectUri;
    }

    public void setProjectUri(String projectUri) {

        this.projectUri = projectUri;
    }
}
