/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.eclipse.lemminx.extensions.synapse.codeactions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lemminx.extensions.synapse.codeactions.AddMissingAttributeCodeAction.AttributeChoice;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Dispatcher for Synapse-specific code actions. Maps diagnostic codes emitted
 * by {@link org.eclipse.lemminx.extensions.synapse.SynapseDiagnosticsParticipant}
 * to their corresponding quick-fix handlers.
 */
public class SynapseCodeActionParticipant implements ICodeActionParticipant {

    private final Map<String, ICodeActionParticipant> handlers;

    public SynapseCodeActionParticipant() {
        handlers = new HashMap<>();
        registerHandlers();
    }

    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions,
                             CancelChecker cancelChecker) {
        cancelChecker.checkCanceled();
        Diagnostic diagnostic = request.getDiagnostic();
        if (diagnostic == null || diagnostic.getCode() == null || !diagnostic.getCode().isLeft()) {
            return;
        }
        ICodeActionParticipant handler = handlers.get(diagnostic.getCode().getLeft());
        if (handler != null) {
            cancelChecker.checkCanceled();
            handler.doCodeAction(request, codeActions, cancelChecker);
        }
    }

    private void registerHandlers() {
        handlers.put("MissingSynapseNamespace",
                new AddMissingAttributeCodeAction(Collections.singletonList(
                        new AttributeChoice("Add Synapse namespace",
                                " xmlns=\"http://ws.apache.org/ns/synapse\""))));

        AddMissingAttributeCodeAction valueOrExpr = new AddMissingAttributeCodeAction(Arrays.asList(
                new AttributeChoice("Add 'value' attribute", " value=\"\""),
                new AttributeChoice("Add 'expression' attribute", " expression=\"${}\"")
        ));
        handlers.put("PropertySetMissingValue", valueOrExpr);
        handlers.put("HeaderSetMissingValue", valueOrExpr);
        handlers.put("WithParamMissingValueOrExpression", valueOrExpr);
        handlers.put("DbParameterMissingValue", valueOrExpr);

        handlers.put("ResourceMissingUriTemplateOrUrlMapping",
                new AddMissingAttributeCodeAction(Arrays.asList(
                        new AttributeChoice("Add 'uri-template' attribute", " uri-template=\"/\""),
                        new AttributeChoice("Add 'url-mapping' attribute", " url-mapping=\"/\""))));

        handlers.put("FilterMissingCondition",
                new AddMissingAttributeCodeAction(Arrays.asList(
                        new AttributeChoice("Add 'xpath' condition", " xpath=\"\""),
                        new AttributeChoice("Add 'source' and 'regex' condition",
                                " source=\"\" regex=\"\""))));

        handlers.put("InboundMissingProtocolOrClass",
                new AddMissingAttributeCodeAction(Arrays.asList(
                        new AttributeChoice("Add protocol='http'", " protocol=\"http\""),
                        new AttributeChoice("Add protocol='https'", " protocol=\"https\""),
                        new AttributeChoice("Add protocol='jms'", " protocol=\"jms\""),
                        new AttributeChoice("Add protocol='kafka'", " protocol=\"kafka\""),
                        new AttributeChoice("Add 'class' attribute", " class=\"\""))));

        handlers.put("EnrichSourceCustomMissingXpath",
                new AddMissingAttributeCodeAction(Collections.singletonList(
                        new AttributeChoice("Add 'xpath' attribute", " xpath=\"\""))));

        handlers.put("EnrichSourcePropertyMissingProperty",
                new AddMissingAttributeCodeAction(Collections.singletonList(
                        new AttributeChoice("Add 'property' attribute", " property=\"\""))));

        handlers.put("TriggerMissingSchedule",
                new AddMissingAttributeCodeAction(Arrays.asList(
                        new AttributeChoice("Add 'interval' attribute", " interval=\"1\""),
                        new AttributeChoice("Add 'cron' attribute", " cron=\"\""),
                        new AttributeChoice("Add 'once' attribute", " once=\"true\""))));

        handlers.put("EnrichSourceInlineMissingContent",
                new InsertChildElementCodeAction(
                        "Add inline content placeholder",
                        "<inline xmlns=\"\"/>"));

        handlers.put("LogCustomMissingProperties",
                new InsertChildElementCodeAction(
                        "Add <property> child element",
                        "<property name=\"\" value=\"\"/>"));

        handlers.put("UnreachableCode",
                new RemoveElementCodeAction("Remove unreachable mediator", true));

        handlers.put("DuplicateSwitchCase",
                new RemoveElementCodeAction("Remove duplicate case", false));

        handlers.put("UndefinedVariable", new InsertVariableDefinitionCodeAction());
    }
}
