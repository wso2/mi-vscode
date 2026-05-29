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
package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.ThrowError;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;

/**
 * Factory for creating ThrowError mediator
 */
public class ThrowErrorFactory extends AbstractMediatorFactory {

    private static final String THROW_ERROR = "throwError";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {
        ThrowError throwError = new ThrowError();
        throwError.elementNode(element);
        populateAttributes(throwError, element);
        return throwError;
    }

    @Override
    public String getTagName() {
        return THROW_ERROR;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {
        ThrowError throwError = (ThrowError) node;
        String type = element.getAttribute(Constant.TYPE);
        if (type != null) {
            throwError.setType(type);
        }
        String errorMessage = element.getAttribute(Constant.ERROR_MESSAGE);
        if (errorMessage != null) {
            throwError.setErrorMessage(errorMessage);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            throwError.setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            throwError.setTraceFilter(traceFilter);
        }
    }
}
