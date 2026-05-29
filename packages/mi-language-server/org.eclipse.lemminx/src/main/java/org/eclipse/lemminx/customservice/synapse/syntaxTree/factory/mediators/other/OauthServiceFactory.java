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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.OauthService;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;

public class OauthServiceFactory extends AbstractMediatorFactory {

    private static final String OAUTH = "oauthService";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        OauthService oauthService = new OauthService();
        oauthService.elementNode(element);
        populateAttributes(oauthService, element);
        return oauthService;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String remoteServiceUrl = element.getAttribute(Constant.REMOTE_SERVICE_URL);
        if (remoteServiceUrl != null) {
            ((OauthService) node).setRemoteServiceUrl(remoteServiceUrl);
        }
        String username = element.getAttribute(Constant.USERNAME);
        if (username != null) {
            ((OauthService) node).setUsername(username);
        }
        String password = element.getAttribute(Constant.PASSWORD);
        if (password != null) {
            ((OauthService) node).setPassword(password);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((OauthService) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((OauthService) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return OAUTH;
    }
}
