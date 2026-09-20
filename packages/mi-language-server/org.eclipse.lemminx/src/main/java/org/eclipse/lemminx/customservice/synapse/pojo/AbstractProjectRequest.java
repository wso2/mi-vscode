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
 * Base class for every {@code synapse/*} request that names the project it belongs to.
 *
 * <p>Declaring the field once here — rather than repeating it in each request — keeps a single
 * shape for it, so a caller never has to know which access style a given request class chose, and
 * a future change to how the project URI behaves (renaming the wire field, tightening its type) is
 * made in one place instead of in every request. Cross-cutting behaviour that depends on the value
 * rather than on the field, such as validating or logging an unresolvable URI, belongs in the
 * resolver that consumes {@link HasProjectUri}, not here.
 *
 * <p>The field stays public so requests that are deserialized straight from JSON keep working
 * without accessors, and optional: a null, blank or unmatched {@code projectUri} resolves to no
 * project and the RPC answers with an empty or failed result, never with another project's data.
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
