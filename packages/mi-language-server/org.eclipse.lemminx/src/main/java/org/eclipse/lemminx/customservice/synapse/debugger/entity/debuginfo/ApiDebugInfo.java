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

public class ApiDebugInfo extends DebugInfo {

    String apiKey;
    String method;
    String uriTemplate;
    String urlMapping;
    String sequenceType;

    public void setApiKey(String apiKey) {

        this.apiKey = apiKey;
    }

    public void setMethod(String method) {

        this.method = method;
    }

    public void setSequenceType(String sequenceType) {

        this.sequenceType = sequenceType;
    }

    public void setUriTemplate(String uriTemplate) {

        this.uriTemplate = uriTemplate;
    }

    public void setUrlMapping(String urlMapping) {

        this.urlMapping = urlMapping;
    }

    public String getApiKey() {

        return apiKey;
    }

    public String getMethod() {

        return method;
    }

    public String getUriTemplate() {

        return uriTemplate;
    }

    public String getUrlMapping() {

        return urlMapping;
    }

    public String getSequenceType() {

        return sequenceType;
    }

    public JsonElement toJson() {

        JsonObject rootNode = new JsonObject();
        JsonObject sequence = new JsonObject();
        JsonObject api = new JsonObject();
        api.addProperty("api-key", apiKey);
        JsonObject resource = new JsonObject();
        resource.addProperty("method", method);
        if (uriTemplate != null) {
            resource.addProperty("uri-template", uriTemplate);
        } else {
            resource.addProperty("url-mapping", urlMapping);
        }
        api.add("resource", resource);
        api.addProperty("sequence-type", sequenceType);
        api.addProperty("mediator-position", mediatorPosition);
        sequence.add("api", api);
        rootNode.add("sequence", sequence);

        rootNode.addProperty("mediation-component", "sequence");
        return rootNode;
    }

    @Override
    public String toString() {

        return "ApiDebugInfo{" +
                "apiKey='" + apiKey + '\'' +
                ", method='" + method + '\'' +
                ", uriTemplate='" + uriTemplate + '\'' +
                ", urlMapping='" + urlMapping + '\'' +
                ", sequenceType='" + sequenceType + '\'' +
                ", mediatorPosition='" + mediatorPosition + '\'' +
                ", isValid=" + isValid +
                ", error='" + error + '\'' +
                '}';
    }
}
