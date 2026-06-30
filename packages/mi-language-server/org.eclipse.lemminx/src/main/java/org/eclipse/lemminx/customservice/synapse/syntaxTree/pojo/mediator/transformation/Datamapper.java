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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Datamapper extends Mediator {

    String config;
    String inputSchema;
    String outputSchema;
    SchemaType inputType;
    SchemaType outputType;
    String xsltStyleSheet;
    String description;
    String traceFilter;

    public Datamapper() {
        setDisplayName("Data Mapper");
    }

    public String getConfig() {

        return config;
    }

    public void setConfig(String config) {

        this.config = config;
    }

    public String getInputSchema() {

        return inputSchema;
    }

    public void setInputSchema(String inputSchema) {

        this.inputSchema = inputSchema;
    }

    public String getOutputSchema() {

        return outputSchema;
    }

    public void setOutputSchema(String outputSchema) {

        this.outputSchema = outputSchema;
    }

    public SchemaType getInputType() {

        return inputType;
    }

    public void setInputType(SchemaType inputType) {

        this.inputType = inputType;
    }

    public SchemaType getOutputType() {

        return outputType;
    }

    public void setOutputType(SchemaType outputType) {

        this.outputType = outputType;
    }

    public String getXsltStyleSheet() {

        return xsltStyleSheet;
    }

    public void setXsltStyleSheet(String xsltStyleSheet) {

        this.xsltStyleSheet = xsltStyleSheet;
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
