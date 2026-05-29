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

package org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SequenceDebugInfo extends DebugInfo {

    String sequenceKey;

    public void setSequenceKey(String sequenceKey) {

        this.sequenceKey = sequenceKey;
    }

    @Override
    public JsonElement toJson() {

        JsonObject rootNode = new JsonObject();
        JsonObject sequence = new JsonObject();
        sequence.addProperty("sequence-type", "named");
        sequence.addProperty("sequence-key", sequenceKey);
        sequence.addProperty("mediator-position", mediatorPosition);

        rootNode.add("sequence", sequence);

        rootNode.addProperty("mediation-component", "sequence");

        return rootNode;
    }

    @Override
    public String toString() {

        return "SequenceDebugInfo{" +
                "sequenceKey='" + sequenceKey + '\'' +
                ", mediatorPosition='" + mediatorPosition + '\'' +
                ", isValid=" + isValid +
                ", error='" + error + '\'' +
                '}';
    }
}
