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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.datasource;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class JndiConfigType extends STNode {

    STNode name;
    JndiEnvType environment;
    boolean useDataSourceFactory;

    public STNode getName() {

        return name;
    }

    public void setName(STNode name) {

        this.name = name;
    }

    public JndiEnvType getEnvironment() {

        return environment;
    }

    public void setEnvironment(JndiEnvType environment) {

        this.environment = environment;
    }

    public boolean isUseDataSourceFactory() {

        return useDataSourceFactory;
    }

    public void setUseDataSourceFactory(boolean useDataSourceFactory) {

        this.useDataSourceFactory = useDataSourceFactory;
    }
}
