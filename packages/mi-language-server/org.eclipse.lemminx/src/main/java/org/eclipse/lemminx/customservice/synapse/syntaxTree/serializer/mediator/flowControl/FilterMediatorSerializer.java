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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.filter.Filter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.AnonymousSequenceSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

import java.util.List;

public class FilterMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Filter filter = (Filter) m;
        OMElement filterElt = fac.createOMElement("filter", synNS);

        if (filter.getSource() != null && filter.getRegex() != null) {
            filterElt.addAttribute("source", filter.getSource(), null);
            filterElt.addAttribute("regex", filter.getRegex(), null);
        } else if (filter.getXpath() != null) {
            SerializerUtils.serializeExpression(filter.getXpath(), filterElt, "xpath", filter);
        } else {
            handleException("Invalid filter mediator. " +
                    "Should have either a 'source' and a 'regex' OR an 'xpath' ");
        }

        serializeThenElse(filterElt, filter, "then");
        serializeThenElse(filterElt, filter, "else");

        if (filter.getDescription() != null) {
            filterElt.addAttribute("description", filter.getDescription(), null);
        }
        return filterElt;
    }

    private void serializeThenElse(OMElement filterElt, Filter filter, String tagName) {

        if (filter.getThen() != null) {
            List<Mediator> thenMediatorList = filter.getThen().getMediatorList();
            OMElement thenElement = AnonymousSequenceSerializer.serializeAnonymousSequence(thenMediatorList);
            thenElement.setLocalName(tagName);
            filterElt.addChild(thenElement);
        }
    }

    @Override
    public String getMediatorClassName() {

        return Filter.class.getName();
    }
}
