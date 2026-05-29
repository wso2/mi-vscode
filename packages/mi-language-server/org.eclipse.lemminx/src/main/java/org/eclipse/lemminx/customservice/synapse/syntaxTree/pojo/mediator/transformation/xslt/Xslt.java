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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xslt;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;

public class Xslt extends Mediator {

    MediatorProperty[] property;
    XsltFeature[] feature;
    XsltResource[] resource;
    String key;
    String source;
    String description;
    String traceFilter;

    public Xslt() {
        setDisplayName("XSLT");
    }

    public MediatorProperty[] getProperty() {

        return property;
    }

    public void setProperty(MediatorProperty[] property) {

        this.property = property;
    }

    public XsltFeature[] getFeature() {

        return feature;
    }

    public void setFeature(XsltFeature[] feature) {

        this.feature = feature;
    }

    public XsltResource[] getResource() {

        return resource;
    }

    public void setResource(XsltResource[] resource) {

        this.resource = resource;
    }

    public String getKey() {

        return key;
    }

    public void setKey(String key) {

        this.key = key;
    }

    public String getSource() {

        return source;
    }

    public void setSource(String source) {

        this.source = source;
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
