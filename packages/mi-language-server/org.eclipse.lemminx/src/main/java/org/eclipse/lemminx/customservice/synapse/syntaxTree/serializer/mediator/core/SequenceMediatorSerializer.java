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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.SequenceMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class SequenceMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        SequenceMediator sequenceMediator = (SequenceMediator) m;
        OMElement sequenceElt = fac.createOMElement("sequence", synNS);

        if (sequenceMediator.getKey() != null) {
            String key = sequenceMediator.getKey();
            sequenceElt.addAttribute("key", key, nullNS);
            if (key.matches("^\\{.*}$")) {
                SerializerUtils.serializeNamespaces(sequenceMediator, sequenceElt);
            }
        } else {
            handleException("The 'key' attribute is required for the Sequence mediator");
        }

        if (sequenceMediator.getDescription() != null) {
            sequenceElt.addAttribute("description", sequenceMediator.getDescription(), nullNS);
        }

        return sequenceElt;
    }

    @Override
    public String getMediatorClassName() {

        return SequenceMediator.class.getName();
    }
}
