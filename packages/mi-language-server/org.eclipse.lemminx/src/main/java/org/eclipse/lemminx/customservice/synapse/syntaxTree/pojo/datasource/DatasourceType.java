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

public class DatasourceType extends STNode {

    STNode name;
    STNode description;
    JndiConfigType jndiConfig;
    DefinitionType definition;

    public STNode getName() {

        return name;
    }

    public void setName(STNode name) {

        this.name = name;
    }

    public STNode getDescription() {

        return description;
    }

    public void setDescription(STNode description) {

        this.description = description;
    }

    public JndiConfigType getJndiConfig() {

        return jndiConfig;
    }

    public void setJndiConfig(JndiConfigType jndiConfig) {

        this.jndiConfig = jndiConfig;
    }

    public DefinitionType getDefinition() {

        return definition;
    }

    public void setDefinition(DefinitionType definition) {

        this.definition = definition;
    }
}
