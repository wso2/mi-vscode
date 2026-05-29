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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.extension;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Script;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.ScriptLanguage;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class ScriptFactory extends AbstractMediatorFactory {

    private static final String SCRIPT = "script";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Script script = new Script();
        script.elementNode(element);
        populateAttributes(script, element);
        // TODO: check handling <xs:any> elements
        List<DOMNode> children = element.getChildren();
        List<Object> elements = new ArrayList<>();
        List<String> scriptKeys = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                if (Constant.INCLUDE.equalsIgnoreCase(node.getNodeName())) {
                    String keyValue = node.getAttribute("key");
                    scriptKeys.add(keyValue);
                } else {
                    String xml = Utils.getInlineString(node);
                    elements.add(xml);
                }
            }
            script.setContent(elements.toArray());
            script.setInclude(scriptKeys.toArray(new String[scriptKeys.size()]));
        }
        return script;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String language = element.getAttribute(Constant.LANGUAGE);
        ScriptLanguage languageEnum = Utils.getEnumFromValue(language, ScriptLanguage.class);
        if (languageEnum != null) {
            ((Script) node).setLanguage(languageEnum);
        }
        String key = element.getAttribute(Constant.KEY);
        if (key != null) {
            ((Script) node).setKey(key);
        }
        String function = element.getAttribute(Constant.FUNCTION);
        if (function != null) {
            ((Script) node).setFunction(function);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Script) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Script) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return SCRIPT;
    }
}
