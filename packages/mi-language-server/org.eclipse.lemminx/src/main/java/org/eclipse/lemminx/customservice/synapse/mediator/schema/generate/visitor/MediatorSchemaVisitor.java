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

package org.eclipse.lemminx.customservice.synapse.mediator.schema.generate.visitor;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.AbstractMediatorVisitor;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorAction;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.AIAgent;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ConnectorParameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.AIChat;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.KnowledgeBase;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.SequenceMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Clone.Clone;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCall;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.DbMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Enqueue;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Event;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Transaction;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.cache.Cache;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.CallTemplate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Drop;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Header;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Log;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Loopback;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Property;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.PropertyGroup;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.PropertyScope;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Respond;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Send;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Store;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.ThrowError;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Variable;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.call.Call;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.callout.Callout;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.Validate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.Foreach;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.Iterate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.ScatterGather;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.Aggregate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Bean;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Class;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Script;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Spring;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.command.PojoCommand;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.ejb.Ejb;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.condRouter.ConditionalRouter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.filter.Filter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.switchMediator.Switch;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.Throttle;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.OauthService;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.bam.Bam;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.builder.Builder;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.entitlement.EntitlementService;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.ntlm.Ntlm;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.publishEvent.PublishEvent;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.Rule;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.Datamapper;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.FastXSLT;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.Jsontransform;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.Enrich;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault.Makefault;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.PayloadFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite.Rewrite;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.smooks.Smooks;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xquery.Xquery;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xslt.Xslt;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.targets.Target;
import org.eclipse.lemminx.customservice.synapse.utils.ConfigFinder;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lsp4j.Position;

import java.io.File;
import java.io.IOException;

public class MediatorSchemaVisitor extends AbstractMediatorVisitor {

    private String projectPath;
    private MediatorTryoutInfo info;
    private Position position;

    public MediatorSchemaVisitor(String projectPath, MediatorTryoutInfo info, Position position) {

        this.projectPath = projectPath;
        this.info = info;
        this.position = position;
    }

    //TODO: Finish all the mediators
    @Override
    protected void visitConnector(Connector node) {

        if (node.getConnectorName() == null) {
            return;
        }
        handleResponseVariable(node);
        handleTargetVariable(node);
    }

    @Override
    protected void visitPropertyGroup(PropertyGroup node) {

        Property[] properties = node.getProperty();
        if (properties != null) {
            for (Property property : properties) {
                visitProperty(property);
            }
        }
    }

    @Override
    protected void visitJsontransform(Jsontransform node) {

        // TODO: replace with the json schema
    }

    @Override
    protected void visitMakefault(Makefault node) {

    }

    @Override
    protected void visitSmooks(Smooks node) {

    }

    @Override
    protected void visitHeader(Header node) {

        if (Constant.SET.equalsIgnoreCase(node.getAction())) {
            String name = node.getName();
            String value = node.getValue() != null ? node.getValue() : node.getExpression();
            org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property property =
                    new org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property(name, value);
            info.addOutputHeader(property);
        } else if (Constant.REMOVE.equalsIgnoreCase(node.getAction())) {
            String name = node.getName();
            info.removeOutputHeader(name);
        }
    }

    @Override
    protected void visitDataServiceCall(DataServiceCall node) {

    }

    @Override
    protected void visitTransaction(Transaction node) {

    }

    @Override
    protected void visitDatamapper(Datamapper node) {


        // TODO: replace with the json schema
    }

    @Override
    protected void visitPojoCommand(PojoCommand node) {

    }

    @Override
    protected void visitPayloadFactory(PayloadFactory node) {

        String content = (String) node.getFormat().getContent();
        if (!StringUtils.isEmpty(content)) {
            info.setOutputPayload(new JsonPrimitive(content));
        } else if (node.getFormat().getKey() != null) {
            try {
                String path =
                        ConfigFinder.findEsbComponentPath(node.getFormat().getKey(), Constant.RESOURCE, projectPath);
                String regContent = org.eclipse.lemminx.customservice.synapse.utils.Utils.readFile(new File(path));
                info.setOutputPayload(new JsonPrimitive(regContent));
            } catch (IOException e) {
                info.setOutputPayload(new JsonPrimitive(StringUtils.EMPTY));
            }
        } else {
            info.setOutputPayload(new JsonPrimitive(StringUtils.EMPTY));
        }
    }

    @Override
    protected void visitEntitlementService(EntitlementService node) {

    }

    @Override
    protected void visitThrottle(Throttle node) {

        // Do nothing
    }

    @Override
    protected void visitCache(Cache node) {

        // Do nothing
    }

    @Override
    protected void visitEnqueue(Enqueue node) {

    }

    @Override
    protected void visitDbreport(DbMediator node) {

    }

    @Override
    protected void visitDblookup(DbMediator node) {

    }

    @Override
    protected void visitEvent(Event node) {

    }

