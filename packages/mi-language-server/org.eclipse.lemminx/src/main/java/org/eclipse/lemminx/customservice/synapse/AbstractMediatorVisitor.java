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

package org.eclipse.lemminx.customservice.synapse;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.AIAgent;
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

public abstract class AbstractMediatorVisitor {

    protected abstract void visitConnector(Connector node);

    protected abstract void visitPropertyGroup(PropertyGroup node);

    protected abstract void visitJsontransform(Jsontransform node);

    protected abstract void visitMakefault(Makefault node);

    protected abstract void visitSmooks(Smooks node);

    protected abstract void visitHeader(Header node);

    protected abstract void visitDataServiceCall(DataServiceCall node);

    protected abstract void visitTransaction(Transaction node);

    protected abstract void visitDatamapper(Datamapper node);

    protected abstract void visitPojoCommand(PojoCommand node);

    protected abstract void visitPayloadFactory(PayloadFactory node);

    protected abstract void visitEntitlementService(EntitlementService node);

    protected abstract void visitThrottle(Throttle node);

    protected abstract void visitCache(Cache node);

    protected abstract void visitEnqueue(Enqueue node);

    protected abstract void visitDbreport(DbMediator node);

    protected abstract void visitDblookup(DbMediator node);

    protected abstract void visitEvent(Event node);

    protected abstract void visitRespond(Respond node);

    protected abstract void visitPublishEvent(PublishEvent node);

    protected abstract void visitSwitch(Switch node);

    protected abstract void visitSpring(Spring node);

    protected abstract void visitRule(Rule node);

    protected abstract void visitConditionalRouter(ConditionalRouter node);

    protected abstract void visitLoopback(Loopback node);

    protected abstract void visitStore(Store node);

    protected abstract void visitValidate(Validate node);

    protected abstract void visitFilter(Filter node);

    protected abstract void visitSend(Send node);

    protected abstract void visitClone(Clone node);

    protected abstract void visitClass(Class node);

    protected abstract void visitAggregate(Aggregate node);

    protected abstract void visitLog(Log node);

    protected abstract void visitEjb(Ejb node);

    protected abstract void visitNTLM(Ntlm node);

    protected abstract void visitRewrite(Rewrite node);

    protected abstract void visitCallTemplate(CallTemplate node);

    protected abstract void visitCall(Call node);

    protected abstract void visitIterate(Iterate node);

    protected abstract void visitProperty(Property node);
    protected abstract void visitVariable(Variable node);
    protected abstract void visitScatterGather(ScatterGather node);

    protected abstract void visitForeach(Foreach node);

    protected abstract void visitEnrich(Enrich node);

    protected abstract void visitScript(Script node);

    protected abstract void visitBean(Bean node);

    protected abstract void visitXquery(Xquery node);

    protected abstract void visitBuilder(Builder node);

    protected abstract void visitDrop(Drop node);

    protected abstract void visitXslt(Xslt node);

    protected abstract void visitBam(Bam node);

    protected abstract void visitFastXSLT(FastXSLT node);

    protected abstract void visitOauthService(OauthService node);

    protected abstract void visitCallout(Callout node);

    protected abstract void visitSequence(SequenceMediator node);

    protected abstract void visitThrowError(ThrowError node);

    protected abstract void visitAIChat(AIChat node);

    protected abstract void visitAIAgent(AIAgent node);

    protected abstract void visitAIKnowledgeBase(KnowledgeBase node);
}
