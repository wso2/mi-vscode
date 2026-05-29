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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.endpoint;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.AbstractFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.EndpointSession;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.SessionType;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

public class SessionFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        EndpointSession session = new EndpointSession();
        session.elementNode(element);
        populateAttributes(session, element);
        DOMNode sessionTimeout = element.getFirstChild();
        if (sessionTimeout != null) {
            String timeout = Utils.getInlineString(sessionTimeout.getFirstChild());
            if (timeout != null && !timeout.isEmpty()) {
                session.setSessionTimeout(Utils.parseInt(timeout));
            }
        }
        return session;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String type = element.getAttribute(Constant.TYPE);
        SessionType typeEnum = Utils.getEnumFromValue(type, SessionType.class);
        if (typeEnum != null) {
            ((EndpointSession) node).setType(typeEnum);
        }
    }
}
