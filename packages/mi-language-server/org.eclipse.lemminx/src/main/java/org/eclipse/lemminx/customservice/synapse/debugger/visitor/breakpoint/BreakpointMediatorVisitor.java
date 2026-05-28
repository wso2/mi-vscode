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

package org.eclipse.lemminx.customservice.synapse.debugger.visitor.breakpoint;

import org.eclipse.lemminx.customservice.synapse.debugger.entity.Breakpoint;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.DebugInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.IDebugInfo;
import org.eclipse.lemminx.customservice.synapse.AbstractMediatorVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.VisitorUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.AIAgent;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.AIChat;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.KnowledgeBase;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.SequenceMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Clone.Clone;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Clone.CloneTarget;
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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.switchMediator.SwitchCase;
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

import java.util.List;

public class BreakpointMediatorVisitor extends AbstractMediatorVisitor {

    List<Breakpoint> breakpoints;
    Breakpoint breakpoint;
    IDebugInfo debugInfo;
    String mediatorPosition;
    int mediatorCount;
    boolean done;

    public BreakpointMediatorVisitor(List<Breakpoint> breakpoints, IDebugInfo debugInfo) {

        this.breakpoints = breakpoints;
        this.breakpoint = breakpoints.get(0);
        this.debugInfo = debugInfo;
        this.mediatorCount = 0;
        this.done = false;
    }

    public BreakpointMediatorVisitor(Breakpoint breakpoint) {

        this.breakpoint = breakpoint;
        this.debugInfo = new DebugInfo();
        this.mediatorCount = 0;
    }

    public boolean isDone() {

        return done;
    }

    public void nextBreakpoint() {

        breakpoints.remove(breakpoint);
        if (breakpoints.size() > 0) {
            this.done = false;
            breakpoint = breakpoints.get(0);
            resetDebugInfo();
        }
    }

    private void resetDebugInfo() {

        debugInfo.setMediatorPosition(null);
        debugInfo.setValid(true);
        debugInfo.setError(null);
    }

    public List<Breakpoint> getBreakpoints() {

        return breakpoints;
    }

    public Breakpoint getBreakpoint() {

        return breakpoint;
    }

    public IDebugInfo getDebugInfo() {

        return debugInfo;
    }

