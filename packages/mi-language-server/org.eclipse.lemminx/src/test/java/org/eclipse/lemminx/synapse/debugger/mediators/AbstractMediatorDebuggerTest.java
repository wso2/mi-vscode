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

package org.eclipse.lemminx.synapse.debugger.mediators;

import org.eclipse.lemminx.synapse.debugger.AbstractDebuggerTest;

public abstract class AbstractMediatorDebuggerTest extends AbstractDebuggerTest {

    private String MEDIATOR_RESOURCE_FOLDER = "/synapse/debugger/mediators/";

    @Override
    protected String getResourcePath() {

        return MEDIATOR_RESOURCE_FOLDER + getTestResourceName();
    }

    protected abstract String getTestResourceName();
}
