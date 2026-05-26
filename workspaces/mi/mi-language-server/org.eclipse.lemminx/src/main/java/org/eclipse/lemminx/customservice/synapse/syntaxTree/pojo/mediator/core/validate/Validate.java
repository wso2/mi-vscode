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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Feature;

public class Validate extends Mediator {

    ValidateProperty[] property;
    ValidateSchema[] schema;
    ValidateOnFail onFail;
    Feature[] feature;
    ValidateResource[] resource;
    boolean cacheSchema;
    String source;
    String description;
    String traceFilter;

    public Validate() {
        setDisplayName("Validate");
    }

    public ValidateProperty[] getProperty() {

        return property;
    }

    public void setProperty(ValidateProperty[] property) {

        this.property = property;
    }

    public ValidateSchema[] getSchema() {

        return schema;
    }

    public void setSchema(ValidateSchema[] schema) {

        this.schema = schema;
    }

    public ValidateOnFail getOnFail() {

        return onFail;
    }

    public void setOnFail(ValidateOnFail onFail) {

        this.onFail = onFail;
    }

    public Feature[] getFeature() {

        return feature;
    }

    public void setFeature(Feature[] feature) {

        this.feature = feature;
    }

    public ValidateResource[] getResource() {

        return resource;
    }

    public void setResource(ValidateResource[] resource) {

        this.resource = resource;
    }

    public boolean isCacheSchema() {

        return cacheSchema;
    }

    public void setCacheSchema(boolean cacheSchema) {

        this.cacheSchema = cacheSchema;
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
