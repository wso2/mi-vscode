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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.Datamapper;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.SchemaType;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;

public class DataMapperFactory extends AbstractMediatorFactory {

    private static final String DATA_MAPPER = "datamapper";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Datamapper datamapper = new Datamapper();
        datamapper.elementNode(element);
        populateAttributes(datamapper, element);
        return datamapper;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String config = element.getAttribute(Constant.CONFIG);
        if (config != null) {
            ((Datamapper) node).setConfig(config);
        }
        String inputSchema = element.getAttribute(Constant.INPUT_SCHEMA);
        if (inputSchema != null) {
            ((Datamapper) node).setInputSchema(inputSchema);
        }
        String outputSchema = element.getAttribute(Constant.OUTPUT_SCHEMA);
        if (outputSchema != null) {
            ((Datamapper) node).setOutputSchema(outputSchema);
        }
        String inputType = element.getAttribute(Constant.INPUT_TYPE);
        SchemaType inputTypeEnum = Utils.getEnumFromValue(inputType, SchemaType.class);
        if (inputTypeEnum != null) {
            ((Datamapper) node).setInputType(inputTypeEnum);
        }
        String outputType = element.getAttribute(Constant.OUTPUT_TYPE);
        SchemaType outputTypeEnum = Utils.getEnumFromValue(outputType, SchemaType.class);
        if (outputTypeEnum != null) {
            ((Datamapper) node).setOutputType(outputTypeEnum);
        }
        String xsltStyleSheet = element.getAttribute(Constant.XSLT_STYLE_SHEET);
        if (xsltStyleSheet != null) {
            ((Datamapper) node).setXsltStyleSheet(xsltStyleSheet);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Datamapper) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Datamapper) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return DATA_MAPPER;
    }
}
