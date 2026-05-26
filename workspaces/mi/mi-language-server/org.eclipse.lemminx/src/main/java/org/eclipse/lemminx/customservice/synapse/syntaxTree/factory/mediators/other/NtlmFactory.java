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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.ntlm.Ntlm;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;

public class NtlmFactory extends AbstractMediatorFactory {

    private static final String NTLM = "NTLM";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Ntlm ntlm = new Ntlm();
        ntlm.elementNode(element);
        populateAttributes(ntlm, element);
        return ntlm;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String domain = element.getAttribute(Constant.DOMAIN);
        if (domain != null) {
            ((Ntlm) node).setDomain(domain);
        }
        String host = element.getAttribute(Constant.HOST);
        if (host != null) {
            ((Ntlm) node).setHost(host);
        }
        String username = element.getAttribute(Constant.USERNAME);
        if (username != null) {
            ((Ntlm) node).setUsername(username);
        }
        String password = element.getAttribute(Constant.PASSWORD);
        if (password != null) {
            ((Ntlm) node).setPassword(password);
        }
        String ntlmVersion = element.getAttribute(Constant.NTLM_VERSION);
        if (ntlmVersion != null) {
            ((Ntlm) node).setNtlmVersion(ntlmVersion);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Ntlm) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Ntlm) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return NTLM;
    }
}
