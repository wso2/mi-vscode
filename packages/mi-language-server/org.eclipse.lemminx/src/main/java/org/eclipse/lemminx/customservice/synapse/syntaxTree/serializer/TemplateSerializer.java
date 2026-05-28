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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.Template;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.TemplateParameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.endpoint.EndpointSerializer;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TemplateSerializer {

    private static Logger log = Logger.getLogger(TemplateSerializer.class.getName());

    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    public static String serializeTemplate(Template template) {

        OMElement templateElt = fac.createOMElement("template", Constant.SYNAPSE_OMNAMESPACE);

        serializeAttributes(template, templateElt);
        serializeChildren(template, templateElt);

        return templateElt.toString();
    }

    private static void serializeAttributes(Template template, OMElement templateElt) {

        if (template.getName() != null) {
            templateElt.addAttribute("name", template.getName(), null);
        }
        if (template.getOnError() != null) {
            templateElt.addAttribute("onError", template.getOnError(), null);
        }
    }

    private static void serializeChildren(Template template, OMElement templateElt) {

        if (template.getParameter() != null) {
            serializeParameters(template.getParameter(), templateElt);
        }
        if (template.getEndpoint() != null) {
            OMElement endpointElt = EndpointSerializer.serializeEndpoint(template.getEndpoint());
            templateElt.addChild(endpointElt);
        } else if (template.getSequence() != null) {
            OMElement sequenceElt =
                    AnonymousSequenceSerializer.serializeAnonymousSequence(template.getSequence().getMediatorList());
            templateElt.addChild(sequenceElt);
        } else {
            handleException("Template must have either an endpoint or a sequence");
        }

    }

    private static void serializeParameters(TemplateParameter[] parameter, OMElement templateElt) {

        for (TemplateParameter param : parameter) {

            OMElement paramElt = fac.createOMElement("parameter", Constant.SYNAPSE_OMNAMESPACE);
            String paramNSPrefix = param.getParamNamespacePrefix();
            if (paramNSPrefix != null) {
                String uri = param.getNamespaces().get("xmlns:" + paramNSPrefix);
                if (uri != null) {
                    paramElt.setNamespace(fac.createOMNamespace(uri, paramNSPrefix));
                } else {
                    handleException("Namespace:" + paramNSPrefix + " is not defined in the parameter");
                }
            }
            if (param.getName() != null) {
                paramElt.addAttribute("name", param.getName(), null);
            } else {
                handleException("Parameter name is required");
            }
            if (param.getDefaultValue() != null) {
                paramElt.addAttribute("defaultValue", param.getDefaultValue(), null);
            }
            paramElt.addAttribute("isMandatory", String.valueOf(param.isMandatory()), null);
            templateElt.addChild(paramElt);
        }
    }

    private static void handleException(String s) {

        log.log(Level.SEVERE, s);
        throw new InvalidConfigurationException(s);
    }
}
