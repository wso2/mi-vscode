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

public class TemplateDebugInfo extends DebugInfo {

    String templateKey;

    public void setTemplateKey(String templateKey) {

        this.templateKey = templateKey;
    }

    public String getTemplateKey() {

        return templateKey;
    }

    @Override
    public JsonElement toJson() {

        JsonObject rootNode = new JsonObject();
        JsonObject template = new JsonObject();
        template.addProperty("template-key", templateKey);
        template.addProperty("mediator-position", mediatorPosition);
        rootNode.add("template", template);

        rootNode.addProperty("mediation-component", "template");

        return rootNode;
    }

    @Override
    public String toString() {

        return "TemplateDebugInfo{" +
                "templateKey='" + templateKey + '\'' +
                ", mediatorPosition='" + mediatorPosition + '\'' +
                ", isValid=" + isValid +
                ", error='" + error + '\'' +
                '}';
    }
}
