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

package org.eclipse.lemminx.extensions.synapse;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.extensions.synapse.codeactions.SynapseCodeActionParticipant;
import org.eclipse.lemminx.services.CodeActionRequest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for Synapse-specific quick fixes (code actions).
 */
public class SynapseCodeActionTest {

    private static final String SYNAPSE_NS = "http://ws.apache.org/ns/synapse";
    private final SynapseCodeActionParticipant codeActionParticipant = new SynapseCodeActionParticipant();

    // ---- Helper methods ----

    private List<Diagnostic> diagnose(String xml) {
        TextDocument textDocument = new TextDocument(xml, "test.xml");
        DOMDocument document = DOMParser.getInstance().parse(textDocument, null);
        List<Diagnostic> diagnostics = new ArrayList<>();
        SynapseDiagnosticsParticipant participant = new SynapseDiagnosticsParticipant();
        participant.doDiagnostics(document, diagnostics, null, () -> {
        });
        return diagnostics;
    }

    private List<Diagnostic> diagnosticsWithCode(List<Diagnostic> diagnostics, String code) {
        return diagnostics.stream()
                .filter(d -> d.getCode() != null && code.equals(d.getCode().getLeft()))
                .collect(Collectors.toList());
    }

    private List<CodeAction> getCodeActions(String xml, String diagnosticCode) {
        TextDocument textDocument = new TextDocument(xml, "test.xml");
        DOMDocument document = DOMParser.getInstance().parse(textDocument, null);
        List<Diagnostic> diagnostics = diagnose(xml);
        List<Diagnostic> matching = diagnosticsWithCode(diagnostics, diagnosticCode);
        assertFalse(matching.isEmpty(), "Expected at least one " + diagnosticCode + " diagnostic");

        Diagnostic diagnostic = matching.get(0);
        List<CodeAction> codeActions = new ArrayList<>();
        CodeActionRequest request = new CodeActionRequest(
                diagnostic, diagnostic.getRange(), document, null, new SharedSettings());
        codeActionParticipant.doCodeAction(request, codeActions, () -> {
        });
        return codeActions;
    }

    private String applyFirstAction(String xml, String diagnosticCode) {
        List<CodeAction> actions = getCodeActions(xml, diagnosticCode);
        assertFalse(actions.isEmpty(), "Expected at least one code action");
        return applyAction(xml, actions.get(0));
    }

    private String applyAction(String xml, CodeAction action) {
        WorkspaceEdit edit = action.getEdit();
        assertNotNull(edit, "CodeAction should have an edit");
        List<TextEdit> textEdits = edit.getDocumentChanges().get(0).getLeft().getEdits();
        assertNotNull(textEdits);
        assertFalse(textEdits.isEmpty());

        // Apply edits in reverse order to preserve offsets
        StringBuilder sb = new StringBuilder(xml);
        List<TextEdit> sorted = new ArrayList<>(textEdits);
        sorted.sort((a, b) -> {
            int lineDiff = b.getRange().getStart().getLine() - a.getRange().getStart().getLine();
            if (lineDiff != 0) return lineDiff;
            return b.getRange().getStart().getCharacter() - a.getRange().getStart().getCharacter();
        });

        TextDocument doc = new TextDocument(xml, "test.xml");
        DOMDocument domDoc = DOMParser.getInstance().parse(doc, null);
        for (TextEdit te : sorted) {
            try {
                int startOffset = domDoc.offsetAt(te.getRange().getStart());
                int endOffset = domDoc.offsetAt(te.getRange().getEnd());
                sb.replace(startOffset, endOffset, te.getNewText());
            } catch (Exception e) {
                fail("Failed to apply text edit: " + e.getMessage());
            }
        }
        return sb.toString();
    }

    private List<String> actionTitles(List<CodeAction> actions) {
        return actions.stream().map(CodeAction::getTitle).collect(Collectors.toList());
    }

    private String synapseWrap(String inner) {
        return "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">\n" + inner + "\n</sequence>";
    }

    // ===== #1: MissingSynapseNamespace =====

