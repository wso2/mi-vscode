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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.CallTemplate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.WithParam;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class CallTemplateMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        CallTemplate callTemplate = (CallTemplate) m;
        OMElement callTemplateElt = fac.createOMElement("call-template", synNS);

        if (callTemplate.getTarget() != null) {
            callTemplateElt.addAttribute("target", callTemplate.getTarget(), nullNS);
        }

        if (callTemplate.getOnError() != null) {
            callTemplateElt.addAttribute("onError", callTemplate.getOnError(), nullNS);
        }

        WithParam[] withParams = callTemplate.getWithParam();

        serializeParams(withParams, callTemplateElt);

        if (callTemplate.getDescription() != null) {
            callTemplateElt.addAttribute("description", callTemplate.getDescription(), nullNS);
        }
        return callTemplateElt;
    }

    private void serializeParams(WithParam[] withParams, OMElement callTemplateElt) {

        if (withParams != null) {
            for (WithParam withParam : withParams) {
                OMElement withParamElt = fac.createOMElement("with-param", synNS);
                if (withParam.getName() != null) {
                    withParamElt.addAttribute("name", withParam.getName(), nullNS);
                }
                String value = withParam.getValue();
                if (value != null) {
                    withParamElt.addAttribute("value", value, nullNS);
                    if (value.matches("^\\{.*}$")) {
                        SerializerUtils.serializeNamespaces(withParam, withParamElt);
                    }
                }
                callTemplateElt.addChild(withParamElt);
            }
        }
    }

    @Override
    public String getMediatorClassName() {

        return CallTemplate.class.getName();
    }
}
