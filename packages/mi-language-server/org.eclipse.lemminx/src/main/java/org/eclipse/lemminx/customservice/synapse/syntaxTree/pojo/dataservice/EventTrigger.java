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

import java.util.List;

public class EventTrigger extends STNode {

    List<STNode> eventTriggerElements;
    String id;
    String language;

    public List<STNode> getEventTriggerElements() {

        return eventTriggerElements;
    }

    public void setEventTriggerElements(List<STNode> eventTriggerElements) {

        this.eventTriggerElements = eventTriggerElements;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public String getLanguage() {

        return language;
    }

    public void setLanguage(String language) {

        this.language = language;
    }
}