    @Test
    public void testMissingSynapseNamespaceQuickFix() {
        String xml = "<sequence name=\"test\"><respond/></sequence>";
        String result = applyFirstAction(xml, "MissingSynapseNamespace");
        assertTrue(result.contains("xmlns=\"http://ws.apache.org/ns/synapse\""),
                "Should add Synapse namespace: " + result);
    }

    @Test
    public void testMissingSynapseNamespaceOneAction() {
        String xml = "<sequence name=\"test\"><respond/></sequence>";
        List<CodeAction> actions = getCodeActions(xml, "MissingSynapseNamespace");
        assertEquals(1, actions.size());
        assertEquals("Add Synapse namespace", actions.get(0).getTitle());
    }

    // ===== #2: PropertySetMissingValue =====

    @Test
    public void testPropertySetMissingValueOffersTwoActions() {
        String xml = synapseWrap("<property name=\"status\" scope=\"default\"/>");
        List<CodeAction> actions = getCodeActions(xml, "PropertySetMissingValue");
        assertEquals(2, actions.size());
        List<String> titles = actionTitles(actions);
        assertTrue(titles.contains("Add 'value' attribute"));
        assertTrue(titles.contains("Add 'expression' attribute"));
    }

    @Test
    public void testPropertySetMissingValueAddValue() {
        String xml = synapseWrap("<property name=\"status\" scope=\"default\"/>");
        String result = applyFirstAction(xml, "PropertySetMissingValue");
        assertTrue(result.contains("value=\"\""), "Should add value attribute: " + result);
    }

    // ===== #3: HeaderSetMissingValue =====

    @Test
    public void testHeaderSetMissingValueOffersTwoActions() {
        String xml = synapseWrap("<header name=\"Content-Type\"/>");
        List<CodeAction> actions = getCodeActions(xml, "HeaderSetMissingValue");
        assertEquals(2, actions.size());
    }

    @Test
    public void testHeaderSetMissingValueAddExpression() {
        String xml = synapseWrap("<header name=\"Content-Type\"/>");
        List<CodeAction> actions = getCodeActions(xml, "HeaderSetMissingValue");
        String result = applyAction(xml, actions.get(1)); // expression is second
        assertTrue(result.contains("expression=\"${}\""), "Should add expression attribute: " + result);
    }

    // ===== #4: WithParamMissingValueOrExpression =====

