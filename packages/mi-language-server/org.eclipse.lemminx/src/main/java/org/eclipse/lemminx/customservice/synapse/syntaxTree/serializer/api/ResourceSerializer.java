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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.api;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.AnonymousSequenceSerializer;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

public class ResourceSerializer {

    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    public static OMElement serializeResource(APIResource resource) {

        OMElement resourceElt = fac.createOMElement("resource", Constant.SYNAPSE_OMNAMESPACE);
        String[] methods = resource.getMethods();
        if (methods.length > 0) {
            String value = "";
            for (String method : methods) {
                value += method + " ";
            }
            resourceElt.addAttribute("methods", value.trim(), null);
        }

        if (resource.getProtocol() != null && resource.getProtocol().length == 1) {
            if ("http".equalsIgnoreCase(resource.getProtocol()[0])) {
                resourceElt.addAttribute("protocol", "http", null);
            } else if ("https".equalsIgnoreCase(resource.getProtocol()[0])) {
                resourceElt.addAttribute("protocol", "https", null);
            }
        }

        if (resource.getUriTemplate() != null) {
            resourceElt.addAttribute("uri-template", resource.getUriTemplate(), null);
        } else if (resource.getUrlMapping() != null) {
            resourceElt.addAttribute("url-mapping", resource.getUrlMapping(), null);
        }

        if (resource.getInSequenceAttribute() != null) {
            resourceElt.addAttribute("inSequence", resource.getInSequenceAttribute(), null);
        } else {
            OMElement inSequenceElt = AnonymousSequenceSerializer.serializeAnonymousSequence(resource.getInSequence());
            inSequenceElt.setLocalName("inSequence");
            resourceElt.addChild(inSequenceElt);
        }

        if (resource.getOutSequenceAttribute() != null) {
            resourceElt.addAttribute("outSequence", resource.getOutSequenceAttribute(), null);
        } else if (resource.getOutSequence() != null) {
            OMElement outSequenceElt = AnonymousSequenceSerializer.serializeAnonymousSequence(resource.getOutSequence());
            outSequenceElt.setLocalName("outSequence");
            resourceElt.addChild(outSequenceElt);
        }

        if (resource.getFaultSequenceAttribute() != null) {
            resourceElt.addAttribute("faultSequence", resource.getFaultSequenceAttribute(), null);
        } else {
            OMElement faultSequenceElt =
                    AnonymousSequenceSerializer.serializeAnonymousSequence(resource.getFaultSequence());
            faultSequenceElt.setLocalName("faultSequence");
            resourceElt.addChild(faultSequenceElt);
        }

        return resourceElt;
    }
}
