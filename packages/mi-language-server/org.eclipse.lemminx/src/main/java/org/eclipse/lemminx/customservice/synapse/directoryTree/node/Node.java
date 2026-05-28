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

package org.eclipse.lemminx.customservice.synapse.directoryTree.node;

public class Node {

    String type;
    String subType;
    String name;
    String path;
    Boolean isFaulty = false;

    public Node(String type, String name, String path) {

        this.type = type;
        this.name = name;
        this.path = path;
    }

    public Node(String type, String subType, String name, String path) {

        this.type = type;
        this.subType = subType;
        this.name = name;
        this.path = path;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public String getSubType() {

        return subType;
    }

    public void setSubType(String subType) {

        this.subType = subType;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getPath() {

        return path;
    }

    public void setPath(String path) {

        this.path = path;
    }

    public Boolean getFaulty() {

        return isFaulty;
    }

    public void setFaulty(Boolean faulty) {

        isFaulty = faulty;
    }

    protected Boolean equals(Node component) {

        return this.type.equals(component.type) && this.name.equals(component.name) && this.path.equals(component.path);
    }
}
