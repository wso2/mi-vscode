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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.ejb.Ejb;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.ejb.EjbArgs;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.ejb.EjbArgsArg;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class EjbFactory extends AbstractMediatorFactory {

    private static final String EJB = "ejb";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Ejb ejb = new Ejb();
        ejb.elementNode(element);
        populateAttributes(ejb, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.ARGS)) {
                    EjbArgs args = new EjbArgs();
                    args.elementNode((DOMElement) child);
                    List<EjbArgsArg> ejbArgsArgs = createEjbArgsArg((DOMElement) child);
                    args.setArg(ejbArgsArgs.toArray(new EjbArgsArg[ejbArgsArgs.size()]));
                    ejb.setArgs(args);
                }
            }
        }
        return ejb;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String beanstalk = element.getAttribute(Constant.BEANSTALK);
        if (beanstalk != null) {
            ((Ejb) node).setBeanstalk(beanstalk);
        }
        String clazz = element.getAttribute(Constant.CLASS);
        if (clazz != null) {
            ((Ejb) node).setClazz(clazz);
        }
        String sessionId = element.getAttribute(Constant.SESSION_ID);
        if (sessionId != null) {
            ((Ejb) node).setSessionId(sessionId);
        }
        String remove = element.getAttribute(Constant.REMOVE);
        if (remove != null) {
            ((Ejb) node).setRemove(Boolean.parseBoolean(remove));
        }
        String method = element.getAttribute(Constant.METHOD);
        if (method != null) {
            ((Ejb) node).setMethod(method);
        }
        String target = element.getAttribute(Constant.TARGET);
        if (target != null) {
            ((Ejb) node).setTarget(target);
        }
        String jndiName = element.getAttribute(Constant.JNDI_NAME);
        if (jndiName != null) {
            ((Ejb) node).setJndiName(jndiName);
        }
        String id = element.getAttribute(Constant.ID);
        if (id != null) {
            ((Ejb) node).setId(id);
        }
        String stateful = element.getAttribute(Constant.STATEFUL);
        if (stateful != null) {
            ((Ejb) node).setStateful(Boolean.parseBoolean(stateful));
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Ejb) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Ejb) node).setTraceFilter(traceFilter);
        }
    }

    private List<EjbArgsArg> createEjbArgsArg(DOMElement node) {

        List<DOMNode> argsChildren = node.getChildren();
        List<EjbArgsArg> args = new ArrayList<>();
        if (argsChildren != null && !argsChildren.isEmpty()) {
            for (DOMNode argsChild : argsChildren) {
                if (argsChild.getNodeName().equalsIgnoreCase(Constant.ARG)) {
                    EjbArgsArg arg = new EjbArgsArg();
                    arg.elementNode((DOMElement) argsChild);
                    String value = argsChild.getAttribute(Constant.VALUE);
                    if (value != null) {
                        arg.setValue(value);
                    }
                    args.add(arg);
                }
            }
        }
        return args;
    }

    @Override
    public String getTagName() {

        return EJB;
    }
}
