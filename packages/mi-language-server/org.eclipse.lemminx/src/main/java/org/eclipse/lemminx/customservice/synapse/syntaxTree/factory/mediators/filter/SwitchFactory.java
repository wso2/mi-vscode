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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.filter;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.switchMediator.Switch;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.switchMediator.SwitchCase;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.switchMediator.SwitchDefault;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class SwitchFactory extends AbstractMediatorFactory {

    private static final String SWITCH = "switch";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Switch _switch = new Switch();
        _switch.elementNode(element);
        populateAttributes(_switch, element);
        List<DOMNode> children = element.getChildren();
        List<SwitchCase> caseList = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.CASE)) {
                    SwitchCase switchCase = createSwitchCase(child);
                    caseList.add(switchCase);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.DEFAULT)) {
                    SwitchDefault switchDefault = createSwitchDefault(child);
                    _switch.set_default(switchDefault);
                }
            }
            _switch.set_case(caseList.toArray(new SwitchCase[caseList.size()]));
        }
        return _switch;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String source = element.getAttribute(Constant.SOURCE);
        if (source != null) {
            ((Switch) node).setSource(source);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Switch) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Switch) node).setTraceFilter(traceFilter);
        }
    }

    private SwitchCase createSwitchCase(DOMNode child) {

        SwitchCase switchCase = new SwitchCase();
        switchCase.elementNode((DOMElement) child);
        String regex = child.getAttribute(Constant.REGEX);
        if (regex != null) {
            switchCase.setRegex(regex);
        }
        List<DOMNode> children = child.getChildren();
        if (children != null && !children.isEmpty()) {
            List<Mediator> mediatorList = SyntaxTreeUtils.createMediators(children);
            switchCase.setMediatorList(mediatorList);
        }
        return switchCase;
    }

    private SwitchDefault createSwitchDefault(DOMNode child) {

        SwitchDefault switchDefault = new SwitchDefault();
        switchDefault.elementNode((DOMElement) child);
        List<DOMNode> children = child.getChildren();
        if (children != null && !children.isEmpty()) {
            List<Mediator> mediatorList = SyntaxTreeUtils.createMediators(children);
            switchDefault.setMediatorList(mediatorList);
        }
        return switchDefault;
    }

    @Override
    public String getTagName() {

        return SWITCH;
    }

}
