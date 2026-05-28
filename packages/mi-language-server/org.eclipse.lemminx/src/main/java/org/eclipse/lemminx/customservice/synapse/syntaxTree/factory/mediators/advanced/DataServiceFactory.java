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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCall;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCallOperations;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCallOperationsOperation;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCallOperationsOperationParam;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCallOperationsType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCallSource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCallSourceType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCallTarget;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.TargetType;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class DataServiceFactory extends AbstractMediatorFactory {

    private static final String DATA_SERVICE_MEDIATOR = "dataservicecall";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        DataServiceCall dataServiceCall = new DataServiceCall();
        dataServiceCall.elementNode(element);
        populateAttributes(dataServiceCall, element);
        List<DOMNode> children = element.getChildren();
        for (DOMNode node : children) {
            if (node.getNodeName().equalsIgnoreCase(Constant.SOURCE)) {
                DataServiceCallSource dataServiceCallSource = createDataServiceCallSource((DOMElement) node);
                dataServiceCall.setSource(dataServiceCallSource);
            } else if (node.getNodeName().equalsIgnoreCase(Constant.OPERATIONS)) {
                DataServiceCallOperations dataServiceCallOperations =
                        createDataServiceCallOperations((DOMElement) node);
                dataServiceCall.setOperations(dataServiceCallOperations);
            } else if (node.getNodeName().equalsIgnoreCase(Constant.TARGET)) {
                DataServiceCallTarget dataServiceCallTarget = createDataServiceCallTarget((DOMElement) node);
                dataServiceCall.setTarget(dataServiceCallTarget);
            } else {
                //invalid configuration
            }
        }
        return dataServiceCall;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        DataServiceCall dataServiceCall = (DataServiceCall) node;
        String serviceName = element.getAttribute(Constant.SERVICE_NAME);
        if (serviceName != null) {
            dataServiceCall.setServiceName(serviceName);
        }

        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            dataServiceCall.setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            dataServiceCall.setTraceFilter(traceFilter);
        }
    }

    private DataServiceCallSource createDataServiceCallSource(DOMElement element) {

        DataServiceCallSource dataServiceCallSource = new DataServiceCallSource();
        dataServiceCallSource.elementNode(element);
        String type = element.getAttribute(Constant.TYPE);
        DataServiceCallSourceType typeEnum = Utils.getEnumFromValue(type, DataServiceCallSourceType.class);
        if (typeEnum != null) {
            dataServiceCallSource.setType(typeEnum);
        }
        return dataServiceCallSource;
    }

    private DataServiceCallOperations createDataServiceCallOperations(DOMElement element) {

        DataServiceCallOperations dataServiceCallOperations = new DataServiceCallOperations();
        dataServiceCallOperations.elementNode(element);
        String type = element.getAttribute(Constant.TYPE);
        DataServiceCallOperationsType typeEnum = Utils.getEnumFromValue(type, DataServiceCallOperationsType.class);
        if (typeEnum != null) {
            dataServiceCallOperations.setType(typeEnum);
        }
        List<DOMNode> children = element.getChildren();
        List<DataServiceCallOperationsOperation> operationsOperationList = new ArrayList<>();
        for (DOMNode node : children) {
            if (node.getNodeName().equalsIgnoreCase(Constant.OPERATION)) {
                DataServiceCallOperationsOperation dataServiceCallOperationsOperation =
                        createDataServiceCallOperationsOperation((DOMElement) node);
                operationsOperationList.add(dataServiceCallOperationsOperation);
            } else {
                //invalid configuration
            }
        }
        dataServiceCallOperations.setOperation(operationsOperationList.toArray(new DataServiceCallOperationsOperation[operationsOperationList.size()]));
        return dataServiceCallOperations;
    }

    private DataServiceCallOperationsOperation createDataServiceCallOperationsOperation(DOMElement element) {

        DataServiceCallOperationsOperation dataServiceCallOperationsOperation =
                new DataServiceCallOperationsOperation();
        dataServiceCallOperationsOperation.elementNode(element);
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            dataServiceCallOperationsOperation.setName(name);
        }
        List<DOMNode> children = element.getChildren();
        List<DataServiceCallOperationsOperationParam> dataServiceCallOperationsOperationParamList = new ArrayList<>();
        for (DOMNode node : children) {
            if (node.getNodeName().equalsIgnoreCase(Constant.PARAM)) {
                DataServiceCallOperationsOperationParam dataServiceCallOperationsOperationParam =
                        createDataServiceCallOperationsOperationParam((DOMElement) node);
                dataServiceCallOperationsOperationParamList.add(dataServiceCallOperationsOperationParam);
            } else {
                //invalid configuration
            }
        }
        dataServiceCallOperationsOperation.setParam(dataServiceCallOperationsOperationParamList.toArray(
                new DataServiceCallOperationsOperationParam[dataServiceCallOperationsOperationParamList.size()]));
        return dataServiceCallOperationsOperation;
    }

    private DataServiceCallOperationsOperationParam createDataServiceCallOperationsOperationParam(DOMElement element) {

        DataServiceCallOperationsOperationParam dataServiceCallOperationsOperationParam =
                new DataServiceCallOperationsOperationParam();
        dataServiceCallOperationsOperationParam.elementNode(element);
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            dataServiceCallOperationsOperationParam.setName(name);
        }
        String value = element.getAttribute(Constant.VALUE);
        if (value != null) {
            dataServiceCallOperationsOperationParam.setValue(value);
        }
        String expression = element.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            dataServiceCallOperationsOperationParam.setExpression(expression);
        }
        String evaluator = element.getAttribute(Constant.EVALUATOR);
        if (evaluator != null) {
            dataServiceCallOperationsOperationParam.setEvaluator(evaluator);
        }
        return dataServiceCallOperationsOperationParam;
    }

    private DataServiceCallTarget createDataServiceCallTarget(DOMElement element) {

        DataServiceCallTarget dataServiceCallTarget = new DataServiceCallTarget();
        dataServiceCallTarget.elementNode(element);
        String type = element.getAttribute(Constant.TYPE);
        TargetType typeEnum = Utils.getEnumFromValue(type, TargetType.class);
        if (typeEnum != null) {
            dataServiceCallTarget.setType(typeEnum);
        }
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            dataServiceCallTarget.setName(name);
        }
        return dataServiceCallTarget;
    }

    @Override
    public String getTagName() {

        return DATA_SERVICE_MEDIATOR;
    }
}