    @Override
    protected void visitRespond(Respond node) {

        // Do nothing
    }

    @Override
    protected void visitPublishEvent(PublishEvent node) {

    }

    @Override
    protected void visitSwitch(Switch node) {

    }

    @Override
    protected void visitSpring(Spring node) {

    }

    @Override
    protected void visitRule(Rule node) {

    }

    @Override
    protected void visitConditionalRouter(ConditionalRouter node) {

    }

    @Override
    protected void visitLoopback(Loopback node) {

        // Do nothing
    }

    @Override
    protected void visitStore(Store node) {

    }

    @Override
    protected void visitValidate(Validate node) {

    }

    @Override
    protected void visitFilter(Filter node) {

        if (node.getThen() != null && Utils.checkNodeInRange(node.getThen(), position)) {
            Utils.visitMediators(projectPath, node.getThen().getMediatorList(), info, position);
        } else if (node.getElse_() != null && Utils.checkNodeInRange(node.getElse_(), position)) {
            Utils.visitMediators(projectPath, node.getElse_().getMediatorList(), info, position);
        } else {
            Utils.visitMediators(projectPath, node.getThen().getMediatorList(), info, position);
            Utils.visitMediators(projectPath, node.getElse_().getMediatorList(), info, position);
        }
    }

    @Override
    protected void visitSend(Send node) {

        // The payload can't be determined
        info.setOutputPayload(new JsonPrimitive(StringUtils.EMPTY));
    }

    @Override
    protected void visitClone(Clone node) {

        // Deprecated
    }

    @Override
    protected void visitClass(Class node) {

        // The payload can't be determined
        info.setOutputPayload(new JsonPrimitive(StringUtils.EMPTY));
    }

    @Override
    protected void visitAggregate(Aggregate node) {

    }

    @Override
    protected void visitLog(Log node) {

        // Do nothing
    }

    @Override
    protected void visitEjb(Ejb node) {

    }

    @Override
    protected void visitNTLM(Ntlm node) {

    }

    @Override
    protected void visitRewrite(Rewrite node) {

    }

    @Override
    protected void visitCallTemplate(CallTemplate node) {

        if (node.getTarget() != null) {
            Utils.visitSequenceTemplate(projectPath, node.getTarget(), info, position);
        }
    }

    @Override
    protected void visitCall(Call node) {

        // The payload can't be determined
        info.setOutputPayload(new JsonPrimitive(StringUtils.EMPTY));
    }

    @Override
    protected void visitIterate(Iterate node) {

        Target target = node.getTarget();
        if (target != null) {
            Utils.visitSequence(projectPath, target.getSequence(), info, position, true);
        }
    }

    @Override
    protected void visitProperty(Property node) {

        String propertyName = node.getName();
        PropertyScope scope = node.getScope();
        org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property property =
                new org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property(propertyName,
                        node.getValue() != null ? node.getValue() : node.getExpression());
        if (scope != null) {
            switch (scope) {
                case AXIS2:
                    info.addOutputAxis2Properties(property);
                    break;
                case AXIS2_CLIENT:
                    info.addOutputAxis2ClientProperties(property);
                    break;
                case OPERATION:
                    info.addOutputAxis2OperationProperties(property);
                    break;
                case TRANSPORT:
                    info.addOutputAxis2TransportProperties(property);
                    break;
                default:
                    info.addOutputSynapseProperties(property);
            }
        } else {
            info.addOutputSynapseProperties(property);
        }
    }

    @Override
    protected void visitVariable(Variable node) {

        String action = node.getAction();
        String name = node.getName();
        if (Constant.SET.equalsIgnoreCase(action)) {
            String value = node.getValue() != null ? node.getValue() : node.getExpression();
            info.addOutputVariable(name, value);
        } else if (Constant.REMOVE.equalsIgnoreCase(action)) {
            info.removeOutputVariable(name);
        }
    }

    @Override
    protected void visitScatterGather(ScatterGather node) {

        if (!Utils.checkNodeInRange(node, position) && node.getResultTarget() != null) {
            if (Constant.VARIABLE.equalsIgnoreCase(node.getResultTarget())) {
                info.addOutputVariable(node.getVariableName(), StringUtils.EMPTY);
            } else {
                info.setOutputPayload(new JsonPrimitive(StringUtils.EMPTY));
            }
        }
    }

    @Override
    protected void visitForeach(Foreach node) {

        Sequence sequence = node.getSequence();
        if (sequence != null) {
            String initialPayload = info.getInput().getPayload().getAsString();
            String collectionToIterate = node.getCollection();
            String iterateContent = Utils.getIterateContent(info, collectionToIterate);
            info.setOutputPayload(new JsonPrimitive(iterateContent));
            Utils.visitSequence(projectPath, sequence, info, position, true);
            if (!Utils.checkNodeInRange(node, position)) {
                if (!node.isContinueWithoutAggregation()) {
                    if (node.isUpdateOriginal()) {
                        info.setOutputPayload(new JsonPrimitive(StringUtils.EMPTY));
                    } else {
                        info.addOutputVariable(node.getVariableName(), info.getOutput().getPayload().getAsString());
                        info.setOutputPayload(new JsonPrimitive(initialPayload));
                    }
                }
            }
        }
    }

