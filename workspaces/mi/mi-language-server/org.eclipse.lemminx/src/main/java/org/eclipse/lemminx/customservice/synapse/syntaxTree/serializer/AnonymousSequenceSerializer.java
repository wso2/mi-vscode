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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.MediatorSerializerFinder;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.util.List;

public class AnonymousSequenceSerializer {

    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    public static OMElement serializeAnonymousSequence(Sequence sequence) {

        List<Mediator> mediatorList = null;
        if (sequence != null) {
            mediatorList = sequence.getMediatorList();
        }
        OMElement sequenceElt = serializeAnonymousSequence(mediatorList);
        return sequenceElt;
    }

    public static OMElement serializeAnonymousSequence(List<Mediator> mediatorList) {

        OMElement sequenceElt = fac.createOMElement("sequence", Constant.SYNAPSE_OMNAMESPACE);

        serializeAnonymousSequence(mediatorList, sequenceElt);
        return sequenceElt;
    }

    public static OMElement serializeAnonymousSequence(List<Mediator> mediatorList, OMElement parentElt) {

        if (mediatorList != null) {
            for (Mediator mediator : mediatorList) {
                AbstractMediatorSerializer serializer = MediatorSerializerFinder.getInstance().getSerializer(mediator);
                if (serializer != null) {
                    serializer.serializeMediator(parentElt, mediator);
                }
            }
        }
        return parentElt;
    }

}
