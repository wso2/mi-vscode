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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ESBNode extends Node {

    private HashMap<String, List<Node>> esbConfigs;

    public ESBNode(String type, String name, String path) {

        super(type, name, path);
        this.esbConfigs = new HashMap<>();
        List<String> esbKeys = new ArrayList<>(Arrays.asList(
                "api", "endpoints", "inbound-endpoints", "message-processors",
                "local-entries", "message-stores", "proxy-services", "sequences",
                "tasks", "templates"
        ));
        for (String key : esbKeys) {
            esbConfigs.put(key, new ArrayList<>());
        }
    }

    public void addEsbConfig(String type, Node component) {

        esbConfigs.get(type).add(component);
    }
}
