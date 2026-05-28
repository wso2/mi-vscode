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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIHandlers;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIHandlersHandler;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIHandlersHandlerProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIResource;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

public class APISerializer {

    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    public static String serializeAPI(API api) {
        // Serialize API
        OMElement apiElt = fac.createOMElement("api", Constant.SYNAPSE_OMNAMESPACE);
        addAttributes(api, apiElt);

        serializeResources(api, apiElt);
        serializeHandlers(api, apiElt);
        return apiElt.toString();
    }

    private static void addAttributes(API api, OMElement apiElt) {

        apiElt.addAttribute("name", api.getName(), null);
        apiElt.addAttribute("context", api.getContext(), null);
        serializeVersioningStrategy(api, apiElt);

        if (api.getHostname() != null) {
            apiElt.addAttribute("hostname", api.getHostname(), null);
        }
        if (api.getPort() != null && api.getPort() != "-1") {
            apiElt.addAttribute("port", String.valueOf(api.getPort()), null);
        }
        if (api.getPublishSwagger() != null) {
            apiElt.addAttribute("publishSwagger", api.getPublishSwagger(), null);
        }
        if (api.getStatistics() != null) {
            apiElt.addAttribute("statistics", api.getStatistics().name(), null);
        }
        if (api.getTrace() != null) {
            apiElt.addAttribute("trace", api.getTrace().name(), null);
        }
    }

    public static OMElement serializeVersioningStrategy(API api, OMElement apiElement) {

        assert api != null;

        if (api.getVersion() != null && !"".equals(api.getVersion())) {
            apiElement.addAttribute("version", api.getVersion(), null);
        }
        if (api.getVersionType() != null) {
            apiElement.addAttribute("version-type", api.getVersionType().name(), null);
        }
        return apiElement;
    }

    private static void serializeResources(API api, OMElement apiElt) {

        APIResource[] resources = api.getResource();
        if (resources != null) {
            for (APIResource r : resources) {
                OMElement resourceElt = ResourceSerializer.serializeResource(r);
                if (resourceElt != null) {
                    apiElt.addChild(resourceElt);
                }
            }
        }
    }

    private static void serializeHandlers(API api, OMElement apiElt) {

        APIHandlers handlers = api.getHandlers();
        if (handlers != null) {
            OMElement handlersElt = fac.createOMElement("handlers", Constant.SYNAPSE_OMNAMESPACE);
            APIHandlersHandler[] handlerArray = handlers.getHandler();
            if (handlerArray != null) {
                for (APIHandlersHandler handler : handlerArray) {
                    serializeHandler(handler, handlersElt);
                }
            }
        }
    }

    private static void serializeHandler(APIHandlersHandler handler, OMElement handlersElt) {

        OMElement handlerElt = fac.createOMElement("handler", Constant.SYNAPSE_OMNAMESPACE);
        handlerElt.addAttribute("class", handler.getClazz(), null);
        APIHandlersHandlerProperty[] properties = handler.getProperty();
        if (properties != null) {
            for (APIHandlersHandlerProperty property : properties) {
                OMElement propertyElt = fac.createOMElement("property", Constant.SYNAPSE_OMNAMESPACE);
                propertyElt.addAttribute("name", property.getName(), null);
                propertyElt.addAttribute("value", property.getValue(), null);
                handlerElt.addChild(propertyElt);
            }
        }
        handlersElt.addChild(handlerElt);
    }
}
