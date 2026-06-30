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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.Iterate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.targets.Target;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.AnonymousSequenceSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class IterateMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Iterate iterateMediator = (Iterate) m;
        OMElement iterateElt = fac.createOMElement("iterate", synNS);

        if (iterateMediator.getId() != null) {
            iterateElt.addAttribute("id", iterateMediator.getId(), null);
        }
        if (iterateMediator.getExpression() != null) {
            SerializerUtils.serializeExpression(iterateMediator.getExpression(), iterateElt, "expression",
                    iterateMediator);
        } else {
            handleException("Invalid Iterate mediator. " +
                    "Should have an 'expression' ");
        }
        if (iterateMediator.getAttachPath() != null) {
            SerializerUtils.serializeExpression(iterateMediator.getAttachPath(), iterateElt, "attachPath",
                    iterateMediator);
        }
        if (iterateMediator.isPreservePayload()) {
            iterateElt.addAttribute("preservePayload", "true", null);
        }
        if (iterateMediator.isContinueParent()) {
            iterateElt.addAttribute("continueParent", "true", null);
        }
        if (iterateMediator.isSequential()) {
            iterateElt.addAttribute("sequential", "true", null);
        }
        if (iterateMediator.getDescription() != null) {
            iterateElt.addAttribute("description", iterateMediator.getDescription(), null);
        }

        serializeTarget(iterateElt, iterateMediator.getTarget());

        return iterateElt;
    }

    private void serializeTarget(OMElement iterateElt, Target target) {

        if (target != null) {
            OMElement targetEle = fac.createOMElement("target", synNS);
            if (target.getSequenceAttribute() != null) {
                targetEle.addAttribute("sequence", target.getSequenceAttribute(), null);
            } else if (target.getSequence() != null) {
                OMElement sequenceElement = AnonymousSequenceSerializer.serializeAnonymousSequence(target.getSequence());
                targetEle.addChild(sequenceElement);
            } else {
                handleException("Invalid target mediator. " +
                        "Should have a 'sequence' ");
            }
            iterateElt.addChild(targetEle);
        } else {
            handleException("Invalid Iterate mediator. " +
                    "Should have a 'target' ");
        }
    }

    @Override
    public String getMediatorClassName() {

        return Iterate.class.getName();
    }
}
