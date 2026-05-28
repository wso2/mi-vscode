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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.Enrich;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.SourceEnrich;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.TargetEnrich;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class EnrichMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Enrich enrichMediator = (Enrich) m;
        OMElement enrichElt = fac.createOMElement("enrich", synNS);

        if (enrichMediator.getDescription() != null) {
            enrichElt.addAttribute("description", enrichMediator.getDescription(), null);
        }
        serializeSource(enrichMediator.getSource(), enrichElt);
        serializeTarget(enrichMediator.getTarget(), enrichElt);

        return enrichElt;
    }

    private void serializeSource(SourceEnrich source, OMElement enrichElt) {

        if (source != null) {
            OMElement sourceElt = fac.createOMElement("source", synNS);
            if (source.isClone()) {
                sourceElt.addAttribute("clone", "true", null);
            }
            if (source.getType() != null) {
                String type = source.getType().name();
                switch (type) {
                    case "custom":
                        sourceElt.addAttribute("type", "custom", null);
                        SerializerUtils.serializeExpression(source.getXpath(), sourceElt, "xpath", source);
                        break;
                    case "envelope":
                    case "body":
                        sourceElt.addAttribute("type", type, null);
                        break;
                    case "property":
                        sourceElt.addAttribute("type", "property", null);
                        sourceElt.addAttribute("property", source.getProperty(), null);
                        break;
                    case "inline":
                        sourceElt.addAttribute("type", "inline", null);
                        if (source.getContent() != null) {
                            OMElement inlineElt = SerializerUtils.stringToOM(source.getContent().toString());
                            sourceElt.addChild(inlineElt);
                        }

                }
            }
            enrichElt.addChild(sourceElt);
        }
    }

    private void serializeTarget(TargetEnrich target, OMElement enrichElt) {

        if (target != null) {
            OMElement targetElt = fac.createOMElement("target", synNS);
            if (target.getAction() != null) {
                targetElt.addAttribute("action", target.getAction().name(), null);
            }
            if (target.getType() != null || target.getXpath() != null) {
                String type;
                if (target.getType() == null) {
                    type = "custom";
                } else {
                    type = target.getType().name();
                }
                switch (type) {
                    case "property":
                        targetElt.addAttribute("type", "property", null);
                        targetElt.addAttribute("property", target.getProperty(), null);
                        break;
                    case "key":
                        targetElt.addAttribute("type", "key", null);
                    case "custom":
                        SerializerUtils.serializeExpression(target.getXpath(), targetElt, "xpath", target);
                        break;
                    case "envelope":
                    case "body":
                        targetElt.addAttribute("type", type, null);
                        break;
                }

            }
            enrichElt.addChild(targetElt);
        }
    }

    @Override
    public String getMediatorClassName() {

        return Enrich.class.getName();
    }
}