    @Override
    protected void visitEnrich(Enrich node) {

        // The payload can't be determined
        info.setOutputPayload(new JsonPrimitive(StringUtils.EMPTY));
    }

    @Override
    protected void visitScript(Script node) {

        // The payload can't be determined
        info.setOutputPayload(new JsonPrimitive(StringUtils.EMPTY));
    }

    @Override
    protected void visitBean(Bean node) {

    }

    @Override
    protected void visitXquery(Xquery node) {

    }

    @Override
    protected void visitBuilder(Builder node) {

    }

    @Override
    protected void visitDrop(Drop node) {

        // Do nothing
    }

    @Override
    protected void visitXslt(Xslt node) {

        // The payload can't be determined
        info.setOutputPayload(new JsonPrimitive(StringUtils.EMPTY));
    }

    @Override
    protected void visitBam(Bam node) {

    }

    @Override
    protected void visitFastXSLT(FastXSLT node) {

        // The payload can't be determined
        info.setOutputPayload(new JsonPrimitive(StringUtils.EMPTY));
    }

    @Override
    protected void visitOauthService(OauthService node) {

    }

    @Override
    protected void visitCallout(Callout node) {

        // The payload can't be determined
        info.setOutputPayload(new JsonPrimitive(StringUtils.EMPTY));
    }

    @Override
    protected void visitSequence(SequenceMediator node) {

        if (node.getKey() != null) {
            Utils.visitNamedSequence(projectPath, node.getKey(), info, position);
        } else {
            Utils.visitMediators(projectPath, node.getMediatorList(), info, position);
        }
    }

    @Override
    protected void visitThrowError(ThrowError node) {

    }

    @Override
    protected void visitAIChat(AIChat node) {

        visitConnector(node);
    }

    @Override
    protected void visitAIAgent(AIAgent node) {

        visitConnector(node);
    }

    @Override
    protected void visitAIKnowledgeBase(KnowledgeBase node) {

        visitConnector(node);
    }

    /**
     *  Handle the response variable of the connector. This variable will have payload, headers and attributes.
     *  If overwriteBody is set to true, the payload will not be stored in the variable and only in the body
     */
    private void handleResponseVariable(Connector node) {
        ConnectorParameter responseVariableParameter = node.getParameter(Constant.RESPONSE_VARIABLE);
        if (responseVariableParameter == null || StringUtils.isEmpty(responseVariableParameter.getValue())) {
            return;
        }
        String responseVariable = responseVariableParameter.getValue();
        org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector connector =
                ConnectorHolder.getInstance().getConnector(node.getConnectorName());
        if (connector != null) {
            ConnectorAction action = connector.getAction(node.getMethod());
            if (action != null && action.getOutputSchema() != null) {
                org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property property =
                        action.getOutputSchema();
                property.setKey(responseVariable);
                // if overwriteBody is true, then the payload will not be stored in the variable and only in the body
                ConnectorParameter overwriteMsgInMsgCtxParam = node.getParameter(Constant.OVERWRITE_BODY);
                if (overwriteMsgInMsgCtxParam != null) {
                    String paramValue = overwriteMsgInMsgCtxParam.getValue();
                    if ("true".equalsIgnoreCase(paramValue)) {
                        org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property payload =
                                property.getProperties().stream().filter(p -> p.getKey().equals(Constant.PAYLOAD)).
                                        findFirst().orElse(null);
                        if (payload != null) {
                            JsonObject payloadObj = new JsonObject();
                            Utils.convertToJsonObject(payload, payloadObj);
                            info.setOutputPayload(new JsonPrimitive(payloadObj.toString()));
                        }
                        property.deleteProperty(Constant.PAYLOAD);
                    }
                }
                if (property.getProperties() != null && !property.getProperties().isEmpty()) {
                    info.addOutputVariable(property);
                }
            }
        }
    }

    /**
     *  Handle the target variable of the connector. This is mostly used in modules to store the output in variables.
     */
    private void handleTargetVariable(Connector node) {
        ConnectorParameter targetVariableParameter = node.getParameter(Constant.CONN_TARGET_VARIABLE);
        if (targetVariableParameter == null || StringUtils.isEmpty(targetVariableParameter.getValue())) {
            return;
        }
        String targetVariable = targetVariableParameter.getValue();
        org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector connector =
                ConnectorHolder.getInstance().getConnector(node.getConnectorName());
        if (connector != null) {
            ConnectorAction action = connector.getAction(node.getMethod());
            if (action != null) {
                info.addOutputVariable(targetVariable, StringUtils.EMPTY);
            }
        }
    }
}
