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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.LocalEntry;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

public class LocalEntryFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        LocalEntry localEntry = new LocalEntry();
        localEntry.elementNode(element);
        populateAttributes(localEntry, element);
        DOMNode inline = element.getFirstChild();
        if (inline != null) {
            String inlineString = Utils.getInlineString(inline, Boolean.FALSE);
            localEntry.setContent(inlineString);
            String subType = getSubType(inline);
            localEntry.setSubType(subType);
        }
        return localEntry;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        LocalEntry localEntry = (LocalEntry) node;
        String key = element.getAttribute(Constant.KEY);
        if (key != null) {
            localEntry.setKey(key);
        }
        String src = element.getAttribute(Constant.SRC);
        if (src != null) {
            localEntry.setSrc(src);
        }
    }

    private String getSubType(DOMNode inline) {

        String nodeName = inline.getNodeName();
        if (nodeName != null) {
            return nodeName.toUpperCase();
        }
        return null;
    }
}
