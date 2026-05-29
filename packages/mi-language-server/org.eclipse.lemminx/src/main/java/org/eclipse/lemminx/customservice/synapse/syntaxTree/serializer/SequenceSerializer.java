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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.NamedSequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SequenceSerializer {

    private static Logger log = Logger.getLogger(SequenceSerializer.class.getName());
    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    public static String serializeSequence(NamedSequence sequence) {

        OMElement sequenceElt = serializeMediators(sequence.getMediatorList());
        if (sequenceElt == null) {
            sequenceElt = fac.createOMElement("sequence", Constant.SYNAPSE_OMNAMESPACE);
        }

        if (sequence.getName() != null) {
            sequenceElt.addAttribute("name", sequence.getName(), null);
        } else {
            handleException("Sequence name is required");
        }
        if (sequence.getOnError() != null) {
            sequenceElt.addAttribute("onError", sequence.getOnError(), null);
        }
        if (sequence.getStatistics() != null) {
            sequenceElt.addAttribute("statistics", sequence.getStatistics().name(), null);
        }
        if (sequence.getTrace() != null) {
            sequenceElt.addAttribute("trace", sequence.getTrace().name(), null);
        }
        if (sequence.getDescription() != null) {
            sequenceElt.addAttribute("description", sequence.getDescription(), null);
        }
        return sequenceElt.toString();

    }

    private static OMElement serializeMediators(List<Mediator> mediators) {

        if (mediators != null) {
            OMElement sequenceElt = AnonymousSequenceSerializer.serializeAnonymousSequence(mediators);
            return sequenceElt;
        }
        return null;
    }

    private static void handleException(String s) {

        log.log(Level.SEVERE, s);
        throw new InvalidConfigurationException(s);
    }
}
