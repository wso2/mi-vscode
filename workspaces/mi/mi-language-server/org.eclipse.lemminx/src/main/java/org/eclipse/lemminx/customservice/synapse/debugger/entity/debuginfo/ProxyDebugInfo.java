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

public class ProxyDebugInfo extends DebugInfo {

    String proxyKey;
    String sequenceType;

    public void setProxyKey(String proxyKey) {

        this.proxyKey = proxyKey;
    }

    public void setSequenceType(String sequenceType) {

        this.sequenceType = sequenceType;
    }

    public String getProxyKey() {

        return proxyKey;
    }

    public String getSequenceType() {

        return sequenceType;
    }

    @Override
    public JsonElement toJson() {

        JsonObject rootNode = new JsonObject();
        JsonObject sequence = new JsonObject();
        JsonObject proxy = new JsonObject();
        proxy.addProperty("proxy-key", proxyKey);
        proxy.addProperty("sequence-type", sequenceType);
        proxy.addProperty("mediator-position", mediatorPosition);
        sequence.add("proxy", proxy);
        rootNode.add("sequence", sequence);

        rootNode.addProperty("mediation-component", "sequence");
        return rootNode;
    }

    @Override
    public String toString() {

        return "ProxyDebugInfo{" +
                "proxyKey='" + proxyKey + '\'' +
                ", sequenceType='" + sequenceType + '\'' +
                ", mediatorPosition='" + mediatorPosition + '\'' +
                ", isValid=" + isValid +
                ", error='" + error + '\'' +
                '}';
    }
}
