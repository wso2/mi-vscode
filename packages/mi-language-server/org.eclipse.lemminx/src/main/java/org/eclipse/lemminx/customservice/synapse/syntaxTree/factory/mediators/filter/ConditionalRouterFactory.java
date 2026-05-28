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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.condRouter.ConditionalRouter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.condRouter.ConditionalRouterConditionalRoute;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.condRouter.ConditionalRouterConditionalRouteCondition;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.Equal;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.targets.Target;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class ConditionalRouterFactory extends AbstractMediatorFactory {

    private static final String CONDITIONAL_ROUTER = "conditionalRouter";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        ConditionalRouter conditionalRouter = new ConditionalRouter();
        conditionalRouter.elementNode(element);
        populateAttributes(conditionalRouter, element);
        List<DOMNode> children = element.getChildren();
        List<ConditionalRouterConditionalRoute> routes = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.CONDITIONAL_ROUTE)) {
                    ConditionalRouterConditionalRoute route = createRoute(child);
                    routes.add(route);
                }
            }
            conditionalRouter.setConditionalRoute(routes.toArray(new ConditionalRouterConditionalRoute[routes.size()]));
        }
        return conditionalRouter;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String continueAfter = element.getAttribute(Constant.CONTINUE_AFTER);
        if (continueAfter != null) {
            ((ConditionalRouter) node).setContinueAfter(Boolean.parseBoolean(continueAfter));
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((ConditionalRouter) node).setDescription(description);
        }
    }

    private ConditionalRouterConditionalRoute createRoute(DOMNode child) {

        ConditionalRouterConditionalRoute route = new ConditionalRouterConditionalRoute();
        route.elementNode((DOMElement) child);
        String breakRoute = child.getAttribute(Constant.BREAK_ROUTE);
        if (breakRoute != null) {
            route.setBreakRoute(Boolean.parseBoolean(breakRoute));
        }
        List<DOMNode> children = child.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode childNode : children) {
                if (childNode.getNodeName().equalsIgnoreCase(Constant.CONDITION)) {
                    ConditionalRouterConditionalRouteCondition condition =
                            createConditionalRouterConditionalRouteCondition(childNode);
                    route.setCondition(condition);
                } else if (childNode.getNodeName().equalsIgnoreCase(Constant.TARGET)) {
                    Target target = SyntaxTreeUtils.createTarget(childNode);
                    route.setTarget(target);
                }
            }
        }
        return route;
    }

    private ConditionalRouterConditionalRouteCondition createConditionalRouterConditionalRouteCondition(DOMNode element) {

        ConditionalRouterConditionalRouteCondition condition = new ConditionalRouterConditionalRouteCondition();
        condition.elementNode((DOMElement) element);
        DOMNode child = element.getFirstChild();
        if (child != null) {
            if (child.getNodeName().equalsIgnoreCase(Constant.EQUAL)) {
                Equal equal = SyntaxTreeUtils.createEqual(child);
                condition.setEqual(equal);
            }
        }
        return condition;
    }

    @Override
    public String getTagName() {

        return CONDITIONAL_ROUTER;
    }
}
