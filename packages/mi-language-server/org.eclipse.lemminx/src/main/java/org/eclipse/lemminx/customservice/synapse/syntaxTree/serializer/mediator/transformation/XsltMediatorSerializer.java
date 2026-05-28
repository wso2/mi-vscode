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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xslt.Xslt;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xslt.XsltFeature;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xslt.XsltResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class XsltMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Xslt xsltMediator = (Xslt) m;
        OMElement xsltElt = fac.createOMElement("xslt", synNS);

        if (xsltMediator.getKey() != null) {
            xsltElt.addAttribute("key", xsltMediator.getKey(), null);
        } else {
            handleException("Invalid Xslt mediator. " +
                    "Should have a 'key' ");
        }

        if (xsltMediator.getSource() != null) {
            SerializerUtils.serializeExpression(xsltMediator.getSource(), xsltElt, "source", xsltMediator);
        }

        if (xsltMediator.getDescription() != null) {
            xsltElt.addAttribute("description", xsltMediator.getDescription(), null);
        }

        serializeMediatorProperties(xsltElt, xsltMediator.getProperty());
        serializeFeatures(xsltElt, xsltMediator.getFeature());
        serializeResources(xsltElt, xsltMediator.getResource());

        return xsltElt;
    }

    private void serializeFeatures(OMElement xsltElt, XsltFeature[] features) {

        if (features != null) {
            for (XsltFeature feature : features) {
                OMElement featureElt = fac.createOMElement("feature", synNS);
                if (feature.getName() != null) {
                    featureElt.addAttribute("name", feature.getName(), null);
                }
                featureElt.addAttribute("value", String.valueOf(feature.isValue()), null);
                xsltElt.addChild(featureElt);
            }
        }
    }

    private void serializeResources(OMElement xsltElt, XsltResource[] resources) {

        if (resources != null) {
            for (XsltResource resource : resources) {
                OMElement resourceElt = fac.createOMElement("resource", synNS);
                if (resource.getLocation() != null) {
                    resourceElt.addAttribute("location", resource.getLocation(), null);
                }
                if (resource.getKey() != null) {
                    resourceElt.addAttribute("key", resource.getKey(), null);
                }
                xsltElt.addChild(resourceElt);
            }
        }
    }

    @Override
    public String getMediatorClassName() {

        return Xslt.class.getName();
    }
}