    @Test
    public void testWithParamMissingValueOffersTwoActions() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<call-template target=\"myTemplate\">"
                + "<with-param name=\"endpoint\"/>"
                + "</call-template></sequence>";
        List<CodeAction> actions = getCodeActions(xml, "WithParamMissingValueOrExpression");
        assertEquals(2, actions.size());
    }

    @Test
    public void testWithParamMissingValueAddValue() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<call-template target=\"myTemplate\">"
                + "<with-param name=\"endpoint\"/>"
                + "</call-template></sequence>";
        String result = applyFirstAction(xml, "WithParamMissingValueOrExpression");
        assertTrue(result.contains("value=\"\""), "Should add value attribute: " + result);
    }

    // ===== #5: ResourceMissingUriTemplateOrUrlMapping =====

    @Test
    public void testResourceMissingUriTemplateOffersTwoActions() {
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\"><inSequence><respond/></inSequence></resource>"
                + "</api>";
        List<CodeAction> actions = getCodeActions(xml, "ResourceMissingUriTemplateOrUrlMapping");
        assertEquals(2, actions.size());
        List<String> titles = actionTitles(actions);
        assertTrue(titles.contains("Add 'uri-template' attribute"));
        assertTrue(titles.contains("Add 'url-mapping' attribute"));
    }

    @Test
    public void testResourceMissingUriTemplateAddUriTemplate() {
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\"><inSequence><respond/></inSequence></resource>"
                + "</api>";
        String result = applyFirstAction(xml, "ResourceMissingUriTemplateOrUrlMapping");
        assertTrue(result.contains("uri-template=\"/\""), "Should add uri-template: " + result);
    }

    // ===== #6: FilterMissingCondition =====

    @Test
    public void testFilterMissingConditionOffersTwoActions() {
        String xml = synapseWrap("<filter><then><respond/></then></filter>");
        List<CodeAction> actions = getCodeActions(xml, "FilterMissingCondition");
        assertEquals(2, actions.size());
        List<String> titles = actionTitles(actions);
        assertTrue(titles.contains("Add 'xpath' condition"));
        assertTrue(titles.contains("Add 'source' and 'regex' condition"));
    }

    @Test
    public void testFilterMissingConditionAddSourceRegex() {
        String xml = synapseWrap("<filter><then><respond/></then></filter>");
        List<CodeAction> actions = getCodeActions(xml, "FilterMissingCondition");
        String result = applyAction(xml, actions.get(1));
        assertTrue(result.contains("source=\"\"") && result.contains("regex=\"\""),
                "Should add source and regex: " + result);
    }

    // ===== #7: InboundMissingProtocolOrClass =====

    @Test
    public void testInboundMissingProtocolOffersFiveActions() {
        String xml = "<inboundEndpoint xmlns=\"" + SYNAPSE_NS
                + "\" name=\"testInbound\" sequence=\"main\" onError=\"fault\"/>";
        List<CodeAction> actions = getCodeActions(xml, "InboundMissingProtocolOrClass");
        assertEquals(5, actions.size());
        List<String> titles = actionTitles(actions);
        assertTrue(titles.contains("Add protocol='http'"));
        assertTrue(titles.contains("Add protocol='kafka'"));
        assertTrue(titles.contains("Add 'class' attribute"));
    }

    @Test
    public void testInboundMissingProtocolAddHttp() {
        String xml = "<inboundEndpoint xmlns=\"" + SYNAPSE_NS
                + "\" name=\"testInbound\" sequence=\"main\" onError=\"fault\"/>";
        String result = applyFirstAction(xml, "InboundMissingProtocolOrClass");
        assertTrue(result.contains("protocol=\"http\""), "Should add protocol=http: " + result);
    }

    // ===== #8: EnrichSourceCustomMissingXpath =====

    @Test
    public void testEnrichSourceCustomMissingXpathOneAction() {
        String xml = synapseWrap(
                "<enrich><source type=\"custom\"/><target type=\"body\"/></enrich>");
        List<CodeAction> actions = getCodeActions(xml, "EnrichSourceCustomMissingXpath");
        assertEquals(1, actions.size());
        assertEquals("Add 'xpath' attribute", actions.get(0).getTitle());
    }

    @Test
    public void testEnrichSourceCustomMissingXpathAddXpath() {
        String xml = synapseWrap(
                "<enrich><source type=\"custom\"/><target type=\"body\"/></enrich>");
        String result = applyFirstAction(xml, "EnrichSourceCustomMissingXpath");
        assertTrue(result.contains("xpath=\"\""), "Should add xpath attribute: " + result);
    }

    // ===== #9: EnrichSourcePropertyMissingProperty =====

    @Test
    public void testEnrichSourcePropertyMissingPropertyOneAction() {
        String xml = synapseWrap(
                "<enrich><source type=\"property\"/><target type=\"body\"/></enrich>");
        List<CodeAction> actions = getCodeActions(xml, "EnrichSourcePropertyMissingProperty");
        assertEquals(1, actions.size());
        assertEquals("Add 'property' attribute", actions.get(0).getTitle());
    }

    @Test
    public void testEnrichSourcePropertyMissingPropertyAddProperty() {
        String xml = synapseWrap(
                "<enrich><source type=\"property\"/><target type=\"body\"/></enrich>");
        String result = applyFirstAction(xml, "EnrichSourcePropertyMissingProperty");
        assertTrue(result.contains("property=\"\""), "Should add property attribute: " + result);
    }

    // ===== #10: EnrichSourceInlineMissingContent =====

    @Test
    public void testEnrichSourceInlineMissingContentOneAction() {
        String xml = synapseWrap(
                "<enrich><source type=\"inline\"/><target type=\"body\"/></enrich>");
        List<CodeAction> actions = getCodeActions(xml, "EnrichSourceInlineMissingContent");
        assertEquals(1, actions.size());
        assertEquals("Add inline content placeholder", actions.get(0).getTitle());
    }

    @Test
    public void testEnrichSourceInlineMissingContentSelfClosed() {
        String xml = synapseWrap(
                "<enrich><source type=\"inline\"/><target type=\"body\"/></enrich>");
        String result = applyFirstAction(xml, "EnrichSourceInlineMissingContent");
        assertTrue(result.contains("<source type=\"inline\">"),
                "Should convert self-closed to open tag: " + result);
        assertTrue(result.contains("<inline xmlns=\"\"/>"),
                "Should add inline child: " + result);
        assertTrue(result.contains("</source>"),
                "Should add closing tag: " + result);
    }

    // ===== #11: LogCustomMissingProperties =====

    @Test
    public void testLogCustomMissingPropertiesOneAction() {
        String xml = synapseWrap("<log level=\"custom\"/>");
        List<CodeAction> actions = getCodeActions(xml, "LogCustomMissingProperties");
        assertEquals(1, actions.size());
        assertEquals("Add <property> child element", actions.get(0).getTitle());
    }

    @Test
    public void testLogCustomMissingPropertiesSelfClosed() {
        String xml = synapseWrap("<log level=\"custom\"/>");
        String result = applyFirstAction(xml, "LogCustomMissingProperties");
        assertTrue(result.contains("<log level=\"custom\">"),
                "Should convert to open tag: " + result);
        assertTrue(result.contains("<property name=\"\" value=\"\"/>"),
                "Should add property child: " + result);
        assertTrue(result.contains("</log>"),
                "Should add closing tag: " + result);
    }

    @Test
    public void testLogCustomMissingPropertiesOpenClose() {
        String xml = synapseWrap("<log level=\"custom\"></log>");
        String result = applyFirstAction(xml, "LogCustomMissingProperties");
        assertTrue(result.contains("<property name=\"\" value=\"\"/>"),
                "Should add property child: " + result);
    }

    // ===== #12: UnreachableCode =====

    @Test
    public void testUnreachableCodeOffersTwoActions() {
        String xml = synapseWrap("<respond/>\n<log level=\"full\"/>\n<drop/>");
        List<CodeAction> actions = getCodeActions(xml, "UnreachableCode");
        assertTrue(actions.size() >= 1, "Should offer at least one removal action");
        List<String> titles = actionTitles(actions);
        assertTrue(titles.stream().anyMatch(t -> t.contains("Remove unreachable mediator")));
    }

    @Test
    public void testUnreachableCodeRemoveAllOption() {
        String xml = synapseWrap("<respond/>\n<log level=\"full\"/>\n<drop/>");
        List<CodeAction> actions = getCodeActions(xml, "UnreachableCode");
        List<String> titles = actionTitles(actions);
        assertTrue(titles.contains("Remove all unreachable mediators"),
                "Should offer 'Remove all unreachable mediators' option: " + titles);
    }

    @Test
    public void testUnreachableCodeRemoveSingle() {
        String xml = synapseWrap("<respond/>\n<log level=\"full\"/>");
        String result = applyFirstAction(xml, "UnreachableCode");
        assertFalse(result.contains("<log level=\"full\""),
                "Should remove the unreachable log element: " + result);
        assertTrue(result.contains("<respond/>"),
                "Should keep the respond element: " + result);
    }

    // ===== #13: DuplicateSwitchCase =====

    @Test
    public void testDuplicateSwitchCaseOneAction() {
        String xml = synapseWrap(
                "<switch source=\"${payload.type}\">"
                        + "<case regex=\"A\"><respond/></case>"
                        + "<case regex=\"A\"><drop/></case>"
                        + "</switch>");
        List<CodeAction> actions = getCodeActions(xml, "DuplicateSwitchCase");
        assertEquals(1, actions.size());
        assertTrue(actions.get(0).getTitle().contains("Remove duplicate case"));
    }

    @Test
    public void testDuplicateSwitchCaseRemoveDuplicate() {
        String xml = synapseWrap(
                "<switch source=\"${payload.type}\">"
                        + "<case regex=\"A\"><respond/></case>"
                        + "<case regex=\"A\"><drop/></case>"
                        + "</switch>");
        String result = applyFirstAction(xml, "DuplicateSwitchCase");
        // After removal, only one case regex="A" should remain
        int count = countOccurrences(result, "regex=\"A\"");
        assertEquals(1, count, "Should have only one case regex=\"A\" after removal: " + result);
    }

    // ===== #15: TriggerMissingSchedule =====

    @Test
    public void testTriggerMissingScheduleOffersThreeActions() {
        String xml = "<task xmlns=\"" + SYNAPSE_NS + "\" name=\"TestTask\" class=\"org.example.Task\">"
                + "<trigger/></task>";
        List<CodeAction> actions = getCodeActions(xml, "TriggerMissingSchedule");
        assertEquals(3, actions.size());
        List<String> titles = actionTitles(actions);
        assertTrue(titles.contains("Add 'interval' attribute"));
        assertTrue(titles.contains("Add 'cron' attribute"));
        assertTrue(titles.contains("Add 'once' attribute"));
    }

    @Test
    public void testTriggerMissingScheduleAddInterval() {
        String xml = "<task xmlns=\"" + SYNAPSE_NS + "\" name=\"TestTask\" class=\"org.example.Task\">"
                + "<trigger/></task>";
        String result = applyFirstAction(xml, "TriggerMissingSchedule");
        assertTrue(result.contains("interval=\"1\""), "Should add interval attribute: " + result);
    }

    // ===== #16: DbParameterMissingValue =====

    @Test
    public void testDbParameterMissingValueOffersTwoActions() {
        String xml = synapseWrap(
                "<dblookup>"
                        + "<connection><pool>"
                        + "<driver>com.mysql.jdbc.Driver</driver>"
                        + "<url>jdbc:mysql://localhost/test</url>"
                        + "<user>root</user>"
                        + "<password>pass</password>"
                        + "</pool></connection>"
                        + "<statement>"
                        + "<sql>SELECT * FROM users WHERE id=?</sql>"
                        + "<parameter type=\"INTEGER\"/>"
                        + "<result name=\"userName\" column=\"name\"/>"
                        + "</statement>"
                        + "</dblookup>");
        List<CodeAction> actions = getCodeActions(xml, "DbParameterMissingValue");
        assertEquals(2, actions.size());
        List<String> titles = actionTitles(actions);
        assertTrue(titles.contains("Add 'value' attribute"));
        assertTrue(titles.contains("Add 'expression' attribute"));
    }

    @Test
    public void testDbParameterMissingValueAddValue() {
        String xml = synapseWrap(
                "<dblookup>"
                        + "<connection><pool>"
                        + "<driver>com.mysql.jdbc.Driver</driver>"
                        + "<url>jdbc:mysql://localhost/test</url>"
                        + "<user>root</user>"
                        + "<password>pass</password>"
                        + "</pool></connection>"
                        + "<statement>"
                        + "<sql>SELECT * FROM users WHERE id=?</sql>"
                        + "<parameter type=\"INTEGER\"/>"
                        + "<result name=\"userName\" column=\"name\"/>"
                        + "</statement>"
                        + "</dblookup>");
        String result = applyFirstAction(xml, "DbParameterMissingValue");
        assertTrue(result.contains("value=\"\""), "Should add value attribute: " + result);
    }

    // ===== #14: UndefinedVariable =====

    @Test
    public void testUndefinedVariableOneAction() {
        String xml = synapseWrap("<property name=\"status\" expression=\"${vars.myStatus}\"/>");
        List<CodeAction> actions = getCodeActions(xml, "UndefinedVariable");
        assertEquals(1, actions.size());
        assertEquals("Define variable 'myStatus'", actions.get(0).getTitle());
    }

    @Test
    public void testUndefinedVariableInsertDefinition() {
        String xml = synapseWrap("<property name=\"status\" expression=\"${vars.myStatus}\"/>");
        String result = applyFirstAction(xml, "UndefinedVariable");
        assertTrue(result.contains("<variable name=\"myStatus\" type=\"STRING\" value=\"\"/>"),
                "Should insert variable definition: " + result);
        // The variable definition should appear before the <property> element
        int varIdx = result.indexOf("<variable name=\"myStatus\"");
        int propIdx = result.indexOf("<property name=\"status\"");
        assertTrue(varIdx < propIdx,
                "Variable definition should be before the property element: " + result);
    }

    // ===== Utility =====

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(substring, idx)) != -1) {
            count++;
            idx += substring.length();
        }
        return count;
    }
}
