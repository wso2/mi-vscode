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

import java.util.ArrayList;
import java.util.List;

public class AdvancedNode extends Node {

    private List<Node> sequences;
    private List<Node> endpoints;

    public AdvancedNode(String type, String name, String path) {

        super(type, name, path);
        this.sequences = new ArrayList<>();
        this.endpoints = new ArrayList<>();
    }

    public AdvancedNode(Node component) {

        super(component.getType(), component.getSubType(), component.getName(), component.getPath());
        this.sequences = new ArrayList<>();
        this.endpoints = new ArrayList<>();
    }

    public void addEndpoint(Node endpoint) {

        if (!alreadyExists(endpoints, endpoint)) {
            endpoints.add(endpoint);
        }
    }

    public void addSequence(Node sequence) {

        if (!alreadyExists(sequences, sequence)) {
            sequences.add(sequence);
        }
    }

    private Boolean alreadyExists(List<Node> components, Node component) {

        for (Node c : components) {
            if (c.getName().equals(component.getName())) {
                return true;
            }
        }
        return false;
    }
}
