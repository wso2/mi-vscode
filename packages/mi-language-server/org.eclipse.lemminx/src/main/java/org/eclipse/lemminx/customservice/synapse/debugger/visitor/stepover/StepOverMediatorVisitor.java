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

package org.eclipse.lemminx.customservice.synapse.debugger.visitor.stepover;

import org.eclipse.lemminx.customservice.synapse.debugger.entity.Breakpoint;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.StepOverInfo;
import org.eclipse.lemminx.customservice.synapse.AbstractMediatorVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.VisitorUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.AIAgent;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.AIChat;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.KnowledgeBase;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.SequenceMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Clone.Clone;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Clone.CloneTarget;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCall;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.DbMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Enqueue;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Event;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Transaction;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.cache.Cache;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.cache.CacheOnCacheHit;
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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.ValidateOnFail;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.Foreach;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.Iterate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.ScatterGather;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.Aggregate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.AggregateOnComplete;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Bean;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Class;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Script;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Spring;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.command.PojoCommand;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.ejb.Ejb;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.condRouter.ConditionalRouter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.filter.Filter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.filter.FilterElse;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.filter.FilterThen;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.switchMediator.Switch;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.switchMediator.SwitchCase;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.switchMediator.SwitchDefault;
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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xquery.Xquery;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.Enrich;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault.Makefault;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.PayloadFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite.Rewrite;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.smooks.Smooks;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xslt.Xslt;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.targets.Target;

import java.util.ArrayList;
import java.util.List;

public class StepOverMediatorVisitor extends AbstractMediatorVisitor {

    Breakpoint breakpoint;
    StepOverInfo stepOverInfo;
    boolean isFound;
    boolean done;

    public StepOverMediatorVisitor(Breakpoint breakpoint, StepOverInfo stepOverInfo) {

        this.breakpoint = breakpoint;
        this.stepOverInfo = stepOverInfo;
    }

