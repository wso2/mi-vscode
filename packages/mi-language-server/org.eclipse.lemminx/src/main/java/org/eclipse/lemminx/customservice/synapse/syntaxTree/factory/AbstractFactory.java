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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.dom.DOMElement;

public abstract class AbstractFactory {

    private String projectPath;
    private String miVersion;
    public abstract STNode create(DOMElement element);

    public abstract void populateAttributes(STNode node, DOMElement element);

    public String getMiVersion() {

        return miVersion;
    }

    public void setMiVersion(String miVersion) {

        this.miVersion = miVersion;
    }

    public String getProjectPath() {

        return projectPath;
    }

    public void setProjectPath(String projectPath) {

        this.projectPath = projectPath;
    }
}
