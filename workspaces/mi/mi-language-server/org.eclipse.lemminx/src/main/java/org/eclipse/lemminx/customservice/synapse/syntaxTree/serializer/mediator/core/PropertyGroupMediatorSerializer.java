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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.PropertyGroup;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class PropertyGroupMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        PropertyGroup propertyGroup = (PropertyGroup) m;
        OMElement propertyGroupElt = fac.createOMElement("propertyGroup", synNS);

        Property[] properties = propertyGroup.getProperty();
        if (properties != null) {
            PropertyMediatorSerializer propertyMediatorSerializer = new PropertyMediatorSerializer();
            for (Property property : properties) {
                OMElement propertyElt = propertyMediatorSerializer.serializeSpecificMediator(property);
                propertyGroupElt.addChild(propertyElt);
            }
        }

        if (propertyGroup.getDescription() != null) {
            propertyGroupElt.addAttribute("description", propertyGroup.getDescription(), nullNS);
        }
        return propertyGroupElt;
    }

    @Override
    public String getMediatorClassName() {

        return PropertyGroup.class.getName();
    }
}
