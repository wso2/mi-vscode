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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.Jsontransform;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class JsonTransformMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Jsontransform jsontransform = (Jsontransform) m;
        OMElement jsontransformElt = fac.createOMElement("jsontransform", synNS);

        if (jsontransform.getSchema() != null) {
            jsontransformElt.addAttribute("schema", jsontransform.getSchema(), nullNS);
        }
        if (jsontransform.getDescription() != null) {
            jsontransformElt.addAttribute("description", jsontransform.getDescription(), nullNS);
        }

        if (jsontransform.getProperty() != null) {
            serializeMediatorProperties(jsontransformElt, jsontransform.getProperty());
        }

        return jsontransformElt;
    }

    @Override
    public String getMediatorClassName() {

        return Jsontransform.class.getName();
    }
}
