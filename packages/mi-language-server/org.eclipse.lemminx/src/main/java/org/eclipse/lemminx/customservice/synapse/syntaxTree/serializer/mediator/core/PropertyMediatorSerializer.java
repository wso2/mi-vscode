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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Property;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

public class PropertyMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Property property = (Property) m;
        OMElement propertyElt = fac.createOMElement("property", synNS);

        if (property.getName() != null) {
            propertyElt.addAttribute("name", property.getName(), nullNS);
        } else {
            handleException("Invalid property mediator. Name is required");
        }

        if (property.getValue() != null) {
            propertyElt.addAttribute("value", property.getValue(), nullNS);
        } else if (property.getAny() != null) {
            OMElement inline = SerializerUtils.stringToOM(property.getAny().toString());
            if (inline != null) {
                propertyElt.addChild(inline);
            }
        } else if (property.getExpression() != null) {
            SerializerUtils.serializeExpression(property.getExpression(), propertyElt, "expression", property);
        } else if ("set".equals(property.getAction())) {
            handleException("Invalid property mediator. Value or expression is required");
        }

        if (property.getScope() != null) {
            propertyElt.addAttribute("scope", property.getScope().getValue(), nullNS);
        }

        if ("remove".equals(property.getAction())) {
            propertyElt.addAttribute("action", property.getAction(), nullNS);
        } else if (property.getType() != null) {
            propertyElt.addAttribute("type", property.getType().name(), nullNS);
        }

        if (property.getPattern() != null) {
            propertyElt.addAttribute("pattern", property.getPattern(), nullNS);
            int group = Utils.parseInt(property.getGroup());
            if (group >= 0) {
                propertyElt.addAttribute("group", property.getGroup(), nullNS);
            }
        }

        if (property.getDescription() != null) {
            propertyElt.addAttribute("description", property.getDescription(), nullNS);
        }

        return propertyElt;
    }

    @Override
    public String getMediatorClassName() {

        return Property.class.getName();
    }
}
