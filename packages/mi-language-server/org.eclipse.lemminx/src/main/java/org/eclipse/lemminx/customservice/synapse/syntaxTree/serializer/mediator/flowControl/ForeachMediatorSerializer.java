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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.Foreach;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.AnonymousSequenceSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class ForeachMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Foreach foreachMediator = (Foreach) m;
        OMElement foreachElt = fac.createOMElement("foreach", synNS);

        if (foreachMediator.getId() != null) {
            foreachElt.addAttribute("id", foreachMediator.getId(), null);
        }
        if (foreachMediator.getExpression() != null) {
            SerializerUtils.serializeExpression(foreachMediator.getExpression(), foreachElt, "expression",
                    foreachMediator);
        } else {
            handleException("Invalid Foreach mediator. " +
                    "Should have an 'expression' ");
        }

        if (foreachMediator.getSequenceAttribute() != null) {
            foreachElt.addAttribute("sequence", foreachMediator.getSequenceAttribute(), null);
        } else if (foreachMediator.getSequence() != null) {
            OMElement sequenceElt = AnonymousSequenceSerializer.serializeAnonymousSequence(foreachMediator.getSequence());
            foreachElt.addChild(sequenceElt);
        } else {
            handleException("Invalid Foreach mediator. " +
                    "Should have a 'sequence' ");
        }

        if (foreachMediator.getDescription() != null) {
            foreachElt.addAttribute("description", foreachMediator.getDescription(), null);
        }

        return foreachElt;
    }

    @Override
    public String getMediatorClassName() {

        return Foreach.class.getName();
    }
}
