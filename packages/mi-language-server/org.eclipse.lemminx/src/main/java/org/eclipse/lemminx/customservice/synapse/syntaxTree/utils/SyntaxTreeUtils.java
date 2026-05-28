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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.utils;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.AbstractFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.endpoint.EndpointFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.MediatorFactoryFinder;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.misc.SequenceFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Parameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.And;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.Equal;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.EvaluatorList;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.Not;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.Or;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.targets.Target;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SyntaxTreeUtils {

    private static MediatorFactoryFinder mediatorFactory = MediatorFactoryFinder.getInstance();

    public static Sequence createSequence(DOMNode node) {

        AbstractFactory sequenceFactory = new SequenceFactory();
        Sequence sequence = (Sequence) sequenceFactory.create((DOMElement) node);
        return sequence;
    }

    public static List<Mediator> createMediators(List<DOMNode> children) {

        List<Mediator> mediatorsList = new ArrayList<>();
        for (DOMNode node : children) {
            Mediator mediator = createMediator(node);
            if (mediator != null) {
                mediatorsList.add(mediator);
            }
        }
        return mediatorsList;
    }

    public static Mediator createMediator(DOMNode node) {

        Mediator mediators = mediatorFactory.getMediator(node);
        return mediators;
    }

    public static Target createTarget(DOMNode node) {

        Target target = new Target();
        target.elementNode((DOMElement) node);
        populateTargetAttributes(target, node);
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equals(Constant.SEQUENCE)) {
                    Sequence sequence = createSequence(child);
                    target.setSequence(sequence);
                } else if (name.equals(Constant.ENDPOINT)) {
                    EndpointFactory endpointFactory = new EndpointFactory();
                    NamedEndpoint namedEndpoint = (NamedEndpoint) endpointFactory.create((DOMElement) child);
                    target.setEndpoint(namedEndpoint);
                }
            }
        }
        return target;
    }

    private static void populateTargetAttributes(Target target, DOMNode node) {

        String sequence = node.getAttribute(Constant.SEQUENCE);
        if (sequence != null) {
            target.setSequenceAttribute(sequence);
        }
        String endpoint = node.getAttribute(Constant.ENDPOINT);
        if (endpoint != null) {
            target.setEndpointAttribute(endpoint);
        }
        String to = node.getAttribute(Constant.TO);
        if (to != null) {
            target.setTo(to);
        }
        String soapAction = node.getAttribute(Constant.SOAP_ACTION);
        if (soapAction != null) {
            target.setSoapAction(soapAction);
        }
    }

    public static MediatorProperty createMediatorProperty(DOMNode element) {

        MediatorProperty property = new MediatorProperty();
        property.elementNode((DOMElement) element);
        String name = element.getAttribute(Constant.NAME);
        if (name != null && !name.isEmpty()) {
            property.setName(name);
        }
        String value = element.getAttribute(Constant.VALUE);
        if (value != null && !value.isEmpty()) {
            property.setValue(value);
        }
        String expression = element.getAttribute(Constant.EXPRESSION);
        if (expression != null && !expression.isEmpty()) {
            property.setExpression(expression);
        }
        List<DOMNode> children = element.getChildren();
        List<String> contents = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String content = Utils.getInlineString(child);
                contents.add(content);
            }
            property.setContent(contents.toArray(new String[contents.size()]));
        }
        return property;
    }

    public static Parameter createParameter(DOMNode child) {

        Parameter parameter = new Parameter();
        parameter.elementNode((DOMElement) child);
        String name = child.getAttribute(Constant.NAME);
        if (name != null && !name.isEmpty()) {
            parameter.setName(name);
        }
        String key = child.getAttribute(Constant.KEY);
        if (key != null && !key.isEmpty()) {
            parameter.setKey(key);
        }
        String locked = child.getAttribute(Constant.LOCKED);
        if (locked != null && !locked.isEmpty()) {
            parameter.setLocked(Boolean.parseBoolean(locked));
        }
        String content = Utils.getInlineString(child.getFirstChild());
        if (content != null && !content.isEmpty()) {
            parameter.setContent(content);
        }
        return parameter;
    }

    public static Not createNot(DOMNode conditionChild) {

        Not not = new Not();
        not.elementNode((DOMElement) conditionChild);
        List<DOMNode> notChildren = conditionChild.getChildren();
        if (notChildren != null && !notChildren.isEmpty()) {
            for (DOMNode notChild : notChildren) {
                String name = notChild.getNodeName();
                if (name.equalsIgnoreCase(Constant.AND)) {
                    And and = createAnd(notChild);
                    not.setAnd(and);
                } else if (name.equalsIgnoreCase(Constant.OR)) {
                    Or or = createOr(notChild);
                    not.setOr(or);
                } else if (name.equalsIgnoreCase(Constant.EQUAL)) {
                    Equal equal = createEqual(notChild);
                    not.setEqual(equal);
                } else if (name.equalsIgnoreCase(Constant.NOT)) {
                    Not notNested = createNot(notChild);
                    not.setNot(notNested);
                }
            }
        }
        return not;
    }

    public static And createAnd(DOMNode conditionChild) {

        And and = new And();
        and.elementNode((DOMElement) conditionChild);
        List<DOMNode> andChildren = conditionChild.getChildren();
        if (andChildren != null && !andChildren.isEmpty()) {
            for (DOMNode andChild : andChildren) {
                String name = andChild.getNodeName();
                if (name.equalsIgnoreCase(Constant.AND)) {
                    And andNested = createAnd(andChild);
                    and.setAnd(andNested);
                } else if (name.equalsIgnoreCase(Constant.OR)) {
                    Or or = createOr(andChild);
                    and.setOr(or);
                } else if (name.equalsIgnoreCase(Constant.EQUAL)) {
                    Equal equal = createEqual(andChild);
                    and.setEqual(equal);
                } else if (name.equalsIgnoreCase(Constant.NOT)) {
                    Not not = createNot(andChild);
                    and.setNot(not);
                }
            }
        }
        return and;
    }

    public static Or createOr(DOMNode conditionChild) {

        Or or = new Or();
        or.elementNode((DOMElement) conditionChild);
        List<DOMNode> orChildren = conditionChild.getChildren();
        if (orChildren != null && !orChildren.isEmpty()) {
            List<EvaluatorList> evaluatorLists = new ArrayList<>();
            for (DOMNode orChild : orChildren) {
                String name = orChild.getNodeName();
                EvaluatorList evaluatorList = new EvaluatorList();
                if (name.equalsIgnoreCase(Constant.AND)) {
                    And and = createAnd(orChild);
                    evaluatorList.setAnd(Optional.of(and));
                    evaluatorLists.add(evaluatorList);
                } else if (name.equalsIgnoreCase(Constant.OR)) {
                    Or orNested = createOr(orChild);
                    evaluatorList.setOr(Optional.of(orNested));
                    evaluatorLists.add(evaluatorList);
                } else if (name.equalsIgnoreCase(Constant.EQUAL)) {
                    Equal equal = createEqual(orChild);
                    evaluatorList.setEqual(Optional.of(equal));
                    evaluatorLists.add(evaluatorList);
                } else if (name.equalsIgnoreCase(Constant.NOT)) {
                    Not not = createNot(orChild);
                    evaluatorList.setNot(Optional.of(not));
                }
            }
            or.setEvaluatorList(evaluatorLists.toArray(new EvaluatorList[evaluatorLists.size()]));
        }
        return or;
    }

    public static Equal createEqual(DOMNode conditionChild) {

        Equal equal = new Equal();
        equal.elementNode((DOMElement) conditionChild);
        String type = conditionChild.getAttribute(Constant.TYPE);
        if (type != null && !type.isEmpty()) {
            equal.setType(type);
        }
        String value = conditionChild.getAttribute(Constant.VALUE);
        if (value != null && !value.isEmpty()) {
            equal.setValue(value);
        }
        String source = conditionChild.getAttribute(Constant.SOURCE);
        if (source != null && !source.isEmpty()) {
            equal.setSource(source);
        }
        return equal;
    }
}
