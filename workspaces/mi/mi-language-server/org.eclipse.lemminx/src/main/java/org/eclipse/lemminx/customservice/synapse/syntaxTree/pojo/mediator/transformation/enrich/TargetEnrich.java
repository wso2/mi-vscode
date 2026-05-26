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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class TargetEnrich extends STNode {

    TargetEnrichAction action;
    TargetEnrichType type;
    String xpath;
    String property;

    public TargetEnrichAction getAction() {

        return action;
    }

    public void setAction(TargetEnrichAction action) {

        this.action = action;
    }

    public TargetEnrichType getType() {

        return type;
    }

    public void setType(TargetEnrichType type) {

        this.type = type;
    }

    public String getXpath() {

        return xpath;
    }

    public void setXpath(String xpath) {

        this.xpath = xpath;
    }

    public String getProperty() {

        return property;
    }

    public void setProperty(String property) {

        this.property = property;
    }
}
