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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.extension;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Class;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class ClassMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Class classMediator = (Class) m;
        OMElement classElt = fac.createOMElement("class", synNS);

        if (classMediator.getName() != null) {
            classElt.addAttribute("name", classMediator.getName(), null);
        }
        if (classMediator.getDescription() != null) {
            classElt.addAttribute("description", classMediator.getDescription(), null);
        }

        serializeMediatorProperties(classElt, classMediator.getProperty());
        return classElt;
    }

    @Override
    public String getMediatorClassName() {

        return Class.class.getName();
    }
}
