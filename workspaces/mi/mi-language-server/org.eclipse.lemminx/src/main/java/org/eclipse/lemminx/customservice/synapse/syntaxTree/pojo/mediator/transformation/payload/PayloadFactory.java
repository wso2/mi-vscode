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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class PayloadFactory extends Mediator {

    PayloadFactoryFormat format;
    PayloadFactoryArgs args;
    MediaType mediaType;
    TemplateType templateType;
    String description;
    String traceFilter;

    public PayloadFactory() {

        setDisplayName("Payload");
    }

    public PayloadFactoryFormat getFormat() {

        return format;
    }

    public void setFormat(PayloadFactoryFormat format) {

        this.format = format;
    }

    public PayloadFactoryArgs getArgs() {

        return args;
    }

    public void setArgs(PayloadFactoryArgs args) {

        this.args = args;
    }

    public MediaType getMediaType() {

        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {

        this.mediaType = mediaType;
    }

    public TemplateType getTemplateType() {

        return templateType;
    }

    public void setTemplateType(TemplateType templateType) {

        this.templateType = templateType;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getTraceFilter() {

        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {

        this.traceFilter = traceFilter;
    }
}
