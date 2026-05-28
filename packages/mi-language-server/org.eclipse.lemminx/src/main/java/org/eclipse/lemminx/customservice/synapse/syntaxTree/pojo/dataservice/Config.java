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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class Config extends STNode {

    Property[] property;
    String id;
    boolean enableOData;

    public Property[] getProperty() {

        return property;
    }

    public void setProperty(Property[] property) {

        this.property = property;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public boolean isEnableOData() {

        return enableOData;
    }

    public void setEnableOData(boolean enableOData) {

        this.enableOData = enableOData;
    }
}
