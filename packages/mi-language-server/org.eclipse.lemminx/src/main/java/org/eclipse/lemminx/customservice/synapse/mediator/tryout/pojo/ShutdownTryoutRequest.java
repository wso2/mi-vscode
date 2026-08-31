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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo;

/**
 * Carries the initiating project's URI so {@code shutDownTryoutServer} only tears down the single
 * shared {@code TryOutManager} when it is still bound to that same project, instead of letting an
 * unrelated project's request kill another project's active try-out session.
 */
public class ShutdownTryoutRequest {

    private String projectUri;

    public String getProjectUri() {

        return projectUri;
    }
}