    void visitSimpleMediator(STNode node) {

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            this.done = true;
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                mediatorPosition = Integer.toString(mediatorCount);
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                markAsInvalid("Breakpoint is not in the starting tag of the mediator:" + node.getTag());
            }
        } else {
            mediatorCount += 1;
        }
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

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            mediatorPosition = Integer.toString(mediatorCount);
            this.done = true;
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                BreakpointMediatorVisitor visitor = new BreakpointMediatorVisitor(breakpoint);
                if (VisitorUtils.checkNodeInRange(node.getOnReject(), breakpoint)) {
                    mediatorPosition += " " + "0";
                    VisitorUtils.visitMediators(node.getOnReject().getMediatorList(), visitor);
                } else if (VisitorUtils.checkNodeInRange(node.getOnAccept(), breakpoint)) {
                    mediatorPosition += " " + "1";
                    VisitorUtils.visitMediators(node.getOnAccept().getMediatorList(), visitor);
                } else if (VisitorUtils.checkNodeInRange(node.getObligations(), breakpoint)) {
                    mediatorPosition += " " + "2";
                    VisitorUtils.visitMediators(node.getObligations().getMediatorList(), visitor);
                } else if (VisitorUtils.checkNodeInRange(node.getAdvice(), breakpoint)) {
                    mediatorPosition += " " + "3";
                    VisitorUtils.visitMediators(node.getAdvice().getMediatorList(), visitor);
                }
                if (visitor.mediatorPosition != null) {
                    mediatorPosition += " " + visitor.mediatorPosition;
                    debugInfo.setMediatorPosition(mediatorPosition);
                } else {
                    markAsInvalid("Invalid breakpoint in Entitlement Service");
                }
            }
        } else {
            mediatorCount += 1;
        }
    }

    @Override
    protected void visitThrottle(Throttle node) {

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            mediatorPosition = Integer.toString(mediatorCount);
            this.done = true;
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                BreakpointMediatorVisitor visitor = new BreakpointMediatorVisitor(breakpoint);
                if (VisitorUtils.checkNodeInRange(node.getOnReject(), breakpoint)) {
                    mediatorPosition += " " + "0";
                    VisitorUtils.visitMediators(node.getOnReject().getMediatorList(), visitor);
                } else if (VisitorUtils.checkNodeInRange(node.getOnAccept(), breakpoint)) {
                    mediatorPosition += " " + "1";
                    VisitorUtils.visitMediators(node.getOnAccept().getMediatorList(), visitor);
                }
                if (visitor.mediatorPosition != null) {
                    mediatorPosition += " " + visitor.mediatorPosition;
                    debugInfo.setMediatorPosition(mediatorPosition);
                } else {
                    markAsInvalid("Invalid breakpoint in Throttle Mediator");
                }
            }
        } else {
            mediatorCount += 1;
        }
    }

    @Override
    protected void visitCache(Cache node) {

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            mediatorPosition = Integer.toString(mediatorCount);
            this.done = true;
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                BreakpointMediatorVisitor visitor = new BreakpointMediatorVisitor(breakpoint);
                if (VisitorUtils.checkNodeInRange(node.getOnCacheHit(), breakpoint)) {
                    VisitorUtils.visitMediators(node.getOnCacheHit().getMediatorList(), visitor);
                }
                if (visitor.mediatorPosition != null) {
                    mediatorPosition += " " + visitor.mediatorPosition;
                    debugInfo.setMediatorPosition(mediatorPosition);
                } else {
                    markAsInvalid("Invalid breakpoint in Cache Mediator");
                }
            }
        } else {
            mediatorCount += 1;
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

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            mediatorPosition = Integer.toString(mediatorCount);
            this.done = true;
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                BreakpointMediatorVisitor visitor = new BreakpointMediatorVisitor(breakpoint);
                if (VisitorUtils.checkNodeInRange(node.get_default(), breakpoint)) {
                    mediatorPosition += " " + "0";
                    VisitorUtils.visitMediators(node.get_default().getMediatorList(), visitor);
                } else {
                    SwitchCase[] cases = node.get_case();
                    if (cases != null) {
                        for (int i = 0; i < cases.length; i++) {
                            if (VisitorUtils.checkNodeInRange(cases[i], breakpoint)) {
                                mediatorPosition += " " + (i + 1);
                                VisitorUtils.visitMediators(cases[i].getMediatorList(), visitor);
                                break;
                            }
                        }
                    }
                }
                if (visitor.mediatorPosition != null) {
                    mediatorPosition += " " + visitor.mediatorPosition;
                    debugInfo.setMediatorPosition(mediatorPosition);
                } else {
                    markAsInvalid("Invalid breakpoint in Switch Mediator");
                }
            }

        } else {
            mediatorCount += 1;
        }
    }

    @Override
    protected void visitSpring(Spring node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitRule(Rule node) {

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            this.done = true;
            mediatorPosition = Integer.toString(mediatorCount);
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                BreakpointMediatorVisitor visitor = new BreakpointMediatorVisitor(breakpoint);
                if (VisitorUtils.checkNodeInRange(node.getChildMediators(), breakpoint)) {
                    VisitorUtils.visitMediators(node.getChildMediators().getMediatorList(), visitor);
                }
                if (visitor.mediatorPosition != null) {
                    mediatorPosition += " " + visitor.mediatorPosition;
                    debugInfo.setMediatorPosition(mediatorPosition);
                } else {
                    markAsInvalid("Invalid breakpoint in Rule Mediator");
                }
            }
        } else {
            mediatorCount += 1;
        }
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

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            this.done = true;
            mediatorPosition = Integer.toString(mediatorCount);
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                BreakpointMediatorVisitor visitor = new BreakpointMediatorVisitor(breakpoint);
                if (VisitorUtils.checkNodeInRange(node.getOnFail(), breakpoint)) {
                    VisitorUtils.visitMediators(node.getOnFail().getMediatorList(), visitor);
                }
                if (visitor.mediatorPosition != null) {
                    mediatorPosition += " " + visitor.mediatorPosition;
                    debugInfo.setMediatorPosition(mediatorPosition);
                } else {
                    markAsInvalid("Invalid breakpoint in Validate Mediator");
                }
            }
        } else {
            mediatorCount += 1;
        }
    }

    @Override
    protected void visitFilter(Filter node) {

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            this.done = true;
            mediatorPosition = Integer.toString(mediatorCount);
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                BreakpointMediatorVisitor visitor = new BreakpointMediatorVisitor(breakpoint);
                if (VisitorUtils.checkNodeInRange(node.getThen(), breakpoint)) {
                    mediatorPosition += " " + "1";
                    VisitorUtils.visitMediators(node.getThen().getMediatorList(), visitor);
                } else if (VisitorUtils.checkNodeInRange(node.getElse_(), breakpoint)) {
                    mediatorPosition += " " + "0";
                    VisitorUtils.visitMediators(node.getElse_().getMediatorList(), visitor);
                }
                if (visitor.mediatorPosition != null) {
                    mediatorPosition += " " + visitor.mediatorPosition;
                    debugInfo.setMediatorPosition(mediatorPosition);
                } else {
                    markAsInvalid("Invalid breakpoint in Filter Mediator");
                }
            }
        } else {
            mediatorCount += 1;
        }
    }

    @Override
    protected void visitSend(Send node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitClone(Clone node) {

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            mediatorPosition = Integer.toString(mediatorCount);
            this.done = true;
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                BreakpointMediatorVisitor visitor = new BreakpointMediatorVisitor(breakpoint);
                CloneTarget[] targets = node.getTarget();
                if (targets != null) {
                    for (int i = 0; i < targets.length; i++) {
                        if (VisitorUtils.checkNodeInRange(targets[i], breakpoint)) {
                            mediatorPosition += " " + i;
                            VisitorUtils.visitMediators(targets[i].getSequence().getMediatorList(), visitor);
                            break;
                        }
                    }
                }
                if (visitor.mediatorPosition != null) {
                    mediatorPosition += " " + visitor.mediatorPosition;
                    debugInfo.setMediatorPosition(mediatorPosition);
                } else {
                    markAsInvalid("Invalid breakpoint in Clone Mediator");
                }
            }
        } else {
            mediatorCount += 1;
        }
    }

    @Override
    protected void visitClass(Class node) {

        visitSimpleMediator(node);
    }

    @Override
    protected void visitAggregate(Aggregate node) {

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            this.done = true;
            mediatorPosition = Integer.toString(mediatorCount);
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                BreakpointMediatorVisitor visitor = new BreakpointMediatorVisitor(breakpoint);
                if (VisitorUtils.checkNodeInRange(node.getCorrelateOnOrCompleteConditionOrOnComplete().getOnComplete().get(), breakpoint)) {
                    VisitorUtils.visitMediators(node.getCorrelateOnOrCompleteConditionOrOnComplete().getOnComplete().get().getMediatorList(), visitor);
                }
                if (visitor.mediatorPosition != null) {
                    mediatorPosition += " " + visitor.mediatorPosition;
                    debugInfo.setMediatorPosition(mediatorPosition);
                } else {
                    markAsInvalid("Invalid breakpoint in Aggregate Mediator");
                }
            }
        } else {
            mediatorCount += 1;
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

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            this.done = true;
            mediatorPosition = Integer.toString(mediatorCount);
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                BreakpointMediatorVisitor visitor = new BreakpointMediatorVisitor(breakpoint);
                if (VisitorUtils.checkNodeInRange(node.getTarget(), breakpoint)) {
                    VisitorUtils.visitMediators(node.getTarget().getSequence().getMediatorList(), visitor);
                }
                if (visitor.mediatorPosition != null) {
                    mediatorPosition += " " + visitor.mediatorPosition;
                    debugInfo.setMediatorPosition(mediatorPosition);
                } else {
                    markAsInvalid("Invalid breakpoint in Iterate Mediator");
                }
            }
        } else {
            mediatorCount += 1;
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

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            mediatorPosition = Integer.toString(mediatorCount);
            this.done = true;
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                BreakpointMediatorVisitor visitor = new BreakpointMediatorVisitor(breakpoint);
                CloneTarget[] targets = node.getTargets();
                if (targets != null) {
                    for (int i = 0; i < targets.length; i++) {
                        if (VisitorUtils.checkNodeInRange(targets[i], breakpoint)) {
                            mediatorPosition += " " + i;
                            VisitorUtils.visitMediators(targets[i].getSequence().getMediatorList(), visitor);
                            break;
                        }
                    }
                }
                if (visitor.mediatorPosition != null) {
                    mediatorPosition += " " + visitor.mediatorPosition;
                    debugInfo.setMediatorPosition(mediatorPosition);
                } else {
                    markAsInvalid("Invalid breakpoint in Scatter Gather Mediator");
                }
            }
        } else {
            mediatorCount += 1;
        }
    }

    @Override
    protected void visitForeach(Foreach node) {

        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            this.done = true;
            mediatorPosition = Integer.toString(mediatorCount);
            if (VisitorUtils.checkValidBreakpoint(node, breakpoint)) {
                debugInfo.setMediatorPosition(mediatorPosition);
            } else {
                BreakpointMediatorVisitor visitor = new BreakpointMediatorVisitor(breakpoint);
                if (VisitorUtils.checkNodeInRange(node.getSequence(), breakpoint)) {
                    VisitorUtils.visitMediators(node.getSequence().getMediatorList(), visitor);
                }
                if (visitor.mediatorPosition != null) {
                    mediatorPosition += " " + visitor.mediatorPosition;
                    debugInfo.setMediatorPosition(mediatorPosition);
                } else {
                    markAsInvalid("Invalid breakpoint in Foreach Mediator");
                }
            }
        } else {
            mediatorCount += 1;
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

        visitConnector((Connector) node);
    }

    @Override
    protected void visitAIAgent(AIAgent node) {

        visitConnector((Connector) node);
    }

    @Override
    protected void visitAIKnowledgeBase(KnowledgeBase node) {

        visitConnector((Connector) node);
    }

    private void markAsInvalid(String error) {

        mediatorPosition = null;
        debugInfo.setMediatorPosition(null);
        debugInfo.setValid(false);
        debugInfo.setError(error);
    }
}
