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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Header;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class HeaderMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Header headerMediator = (Header) m;
        OMElement headerElt = fac.createOMElement("header", synNS);

        if (headerMediator.getName() != null) {
            SerializerUtils.serializeExpression(headerMediator.getName(), headerElt, "name", headerMediator);
        }
        if (headerMediator.getAction() != null) {
            headerElt.addAttribute("action", headerMediator.getAction(), null);
        }
        if (headerMediator.getScope() != null) {
            headerElt.addAttribute("scope", headerMediator.getScope().getValue(), null);
        }
        if (headerMediator.getAction() != null) {
            String action = headerMediator.getAction();
            if ("set".equals(action)) {
                if (headerMediator.getValue() != null) {
                    headerElt.addAttribute("value", headerMediator.getValue(), null);
                } else if (headerMediator.getAny() != null) {
                    String inline = headerMediator.getAny().toString();
                    OMElement inlineElt = SerializerUtils.stringToOM(inline);
                    headerElt.addChild(inlineElt);
                } else if (headerMediator.getExpression() != null) {
                    SerializerUtils.serializeExpression(headerMediator.getExpression(), headerElt,
                            "expression", headerMediator);
                }
            } else if (!"remove".equals(action)) {
                handleException("Invalid action for header mediator. Action should be either 'set' or 'remove'");
            }
        }
        if (headerMediator.getDescription() != null) {
            headerElt.addAttribute("description", headerMediator.getDescription(), null);
        }
        return headerElt;
    }

    @Override
    public String getMediatorClassName() {

        return Header.class.getName();
    }
}