    void visitSimpleMediator(STNode node) {

        if (isFound) {
            stepOverInfo.add(getBreakpointForNode(node));
            done = true;
        } else {
            if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
                this.isFound = true;
            }
        }
    }

    private void addToNextBreakpoints(Breakpoint breakpoint) {

        if (breakpoint != null) {
            stepOverInfo.add(breakpoint);
        }
    }

    private Breakpoint getFirstMediatorBreakpoint(Sequence sequence) {

        if (sequence != null) {
            return getFirstMediatorBreakpoint(sequence.getMediatorList());
        }
        return null;
    }

    private Breakpoint getFirstMediatorBreakpoint(List<Mediator> mediatorList) {

        if (mediatorList != null && !mediatorList.isEmpty()) {
            return getBreakpointForNode(mediatorList.get(0));
        }
        return null;
    }

    private Breakpoint getBreakpointForNode(STNode node) {

        int line = node.getRange().getStartTagRange().getStart().getLine();
        int column = node.getRange().getStartTagRange().getStart().getCharacter();
        return new Breakpoint(line, column);
    }

    @Override
    protected void visitConnector(Connector node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitPropertyGroup(PropertyGroup node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitJsontransform(Jsontransform node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitMakefault(Makefault node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitSmooks(Smooks node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitHeader(Header node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitDataServiceCall(DataServiceCall node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitTransaction(Transaction node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitDatamapper(Datamapper node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitPojoCommand(PojoCommand node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitPayloadFactory(PayloadFactory node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitEntitlementService(EntitlementService node) {

        if (isFound) {
            stepOverInfo.add(getBreakpointForNode(node));
            done = true;
        } else {
            if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
                Sequence onAccept = node.getOnAccept();
                Sequence onReject = node.getOnReject();
                Sequence advice = node.getAdvice();
                Sequence obligations = node.getObligations();
                if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                    addToNextBreakpoints(getFirstMediatorBreakpoint(onAccept));
                    addToNextBreakpoints(getFirstMediatorBreakpoint(onReject));
                    addToNextBreakpoints(getFirstMediatorBreakpoint(advice));
                    addToNextBreakpoints(getFirstMediatorBreakpoint(obligations));
                    if (stepOverInfo.size() == 4) {
                        done = true;
                    }
                } else {
                    if (VisitorUtils.checkNodeInRange(onReject, breakpoint)) {
                        VisitorUtils.visitMediators(onReject.getMediatorList(), this);
                    } else if (VisitorUtils.checkNodeInRange(onAccept, breakpoint)) {
                        VisitorUtils.visitMediators(onAccept.getMediatorList(), this);
                    } else if (VisitorUtils.checkNodeInRange(advice, breakpoint)) {
                        VisitorUtils.visitMediators(advice.getMediatorList(), this);
                    } else if (VisitorUtils.checkNodeInRange(obligations, breakpoint)) {
                        VisitorUtils.visitMediators(obligations.getMediatorList(), this);
                    }
                }
                if (!done) {
                    isFound = true;
                }
            }
        }
    }

    @Override
    protected void visitThrottle(Throttle node) {

        if (isFound) {
            stepOverInfo.add(getBreakpointForNode(node));
            done = true;
        } else {
            if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
                Sequence onAccept = node.getOnAccept();
                Sequence onReject = node.getOnReject();
                if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                    addToNextBreakpoints(getFirstMediatorBreakpoint(onAccept));
                    addToNextBreakpoints(getFirstMediatorBreakpoint(onReject));
                    if (stepOverInfo.size() == 2) {
                        done = true;
                    }
                } else {
                    if (VisitorUtils.checkNodeInRange(onReject, breakpoint)) {
                        VisitorUtils.visitMediators(onReject.getMediatorList(), this);
                    } else if (VisitorUtils.checkNodeInRange(onAccept, breakpoint)) {
                        VisitorUtils.visitMediators(onAccept.getMediatorList(), this);
                    }
                }
                if (!done) {
                    isFound = true;
                }
            }
        }
    }

    @Override
    protected void visitCache(Cache node) {

        if (isFound) {
            stepOverInfo.add(getBreakpointForNode(node));
            done = true;
        } else {
            if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
                CacheOnCacheHit onCacheHit = node.getOnCacheHit();
                if (onCacheHit != null && onCacheHit.getMediatorList() != null) {
                    if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                        addToNextBreakpoints(getFirstMediatorBreakpoint(onCacheHit.getMediatorList()));
                    } else {
                        if (VisitorUtils.checkNodeInRange(onCacheHit, breakpoint)) {
                            VisitorUtils.visitMediators(onCacheHit.getMediatorList(), this);
                        }
                    }
                }
                if (!done) {
                    isFound = true;
                }
            }
        }
    }

    @Override
    protected void visitEnqueue(Enqueue node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitDbreport(DbMediator node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitDblookup(DbMediator node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitEvent(Event node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitRespond(Respond node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitPublishEvent(PublishEvent node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitSwitch(Switch node) {

        if (isFound) {
            stepOverInfo.add(getBreakpointForNode(node));
            done = true;
        } else {
            if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
                SwitchCase[] cases = node.get_case();
                SwitchDefault default_ = node.get_default();
                if (cases != null || default_ != null) {
                    if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                        addToNextBreakpoints(getFirstMediatorBreakpoint(default_.getMediatorList()));
                        for (SwitchCase switchCase : cases) {
                            addToNextBreakpoints(getFirstMediatorBreakpoint(switchCase.getMediatorList()));
                        }
                        int casesLength = 1;
                        if (cases != null) {
                            casesLength = cases.length + 1;
                        }
                        if (!stepOverInfo.isEmpty() && stepOverInfo.size() == casesLength) {
                            done = true;
                        }
                    } else {
                        for (SwitchCase switchCase : cases) {
                            if (switchCase != null && VisitorUtils.checkNodeInRange(switchCase, breakpoint)) {
                                VisitorUtils.visitMediators(switchCase.getMediatorList(), this);
                            }
                        }
                        if (default_ != null && VisitorUtils.checkNodeInRange(default_, breakpoint)) {
                            VisitorUtils.visitMediators(default_.getMediatorList(), this);
                        }
                    }
                }
                if (!done) {
                    isFound = true;
                }
            }
        }
    }

    @Override
    protected void visitSpring(Spring node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitRule(Rule node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitConditionalRouter(ConditionalRouter node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitLoopback(Loopback node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitStore(Store node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitValidate(Validate node) {

        if (isFound) {
            stepOverInfo.add(getBreakpointForNode(node));
            done = true;
        } else {
            if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
                ValidateOnFail onFail = node.getOnFail();
                if (onFail != null && onFail.getMediatorList() != null) {
                    if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                        addToNextBreakpoints(getFirstMediatorBreakpoint(onFail.getMediatorList()));
                    } else {
                        if (VisitorUtils.checkNodeInRange(onFail, breakpoint)) {
                            VisitorUtils.visitMediators(onFail.getMediatorList(), this);
                        }
                    }
                }
                if (!done) {
                    isFound = true;
                }
            }
        }
    }

    @Override
    protected void visitFilter(Filter node) {

        if (isFound) {
            stepOverInfo.add(getBreakpointForNode(node));
            done = true;
        } else {
            if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
                FilterThen then = node.getThen();
                FilterElse else_ = node.getElse_();
                if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                    List<Mediator> thenMediators = new ArrayList<>();
                    List<Mediator> elseMediators = new ArrayList<>();
                    if (then != null && then.getMediatorList() != null) {
                        thenMediators = then.getMediatorList();
                    }
                    if (else_ != null && else_.getMediatorList() != null) {
                        elseMediators = else_.getMediatorList();
                    }
                    addToNextBreakpoints(getFirstMediatorBreakpoint(thenMediators));
                    addToNextBreakpoints(getFirstMediatorBreakpoint(elseMediators));
                    if (stepOverInfo.size() == 2) {
                        done = true;
                    }
                } else {
                    if (VisitorUtils.checkNodeInRange(then, breakpoint)) {
                        VisitorUtils.visitMediators(then.getMediatorList(), this);
                    } else if (VisitorUtils.checkNodeInRange(else_, breakpoint)) {
                        VisitorUtils.visitMediators(else_.getMediatorList(), this);
                    }
                }
                if (!done) {
                    isFound = true;
                }
            }
        }
    }

    @Override
    protected void visitSend(Send node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitClone(Clone node) {

        if (isFound) {
            stepOverInfo.add(getBreakpointForNode(node));
            done = true;
        } else {
            if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
                CloneTarget[] targets = node.getTarget();
                if (targets != null && targets.length > 0) {
                    if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                        for (CloneTarget target : targets) {
                            addToNextBreakpoints(getFirstMediatorBreakpoint(target.getSequence()));
                        }
                        if (!stepOverInfo.isEmpty() && stepOverInfo.size() == targets.length) {
                            done = true;
                        }
                    } else {
                        for (CloneTarget target : targets) {
                            if (target != null && VisitorUtils.checkNodeInRange(target, breakpoint)) {
                                VisitorUtils.visitMediators(target.getSequence().getMediatorList(), this);
                            }
                        }
                    }
                }
                if (!done) {
                    isFound = true;
                }
            }
        }
    }

    @Override
    protected void visitClass(Class node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitAggregate(Aggregate node) {

        if (isFound) {
            stepOverInfo.add(getBreakpointForNode(node));
            done = true;
        } else {
            AggregateOnComplete onComplete = node.getCorrelateOnOrCompleteConditionOrOnComplete().getOnComplete().get();
            if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
                if (onComplete != null) {
                    if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                        addToNextBreakpoints(getFirstMediatorBreakpoint(onComplete.getMediatorList()));
                    } else {
                        if (VisitorUtils.checkNodeInRange(onComplete, breakpoint)) {
                            VisitorUtils.visitMediators(onComplete.getMediatorList(), this);
                        }
                    }
                }
                if (!done) {
                    isFound = true;
                }
            }
        }
    }

    @Override
    protected void visitLog(Log node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitEjb(Ejb node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitNTLM(Ntlm node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitRewrite(Rewrite node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitCallTemplate(CallTemplate node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitCall(Call node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitIterate(Iterate node) {

        if (isFound) {
            stepOverInfo.add(getBreakpointForNode(node));
            done = true;
        } else {
            Target target = node.getTarget();
            if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
                if (target != null) {
                    if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                        addToNextBreakpoints(getFirstMediatorBreakpoint(target.getSequence()));
                        if (!stepOverInfo.isEmpty()) {
                            done = true;
                        }
                    } else {
                        if (VisitorUtils.checkNodeInRange(target, breakpoint)) {
                            VisitorUtils.visitMediators(target.getSequence().getMediatorList(), this);
                        }
                    }
                }
                if (!done) {
                    isFound = true;
                }
            }
        }
    }

    @Override
    protected void visitProperty(Property node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitVariable(Variable node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitScatterGather(ScatterGather node) {

        if (isFound) {
            stepOverInfo.add(getBreakpointForNode(node));
            done = true;
        } else {
            if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
                CloneTarget[] targets = node.getTargets();
                if (targets != null && targets.length > 0) {
                    if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                        for (CloneTarget target : targets) {
                            addToNextBreakpoints(getFirstMediatorBreakpoint(target.getSequence()));
                        }
                        if (!stepOverInfo.isEmpty() && stepOverInfo.size() == targets.length) {
                            done = true;
                        }
                    } else {
                        for (CloneTarget target : targets) {
                            if (target != null && VisitorUtils.checkNodeInRange(target, breakpoint)) {
                                VisitorUtils.visitMediators(target.getSequence().getMediatorList(), this);
                            }
                        }
                    }
                }
                if (!done) {
                    isFound = true;
                }
            }
        }
    }

    @Override
    protected void visitForeach(Foreach node) {

        if (isFound) {
            stepOverInfo.add(getBreakpointForNode(node));
            done = true;
        } else {
            Sequence sequence = node.getSequence();
            if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
                if (sequence != null) {
                    if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                        addToNextBreakpoints(getFirstMediatorBreakpoint(sequence));
                        if (!stepOverInfo.isEmpty()) {
                            done = true;
                        }
                    } else {
                        if (VisitorUtils.checkNodeInRange(sequence, breakpoint)) {
                            VisitorUtils.visitMediators(sequence.getMediatorList(), this);
                        }
                    }
                }
                if (!done) {
                    isFound = true;
                }
            }
        }
    }

    @Override
    protected void visitEnrich(Enrich node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitScript(Script node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitBean(Bean node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitXquery(Xquery node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitBuilder(Builder node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitDrop(Drop node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitXslt(Xslt node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitBam(Bam node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitFastXSLT(FastXSLT node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitOauthService(OauthService node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitCallout(Callout node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitSequence(SequenceMediator node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitThrowError(ThrowError node) {
        visitSimpleMediator(node);
    }

    @Override
    protected void visitAIChat(AIChat node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitAIAgent(AIAgent node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitAIKnowledgeBase(KnowledgeBase node) {

        visitSimpleMediator(node);
    }

    public boolean isDone() {

        return done;
    }
}
