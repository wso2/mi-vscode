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

import org.eclipse.lemminx.SynapseLanguageService;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.NewProjectResourceFinder;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for SynapseDiagnosticsParticipant: semantic validation rules for
 * Synapse XML that XSD cannot express.
 */
public class SynapseDiagnosticsParticipantTest {

    private static final String SYNAPSE_NS = "http://ws.apache.org/ns/synapse";

    /**
     * Parses XML and runs the SynapseDiagnosticsParticipant, returning all diagnostics.
     */
    private List<Diagnostic> diagnose(String xml) {
        TextDocument textDocument = new TextDocument(xml, "test.xml");
        DOMDocument document = DOMParser.getInstance().parse(textDocument, null);
        List<Diagnostic> diagnostics = new ArrayList<>();
        SynapseDiagnosticsParticipant participant = new SynapseDiagnosticsParticipant();
        participant.doDiagnostics(document, diagnostics, null, () -> {});
        return diagnostics;
    }

    private List<Diagnostic> diagnosticsWithCode(List<Diagnostic> diagnostics, String code) {
        return diagnostics.stream()
                .filter(d -> d.getCode() != null && code.equals(d.getCode().getLeft()))
                .collect(Collectors.toList());
    }

    private String synapseWrap(String inner) {
        return "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">" + inner + "</sequence>";
    }

    // ===== API Resource validation =====

    @Test
    public void testAPIResourceMissingBothAttributes() {
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\"><inSequence><respond/></inSequence></resource>"
                + "</api>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ResourceMissingUriTemplateOrUrlMapping");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testAPIResourceWithUriTemplate() {
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\" uri-template=\"/foo\"><inSequence><respond/></inSequence></resource>"
                + "</api>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ResourceMissingUriTemplateOrUrlMapping");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testAPIResourceWithUrlMapping() {
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\" url-mapping=\"/foo\"><inSequence><respond/></inSequence></resource>"
                + "</api>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ResourceMissingUriTemplateOrUrlMapping");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testResourceNotUnderApiSkipped() {
        // <resource> not under <api> should not trigger the check
        String xml = "<proxy xmlns=\"" + SYNAPSE_NS + "\" name=\"TestProxy\">"
                + "<resource methods=\"GET\"/>"
                + "</proxy>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ResourceMissingUriTemplateOrUrlMapping");
        assertTrue(diags.isEmpty());
    }

    // ===== Filter mediator validation =====

    @Test
    public void testFilterMissingCondition() {
        String xml = synapseWrap("<filter><then><respond/></then></filter>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "FilterMissingCondition");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testFilterWithSourceAndRegex() {
        String xml = synapseWrap("<filter source=\"get-property('type')\" regex=\"premium\">"
                + "<then><respond/></then></filter>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "FilterMissingCondition");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testFilterWithXpath() {
        String xml = synapseWrap("<filter xpath=\"//name\">"
                + "<then><respond/></then></filter>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "FilterMissingCondition");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testFilterWithSourceButNoRegex() {
        String xml = synapseWrap("<filter source=\"get-property('type')\">"
                + "<then><respond/></then></filter>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "FilterMissingCondition");
        assertEquals(1, diags.size());
    }

    // ===== Property mediator validation =====

    @Test
    public void testPropertySetMissingValue() {
        String xml = synapseWrap("<property name=\"foo\" scope=\"synapse\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertySetMissingValue");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testPropertySetWithValue() {
        String xml = synapseWrap("<property name=\"foo\" value=\"bar\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertySetMissingValue");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPropertySetWithExpression() {
        String xml = synapseWrap("<property name=\"foo\" expression=\"${payload.x}\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertySetMissingValue");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPropertyRemoveAction() {
        String xml = synapseWrap("<property name=\"foo\" action=\"remove\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertySetMissingValue");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPropertyInsideLogSkipped() {
        String xml = synapseWrap("<log level=\"custom\"><property name=\"msg\" value=\"test\"/></log>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertySetMissingValue");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPropertyWithInlineXmlChild() {
        String xml = synapseWrap("<property name=\"foo\"><inline>content</inline></property>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertySetMissingValue");
        assertTrue(diags.isEmpty());
    }

    // ===== Header mediator validation =====

    @Test
    public void testHeaderSetMissingValue() {
        String xml = synapseWrap("<header name=\"Content-Type\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "HeaderSetMissingValue");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testHeaderSetWithValue() {
        String xml = synapseWrap("<header name=\"Content-Type\" value=\"text/xml\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "HeaderSetMissingValue");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testHeaderRemoveAction() {
        String xml = synapseWrap("<header name=\"Content-Type\" action=\"remove\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "HeaderSetMissingValue");
        assertTrue(diags.isEmpty());
    }

    // ===== Log mediator validation =====

    @Test
    public void testLogCustomMissingProperties() {
        String xml = synapseWrap("<log level=\"custom\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "LogCustomMissingProperties");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testLogCustomWithProperty() {
        String xml = synapseWrap("<log level=\"custom\"><property name=\"msg\" value=\"test\"/></log>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "LogCustomMissingProperties");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testLogNonCustomLevelNoWarning() {
        String xml = synapseWrap("<log level=\"full\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "LogCustomMissingProperties");
        assertTrue(diags.isEmpty());
    }

    // ===== Switch mediator validation =====

    @Test
    public void testSwitchDuplicateCaseRegex() {
        String xml = synapseWrap("<switch source=\"get-property('type')\">"
                + "<case regex=\"foo\"><respond/></case>"
                + "<case regex=\"foo\"><respond/></case>"
                + "</switch>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "DuplicateSwitchCase");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testSwitchUniqueCaseRegex() {
        String xml = synapseWrap("<switch source=\"get-property('type')\">"
                + "<case regex=\"foo\"><respond/></case>"
                + "<case regex=\"bar\"><respond/></case>"
                + "</switch>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "DuplicateSwitchCase");
        assertTrue(diags.isEmpty());
    }

    // ===== Inbound endpoint validation =====

    @Test
    public void testInboundMissingProtocolAndClass() {
        String xml = "<inboundEndpoint xmlns=\"" + SYNAPSE_NS + "\" name=\"test\" sequence=\"main\" suspend=\"false\"/>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InboundMissingProtocolOrClass");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testInboundWithProtocol() {
        String xml = "<inboundEndpoint xmlns=\"" + SYNAPSE_NS
                + "\" name=\"test\" protocol=\"http\" sequence=\"main\" suspend=\"false\"/>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InboundMissingProtocolOrClass");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testInboundWithClass() {
        String xml = "<inboundEndpoint xmlns=\"" + SYNAPSE_NS
                + "\" name=\"test\" class=\"com.example.Custom\" sequence=\"main\" suspend=\"false\"/>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InboundMissingProtocolOrClass");
        assertTrue(diags.isEmpty());
    }

    // ===== Enrich source validation =====

    @Test
    public void testEnrichSourceCustomMissingXpath() {
        String xml = synapseWrap("<enrich><source type=\"custom\"/><target type=\"body\"/></enrich>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "EnrichSourceCustomMissingXpath");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testEnrichSourceCustomWithXpath() {
        String xml = synapseWrap("<enrich><source type=\"custom\" xpath=\"//foo\"/><target type=\"body\"/></enrich>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "EnrichSourceCustomMissingXpath");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testEnrichSourceDefaultTypeIsCustom() {
        // No type attr defaults to "custom", so xpath is required
        String xml = synapseWrap("<enrich><source/><target type=\"body\"/></enrich>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "EnrichSourceCustomMissingXpath");
        assertEquals(1, diags.size());
    }

    @Test
    public void testEnrichSourcePropertyMissingProperty() {
        String xml = synapseWrap("<enrich><source type=\"property\"/><target type=\"body\"/></enrich>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "EnrichSourcePropertyMissingProperty");
        assertEquals(1, diags.size());
    }

    @Test
    public void testEnrichSourceInlineMissingContent() {
        String xml = synapseWrap("<enrich><source type=\"inline\"/><target type=\"body\"/></enrich>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "EnrichSourceInlineMissingContent");
        assertEquals(1, diags.size());
    }

    @Test
    public void testEnrichSourceInlineWithChild() {
        String xml = synapseWrap("<enrich><source type=\"inline\"><foo/></source><target type=\"body\"/></enrich>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "EnrichSourceInlineMissingContent");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testEnrichSourceInlineWithJsonText() {
        // M5 fix: inline with JSON text content should not be flagged
        String xml = synapseWrap(
                "<enrich><source type=\"inline\">{\"key\": \"value\"}</source><target type=\"body\"/></enrich>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "EnrichSourceInlineMissingContent");
        assertTrue(diags.isEmpty(), "Inline source with JSON text content should be valid");
    }

    @Test
    public void testSourceNotInsideEnrichSkipped() {
        // <source> not under <enrich> should not trigger enrich validation
        String xml = synapseWrap("<mediator><source type=\"custom\"/></mediator>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "EnrichSourceCustomMissingXpath");
        assertTrue(diags.isEmpty());
    }

    // ===== Unreachable code detection =====

    @Test
    public void testUnreachableCodeAfterRespond() {
        String xml = synapseWrap("<respond/><log level=\"full\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UnreachableCode");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
        assertTrue(diags.get(0).getMessage().contains("respond"));
    }

    @Test
    public void testUnreachableCodeAfterDrop() {
        String xml = synapseWrap("<drop/><property name=\"x\" value=\"y\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UnreachableCode");
        assertEquals(1, diags.size());
        assertTrue(diags.get(0).getMessage().contains("drop"));
    }

    @Test
    public void testNoUnreachableCodeBeforeTerminal() {
        String xml = synapseWrap("<log level=\"full\"/><respond/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UnreachableCode");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testMultipleUnreachableMediators() {
        String xml = synapseWrap("<respond/><log level=\"full\"/><property name=\"x\" value=\"y\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UnreachableCode");
        assertEquals(2, diags.size(), "Both mediators after respond should be flagged");
    }

    @Test
    public void testUnreachableCodeAfterLoopback() {
        String xml = synapseWrap("<loopback/><log level=\"full\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UnreachableCode");
        assertEquals(1, diags.size());
        assertTrue(diags.get(0).getMessage().contains("loopback"));
    }

    // ===== Missing Synapse namespace =====

    @Test
    public void testMissingSynapseNamespaceWarning() {
        // Synapse root element without xmlns
        String xml = "<api name=\"TestAPI\" context=\"/test\"/>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "MissingSynapseNamespace");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testNonSynapseRootElementNoWarning() {
        // Not a Synapse root element name — should not trigger warning
        String xml = "<beans/>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "MissingSynapseNamespace");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testCorrectNamespaceNoWarning() {
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\"/>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "MissingSynapseNamespace");
        assertTrue(diags.isEmpty());
    }

    // ===== Variable reference validation =====

    @Test
    public void testUndefinedVariableWarning() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<log level=\"custom\"><property name=\"x\" expression=\"${vars.myVar}\"/></log>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UndefinedVariable");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
        assertTrue(diags.get(0).getMessage().contains("myVar"));
    }

    @Test
    public void testDefinedVariableNoWarning() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<variable name=\"myVar\" value=\"test\"/>"
                + "<log level=\"custom\"><property name=\"x\" expression=\"${vars.myVar}\"/></log>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UndefinedVariable");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPropertyScopeDefaultDefinesVariable() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<property name=\"myVar\" scope=\"default\" value=\"x\"/>"
                + "<log level=\"custom\"><property name=\"x\" expression=\"${vars.myVar}\"/></log>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UndefinedVariable");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testVariableRemoveActionDoesNotDefine() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<variable name=\"myVar\" action=\"remove\"/>"
                + "<log level=\"custom\"><property name=\"x\" expression=\"${vars.myVar}\"/></log>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UndefinedVariable");
        assertEquals(1, diags.size(), "Variable with action=remove should not count as defined");
    }

    @Test
    public void testResponseVariableDefinesVariable() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<gmail.sendMail configKey=\"CONN\">"
                + "<to>test@example.com</to>"
                + "<responseVariable>gmailResponse</responseVariable>"
                + "</gmail.sendMail>"
                + "<log level=\"custom\"><property name=\"x\" expression=\"${vars.gmailResponse}\"/></log>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UndefinedVariable");
        assertTrue(diags.isEmpty(), "responseVariable child element should define the variable");
    }

    // ===== Non-Synapse document skipping =====

    @Test
    public void testNonSynapseDocumentSkipped() {
        String xml = "<beans xmlns=\"http://www.springframework.org/schema/beans\"/>";
        List<Diagnostic> diags = diagnose(xml);
        assertTrue(diags.isEmpty(), "Non-Synapse document should produce zero diagnostics");
    }

    @Test
    public void testWrongNamespaceDetected() {
        String xml = "<api xmlns=\"http://example.com/wrong\" name=\"test\" context=\"/test\"/>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "WrongSynapseNamespace");
        assertEquals(1, diags.size(), "Wrong namespace should produce WrongSynapseNamespace warning");
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    // ===== P0-1: with-param value or expression =====

    @Test
    public void testWithParamMissingBothValueAndExpression() {
        String xml = synapseWrap("<call-template target=\"MyTemplate\">"
                + "<with-param name=\"ep\"/>"
                + "</call-template>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "WithParamMissingValueOrExpression");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
        assertTrue(diags.get(0).getMessage().contains("'ep'"));
    }

    @Test
    public void testWithParamWithValue() {
        String xml = synapseWrap("<call-template target=\"MyTemplate\">"
                + "<with-param name=\"ep\" value=\"HospitalEP\"/>"
                + "</call-template>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "WithParamMissingValueOrExpression");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testWithParamWithExpression() {
        String xml = synapseWrap("<call-template target=\"MyTemplate\">"
                + "<with-param name=\"ep\" expression=\"${vars.ep}\"/>"
                + "</call-template>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "WithParamMissingValueOrExpression");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testMultipleWithParamsMixedValidity() {
        String xml = synapseWrap("<call-template target=\"MyTemplate\">"
                + "<with-param name=\"good\" value=\"ok\"/>"
                + "<with-param name=\"bad\"/>"
                + "</call-template>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "WithParamMissingValueOrExpression");
        assertEquals(1, diags.size());
        assertTrue(diags.get(0).getMessage().contains("'bad'"));
    }

    // ===== P0-2: Task trigger scheduling =====

    @Test
    public void testTriggerMissingSchedule() {
        String xml = "<task xmlns=\"" + SYNAPSE_NS + "\" name=\"MyTask\" class=\"com.example.Task\" group=\"synapse.simple.quartz\">"
                + "<trigger/>"
                + "</task>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "TriggerMissingSchedule");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testTriggerWithInterval() {
        String xml = "<task xmlns=\"" + SYNAPSE_NS + "\" name=\"MyTask\" class=\"com.example.Task\" group=\"synapse.simple.quartz\">"
                + "<trigger interval=\"5\"/>"
                + "</task>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "TriggerMissingSchedule");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testTriggerWithCron() {
        String xml = "<task xmlns=\"" + SYNAPSE_NS + "\" name=\"MyTask\" class=\"com.example.Task\" group=\"synapse.simple.quartz\">"
                + "<trigger cron=\"0 0/5 * * * ?\"/>"
                + "</task>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "TriggerMissingSchedule");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testTriggerWithOnce() {
        String xml = "<task xmlns=\"" + SYNAPSE_NS + "\" name=\"MyTask\" class=\"com.example.Task\" group=\"synapse.simple.quartz\">"
                + "<trigger once=\"true\"/>"
                + "</task>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "TriggerMissingSchedule");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testTriggerOutsideTaskNotValidated() {
        // <trigger> not inside <task> should be ignored
        String xml = synapseWrap("<trigger/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "TriggerMissingSchedule");
        assertTrue(diags.isEmpty());
    }

    // ===== P0-5: DB parameter value or expression =====

    @Test
    public void testDbParameterMissingValue() {
        String xml = synapseWrap("<dblookup>"
                + "<connection/>"
                + "<statement>"
                + "<sql>SELECT * FROM users WHERE id = ?</sql>"
                + "<parameter type=\"INTEGER\"/>"
                + "</statement>"
                + "</dblookup>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "DbParameterMissingValue");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testDbParameterWithValue() {
        String xml = synapseWrap("<dblookup>"
                + "<connection/>"
                + "<statement>"
                + "<sql>SELECT * FROM users WHERE id = ?</sql>"
                + "<parameter type=\"INTEGER\" value=\"1\"/>"
                + "</statement>"
                + "</dblookup>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "DbParameterMissingValue");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testDbParameterWithExpression() {
        String xml = synapseWrap("<dblookup>"
                + "<connection/>"
                + "<statement>"
                + "<sql>SELECT * FROM users WHERE id = ?</sql>"
                + "<parameter type=\"INTEGER\" expression=\"${payload.id}\"/>"
                + "</statement>"
                + "</dblookup>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "DbParameterMissingValue");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testDbParameterOutsideStatementNotValidated() {
        // <parameter> under <parameters> (inbound endpoint) should be ignored
        String xml = "<inboundEndpoint xmlns=\"" + SYNAPSE_NS + "\" name=\"test\" protocol=\"http\" sequence=\"testSeq\">"
                + "<parameters>"
                + "<parameter name=\"inbound.http.port\">8085</parameter>"
                + "</parameters>"
                + "</inboundEndpoint>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "DbParameterMissingValue");
        assertTrue(diags.isEmpty());
    }

    // ===== P0-6: PayloadFactory args mismatch =====

    @Test
    public void testPayloadFactoryArgsMismatch() {
        String xml = synapseWrap("<payloadFactory media-type=\"json\">"
                + "<format>{\"name\": \"$1\", \"age\": \"$2\", \"city\": \"$3\"}</format>"
                + "<args>"
                + "<arg expression=\"${payload.name}\"/>"
                + "<arg expression=\"${payload.age}\"/>"
                + "</args>"
                + "</payloadFactory>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PayloadFactoryArgsMismatch");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
        assertTrue(diags.get(0).getMessage().contains("$3"));
        assertTrue(diags.get(0).getMessage().contains("2 arg(s)"));
    }

    @Test
    public void testPayloadFactoryArgsMatch() {
        String xml = synapseWrap("<payloadFactory media-type=\"json\">"
                + "<format>{\"name\": \"$1\", \"age\": \"$2\"}</format>"
                + "<args>"
                + "<arg expression=\"${payload.name}\"/>"
                + "<arg expression=\"${payload.age}\"/>"
                + "</args>"
                + "</payloadFactory>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PayloadFactoryArgsMismatch");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPayloadFactoryNoPlaceholders() {
        String xml = synapseWrap("<payloadFactory media-type=\"json\">"
                + "<format>{\"status\": \"ok\"}</format>"
                + "<args/>"
                + "</payloadFactory>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PayloadFactoryArgsMismatch");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPayloadFactoryFreemarkerSkipped() {
        String xml = synapseWrap("<payloadFactory media-type=\"json\" template-type=\"freemarker\">"
                + "<format>{\"name\": \"$1\"}</format>"
                + "</payloadFactory>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PayloadFactoryArgsMismatch");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPayloadFactoryNoArgsElement() {
        String xml = synapseWrap("<payloadFactory media-type=\"json\">"
                + "<format>{\"name\": \"$1\", \"age\": \"$2\"}</format>"
                + "</payloadFactory>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PayloadFactoryArgsMismatch");
        assertEquals(1, diags.size());
        assertTrue(diags.get(0).getMessage().contains("$2"));
        assertTrue(diags.get(0).getMessage().contains("0 arg(s)"));
    }

    @Test
    public void testPayloadFactoryExtraArgsNoWarning() {
        // Extra args are not a problem — only missing args for used placeholders
        String xml = synapseWrap("<payloadFactory media-type=\"json\">"
                + "<format>{\"name\": \"$1\"}</format>"
                + "<args>"
                + "<arg expression=\"${payload.name}\"/>"
                + "<arg expression=\"${payload.extra}\"/>"
                + "</args>"
                + "</payloadFactory>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PayloadFactoryArgsMismatch");
        assertTrue(diags.isEmpty());
    }

    // ===== P1-11: Enrich target-source compatibility =====

    @Test
    public void testEnrichBodyToBodyChild() {
        String xml = synapseWrap("<enrich>"
                + "<source type=\"body\"/>"
                + "<target type=\"body\" action=\"child\"/>"
                + "</enrich>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "EnrichCircularBodyReference");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testEnrichBodyToBodyReplace() {
        // Replace (default action) is valid — only child/sibling is circular
        String xml = synapseWrap("<enrich>"
                + "<source type=\"body\"/>"
                + "<target type=\"body\"/>"
                + "</enrich>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "EnrichCircularBodyReference");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testEnrichEnvelopeToProperty() {
        String xml = synapseWrap("<enrich>"
                + "<source type=\"envelope\"/>"
                + "<target type=\"property\" property=\"myProp\"/>"
                + "</enrich>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "EnrichIncompatibleSourceTarget");
        assertEquals(1, diags.size());
    }

    @Test
    public void testEnrichValidCombination() {
        String xml = synapseWrap("<enrich>"
                + "<source type=\"custom\" xpath=\"//result\"/>"
                + "<target type=\"body\"/>"
                + "</enrich>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "EnrichCircularBodyReference");
        assertTrue(diags.isEmpty());
        diags = diagnosticsWithCode(diagnose(xml), "EnrichIncompatibleSourceTarget");
        assertTrue(diags.isEmpty());
    }

    // ===== P1-12: Throttle mediator needs policy =====

    @Test
    public void testThrottleMissingPolicy() {
        String xml = synapseWrap("<throttle id=\"my-throttle\">"
                + "<onAccept><log/></onAccept>"
                + "<onReject><drop/></onReject>"
                + "</throttle>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ThrottleMissingPolicy");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testThrottleWithPolicyKey() {
        String xml = synapseWrap("<throttle id=\"my-throttle\">"
                + "<policy key=\"conf:/throttle-policy.xml\"/>"
                + "<onAccept><log/></onAccept>"
                + "<onReject><drop/></onReject>"
                + "</throttle>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ThrottleMissingPolicy");
        assertTrue(diags.isEmpty());
    }

    // ===== P1-13: Clone/iterate target must have sequence or endpoint =====

    @Test
    public void testCloneTargetEmpty() {
        String xml = synapseWrap("<clone>"
                + "<target to=\"http://example.com\"/>"
                + "</clone>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "CloneIterateTargetEmpty");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testCloneTargetWithSequenceAttr() {
        String xml = synapseWrap("<clone>"
                + "<target sequence=\"mySeq\"/>"
                + "</clone>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "CloneIterateTargetEmpty");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testCloneTargetWithInlineSequence() {
        String xml = synapseWrap("<clone>"
                + "<target><sequence><log/></sequence></target>"
                + "</clone>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "CloneIterateTargetEmpty");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testIterateTargetEmpty() {
        String xml = synapseWrap("<iterate expression=\"//items/item\">"
                + "<target/>"
                + "</iterate>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "CloneIterateTargetEmpty");
        assertEquals(1, diags.size());
    }

    @Test
    public void testTargetOutsideCloneNotValidated() {
        // <target> inside <proxy> should not be validated
        String xml = "<proxy xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<target><inSequence><respond/></inSequence></target>"
                + "</proxy>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "CloneIterateTargetEmpty");
        assertTrue(diags.isEmpty());
    }

    // ===== P1-17: Script mediator must have key or content =====

    @Test
    public void testScriptMissingContent() {
        String xml = synapseWrap("<script language=\"js\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ScriptMissingContent");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testScriptWithKey() {
        String xml = synapseWrap("<script language=\"js\" key=\"conf:/scripts/transform.js\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ScriptMissingContent");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testScriptWithInlineContent() {
        String xml = synapseWrap("<script language=\"js\">mc.setPayloadJSON({});</script>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ScriptMissingContent");
        assertTrue(diags.isEmpty());
    }

    // ===== P2-24: Unreachable code in onAccept/onReject =====

    @Test
    public void testUnreachableCodeInOnAccept() {
        String xml = synapseWrap(
                "<throttle id=\"t\"><policy key=\"gov:throttle\"/>" +
                "<onAccept><respond/><log level=\"full\"/></onAccept></throttle>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UnreachableCode");
        assertEquals(1, diags.size());
        assertTrue(diags.get(0).getMessage().contains("log"));
    }

    @Test
    public void testUnreachableCodeInOnReject() {
        String xml = synapseWrap(
                "<throttle id=\"t\"><policy key=\"gov:throttle\"/>" +
                "<onReject><drop/><log level=\"full\"/></onReject></throttle>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UnreachableCode");
        assertEquals(1, diags.size());
        assertTrue(diags.get(0).getMessage().contains("log"));
    }

    @Test
    public void testReachableCodeInOnAccept() {
        String xml = synapseWrap(
                "<throttle id=\"t\"><policy key=\"gov:throttle\"/>" +
                "<onAccept><log level=\"full\"/><respond/></onAccept></throttle>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "UnreachableCode");
        assertTrue(diags.isEmpty());
    }

    // ===== P2-23: Class mediator FQN validation =====

    @Test
    public void testClassMediatorInvalidFQN_NoPackage() {
        String xml = synapseWrap("<class name=\"MyMediator\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidClassFQN");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testClassMediatorValidFQN() {
        String xml = synapseWrap("<class name=\"com.example.MyMediator\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidClassFQN");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testClassMediatorNumberStartSegment() {
        String xml = synapseWrap("<class name=\"com.123invalid.Foo\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidClassFQN");
        assertEquals(1, diags.size());
    }

    @Test
    public void testClassMediatorExpressionSkipped() {
        String xml = synapseWrap("<class name=\"${payload.className}\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidClassFQN");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testClassMediatorDeepPackage() {
        String xml = synapseWrap("<class name=\"org.wso2.mi.custom.mediator.Transform\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidClassFQN");
        assertTrue(diags.isEmpty());
    }

    // ===== P2-20: Bean mediator conditional required attributes =====

    @Test
    public void testBeanCreateMissingClass() {
        String xml = synapseWrap("<bean action=\"CREATE\" var=\"x\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "BeanCreateMissingClass");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testBeanCreateWithClass() {
        String xml = synapseWrap("<bean action=\"CREATE\" var=\"x\" class=\"com.example.Foo\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "BeanCreateMissingClass");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testBeanSetPropertyMissingProperty() {
        String xml = synapseWrap("<bean action=\"SET_PROPERTY\" var=\"x\" value=\"123\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "BeanPropertyActionMissingProperty");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testBeanSetPropertyMissingValue() {
        String xml = synapseWrap("<bean action=\"SET_PROPERTY\" var=\"x\" property=\"name\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "BeanSetPropertyMissingValue");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testBeanSetPropertyComplete() {
        String xml = synapseWrap("<bean action=\"SET_PROPERTY\" var=\"x\" property=\"name\" value=\"123\"/>");
        List<Diagnostic> diags = diagnose(xml);
        List<Diagnostic> beanDiags = diags.stream()
                .filter(d -> d.getCode() != null && d.getCode().getLeft() != null
                        && d.getCode().getLeft().toString().startsWith("Bean"))
                .collect(Collectors.toList());
        assertTrue(beanDiags.isEmpty());
    }

    @Test
    public void testBeanGetPropertyMissingProperty() {
        String xml = synapseWrap("<bean action=\"GET_PROPERTY\" var=\"x\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "BeanPropertyActionMissingProperty");
        assertEquals(1, diags.size());
    }

    @Test
    public void testBeanGetPropertyComplete() {
        String xml = synapseWrap("<bean action=\"GET_PROPERTY\" var=\"x\" property=\"name\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "BeanPropertyActionMissingProperty");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testBeanRemoveNoExtras() {
        String xml = synapseWrap("<bean action=\"REMOVE\" var=\"x\"/>");
        List<Diagnostic> diags = diagnose(xml);
        List<Diagnostic> beanDiags = diags.stream()
                .filter(d -> d.getCode() != null && d.getCode().getLeft() != null
                        && d.getCode().getLeft().toString().startsWith("Bean"))
                .collect(Collectors.toList());
        assertTrue(beanDiags.isEmpty());
    }

    // ===== P2-21: Regex syntax validation in switch/filter =====

    @Test
    public void testSwitchCaseInvalidRegex() {
        String xml = synapseWrap(
                "<switch source=\"${payload.type}\">" +
                "<case regex=\"[unclosed\"><log/></case>" +
                "<default><log/></default></switch>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidRegexPattern");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testSwitchCaseValidRegex() {
        String xml = synapseWrap(
                "<switch source=\"${payload.type}\">" +
                "<case regex=\"^foo.*$\"><log/></case>" +
                "<default><log/></default></switch>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidRegexPattern");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testFilterInvalidRegex() {
        String xml = synapseWrap("<filter source=\"${payload.type}\" regex=\"(unclosed\">" +
                "<then><log/></then><else><log/></else></filter>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidRegexPattern");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testFilterValidRegex() {
        String xml = synapseWrap("<filter source=\"${payload.type}\" regex=\"^premium$\">" +
                "<then><log/></then><else><log/></else></filter>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidRegexPattern");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testSwitchCaseEmptyRegex() {
        // Empty regex is technically valid (matches empty string)
        String xml = synapseWrap(
                "<switch source=\"${payload.type}\">" +
                "<case regex=\"\"><log/></case>" +
                "<default><log/></default></switch>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidRegexPattern");
        assertTrue(diags.isEmpty());
    }

    // ===== P2-25: Property mediator type-value mismatch =====

    @Test
    public void testPropertyIntegerValid() {
        String xml = synapseWrap("<property name=\"x\" type=\"INTEGER\" value=\"42\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertyTypeMismatch");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPropertyIntegerInvalid() {
        String xml = synapseWrap("<property name=\"x\" type=\"INTEGER\" value=\"abc\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertyTypeMismatch");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testPropertyBooleanValid() {
        String xml = synapseWrap("<property name=\"x\" type=\"BOOLEAN\" value=\"true\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertyTypeMismatch");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPropertyBooleanInvalid() {
        String xml = synapseWrap("<property name=\"x\" type=\"BOOLEAN\" value=\"yes\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertyTypeMismatch");
        assertEquals(1, diags.size());
    }

    @Test
    public void testPropertyDoubleValid() {
        String xml = synapseWrap("<property name=\"x\" type=\"DOUBLE\" value=\"3.14\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertyTypeMismatch");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPropertyDoubleInvalid() {
        String xml = synapseWrap("<property name=\"x\" type=\"DOUBLE\" value=\"not_num\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertyTypeMismatch");
        assertEquals(1, diags.size());
    }

    @Test
    public void testPropertyLongValid() {
        String xml = synapseWrap("<property name=\"x\" type=\"LONG\" value=\"999999999999\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertyTypeMismatch");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPropertyFloatInvalid() {
        String xml = synapseWrap("<property name=\"x\" type=\"FLOAT\" value=\"xyz\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertyTypeMismatch");
        assertEquals(1, diags.size());
    }

    @Test
    public void testPropertyShortOverflow() {
        String xml = synapseWrap("<property name=\"x\" type=\"SHORT\" value=\"99999\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertyTypeMismatch");
        assertEquals(1, diags.size());
    }

    @Test
    public void testPropertyExpressionSkipped() {
        String xml = synapseWrap("<property name=\"x\" type=\"INTEGER\" value=\"${payload.num}\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertyTypeMismatch");
        assertTrue(diags.isEmpty());
    }

    // ===== P2-29: Endpoint suspend/timeout config validation =====

    @Test
    public void testErrorCodesValid() {
        String xml = "<endpoint xmlns=\"" + SYNAPSE_NS + "\" name=\"EP\">"
                + "<http method=\"GET\" uri-template=\"http://localhost:8080\">"
                + "<suspendOnFailure><errorCodes>101503, 101504</errorCodes></suspendOnFailure>"
                + "</http></endpoint>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidErrorCodesFormat");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testErrorCodesInvalid() {
        String xml = "<endpoint xmlns=\"" + SYNAPSE_NS + "\" name=\"EP\">"
                + "<http method=\"GET\" uri-template=\"http://localhost:8080\">"
                + "<suspendOnFailure><errorCodes>abc, def</errorCodes></suspendOnFailure>"
                + "</http></endpoint>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidErrorCodesFormat");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testProgressionFactorValid() {
        String xml = "<endpoint xmlns=\"" + SYNAPSE_NS + "\" name=\"EP\">"
                + "<http method=\"GET\" uri-template=\"http://localhost:8080\">"
                + "<suspendOnFailure><progressionFactor>2.0</progressionFactor></suspendOnFailure>"
                + "</http></endpoint>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidProgressionFactor");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testProgressionFactorZero() {
        String xml = "<endpoint xmlns=\"" + SYNAPSE_NS + "\" name=\"EP\">"
                + "<http method=\"GET\" uri-template=\"http://localhost:8080\">"
                + "<suspendOnFailure><progressionFactor>0</progressionFactor></suspendOnFailure>"
                + "</http></endpoint>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidProgressionFactor");
        assertEquals(1, diags.size());
    }

    @Test
    public void testProgressionFactorNegative() {
        String xml = "<endpoint xmlns=\"" + SYNAPSE_NS + "\" name=\"EP\">"
                + "<http method=\"GET\" uri-template=\"http://localhost:8080\">"
                + "<suspendOnFailure><progressionFactor>-1</progressionFactor></suspendOnFailure>"
                + "</http></endpoint>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidProgressionFactor");
        assertEquals(1, diags.size());
    }

    @Test
    public void testResponseActionValid() {
        String xml = "<endpoint xmlns=\"" + SYNAPSE_NS + "\" name=\"EP\">"
                + "<http method=\"GET\" uri-template=\"http://localhost:8080\">"
                + "<timeout><responseAction>discard</responseAction></timeout>"
                + "</http></endpoint>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidResponseAction");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testResponseActionInvalid() {
        String xml = "<endpoint xmlns=\"" + SYNAPSE_NS + "\" name=\"EP\">"
                + "<http method=\"GET\" uri-template=\"http://localhost:8080\">"
                + "<timeout><responseAction>ignore</responseAction></timeout>"
                + "</http></endpoint>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidResponseAction");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testResponseActionFault() {
        String xml = "<endpoint xmlns=\"" + SYNAPSE_NS + "\" name=\"EP\">"
                + "<http method=\"GET\" uri-template=\"http://localhost:8080\">"
                + "<timeout><responseAction>fault</responseAction></timeout>"
                + "</http></endpoint>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidResponseAction");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testProgressionFactorNonNumeric() {
        String xml = "<endpoint xmlns=\"" + SYNAPSE_NS + "\" name=\"EP\">"
                + "<http method=\"GET\" uri-template=\"http://localhost:8080\">"
                + "<suspendOnFailure><progressionFactor>abc</progressionFactor></suspendOnFailure>"
                + "</http></endpoint>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "InvalidProgressionFactor");
        assertEquals(1, diags.size());
    }

    // ===== P2-25: Property BOOLEAN case insensitive =====

    @Test
    public void testPropertyBooleanCaseInsensitive() {
        String xml = synapseWrap("<property name=\"x\" type=\"BOOLEAN\" value=\"TRUE\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertyTypeMismatch");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPropertyStringAlwaysValid() {
        String xml = synapseWrap("<property name=\"x\" type=\"STRING\" value=\"anything goes\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "PropertyTypeMismatch");
        assertTrue(diags.isEmpty());
    }

    // ===== P3-31: Rewrite mediator action validation =====

    @Test
    public void testRewriteActionSetMissingValue() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<rewrite><rewriterule><action type=\"set\" fragment=\"path\"/></rewriterule></rewrite>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "RewriteActionMissingValue");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testRewriteActionSetWithValue() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<rewrite><rewriterule><action type=\"set\" value=\"/new\" fragment=\"path\"/></rewriterule></rewrite>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "RewriteActionMissingValue");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testRewriteActionSetWithXpath() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<rewrite><rewriterule><action type=\"set\" xpath=\"get-property('uri')\" fragment=\"path\"/></rewriterule></rewrite>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "RewriteActionMissingValue");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testRewriteActionRemoveNoValueNeeded() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<rewrite><rewriterule><action type=\"remove\" fragment=\"query\"/></rewriterule></rewrite>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "RewriteActionMissingValue");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testRewriteActionReplaceMissingAll() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<rewrite><rewriterule><action type=\"replace\" fragment=\"path\"/></rewriterule></rewrite>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "RewriteActionMissingValue");
        assertEquals(1, diags.size());
    }

    @Test
    public void testRewriteActionReplaceWithRegex() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<rewrite><rewriterule><action type=\"replace\" regex=\"/old\" value=\"/new\" fragment=\"path\"/></rewriterule></rewrite>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "RewriteActionMissingValue");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testRewriteActionDefaultTypeNoValue() {
        // No type attribute defaults to "set" — still needs value/xpath
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<rewrite><rewriterule><action fragment=\"path\"/></rewriterule></rewrite>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "RewriteActionMissingValue");
        assertEquals(1, diags.size());
    }

    @Test
    public void testRewriteActionAppendWithValue() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<rewrite><rewriterule><action type=\"append\" value=\"/suffix\" fragment=\"path\"/></rewriterule></rewrite>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "RewriteActionMissingValue");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testRewriteActionNotInsideRewriteRule() {
        // <action> outside <rewriterule> should not be validated
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<action type=\"set\"/>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "RewriteActionMissingValue");
        assertTrue(diags.isEmpty());
    }

    // ===== P3-47: Validate mediator on-fail must have content =====

    @Test
    public void testValidateOnFailEmpty() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<validate><schema key=\"gov:schema.xsd\"/><on-fail/></validate>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ValidateOnFailEmpty");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testValidateOnFailWithMediator() {
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<validate><schema key=\"gov:schema.xsd\"/><on-fail><drop/></on-fail></validate>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ValidateOnFailEmpty");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testOnFailOutsideValidate() {
        // <on-fail> outside <validate> should not be validated
        String xml = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
                + "<on-fail/>"
                + "</sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ValidateOnFailEmpty");
        assertTrue(diags.isEmpty());
    }

    // ===== Wrong namespace detection =====

    @Test
    public void testWrongNamespace() {
        String xml = "<api xmlns=\"http://wrong.namespace.com/synapse\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\" uri-template=\"/test\"><inSequence><respond/></inSequence></resource>"
                + "</api>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "WrongSynapseNamespace");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
        assertTrue(diags.get(0).getMessage().contains("http://wrong.namespace.com/synapse"));
    }

    @Test
    public void testWrongNamespaceOnSequence() {
        String xml = "<sequence xmlns=\"http://example.com/wrong\" name=\"test\"><respond/></sequence>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "WrongSynapseNamespace");
        assertEquals(1, diags.size());
    }

    @Test
    public void testCorrectNamespaceNoWrongWarning() {
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\" uri-template=\"/test\"><inSequence><respond/></inSequence></resource>"
                + "</api>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "WrongSynapseNamespace");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testNonSynapseRootNoWrongWarning() {
        // A non-Synapse root element should not trigger wrong namespace warning
        String xml = "<beans xmlns=\"http://www.springframework.org/schema/beans\"/>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "WrongSynapseNamespace");
        assertTrue(diags.isEmpty());
    }

    // ===== Resource with both uri-template and url-mapping =====

    @Test
    public void testResourceBothUriTemplateAndUrlMapping() {
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\" uri-template=\"/doctors\" url-mapping=\"/doctors\">"
                + "<inSequence><respond/></inSequence></resource></api>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ResourceBothUriTemplateAndUrlMapping");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
        assertTrue(diags.get(0).getMessage().contains("url-mapping"));
    }

    @Test
    public void testResourceOnlyUriTemplateNoBothWarning() {
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\" uri-template=\"/doctors\">"
                + "<inSequence><respond/></inSequence></resource></api>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ResourceBothUriTemplateAndUrlMapping");
        assertTrue(diags.isEmpty());
    }

    // ===== Variable with both value and expression =====

    @Test
    public void testVariableBothValueAndExpression() {
        String xml = synapseWrap("<variable name=\"x\" type=\"STRING\" value=\"hello\" expression=\"${payload.x}\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "BothValueAndExpression");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
        assertTrue(diags.get(0).getMessage().contains("'x'"));
    }

    @Test
    public void testVariableOnlyValueNoBothWarning() {
        String xml = synapseWrap("<variable name=\"x\" type=\"STRING\" value=\"hello\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "BothValueAndExpression");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testVariableOnlyExpressionNoBothWarning() {
        String xml = synapseWrap("<variable name=\"x\" type=\"STRING\" expression=\"${payload.x}\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "BothValueAndExpression");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testPropertyBothValueAndExpression() {
        String xml = synapseWrap("<property name=\"x\" value=\"hello\" expression=\"${payload.x}\"/>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "BothValueAndExpression");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testPropertyInsideLogNoBothWarning() {
        // <property> inside <log> should not trigger this warning
        String xml = synapseWrap("<log level=\"custom\"><property name=\"x\" value=\"hello\" expression=\"${payload.x}\"/></log>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "BothValueAndExpression");
        assertTrue(diags.isEmpty());
    }

    // ===== ForEach missing collection or expression =====

    @Test
    public void testForEachMissingBothAttributes() {
        String xml = synapseWrap("<foreach><sequence><log/></sequence></foreach>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ForEachMissingCollectionOrExpression");
        assertEquals(1, diags.size());
        assertEquals(DiagnosticSeverity.Error, diags.get(0).getSeverity());
    }

    @Test
    public void testForEachWithCollectionNoError() {
        String xml = synapseWrap("<foreach collection=\"${payload.items}\"><sequence><log/></sequence></foreach>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ForEachMissingCollectionOrExpression");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testForEachWithExpressionNoError() {
        String xml = synapseWrap("<foreach expression=\"//items\"><sequence><log/></sequence></foreach>");
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "ForEachMissingCollectionOrExpression");
        assertTrue(diags.isEmpty());
    }

    // ===== Duplicate API resource URI-template =====

    @Test
    public void testDuplicateResourceUriTemplate() {
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\" uri-template=\"/doctors\"><inSequence><respond/></inSequence></resource>"
                + "<resource methods=\"GET\" uri-template=\"/doctors\"><inSequence><respond/></inSequence></resource>"
                + "</api>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "DuplicateResourceUriTemplate");
        assertEquals(2, diags.size());
        assertEquals(DiagnosticSeverity.Warning, diags.get(0).getSeverity());
    }

    @Test
    public void testDuplicateResourceSamePathDifferentMethods() {
        // Same path but different methods — NOT a duplicate
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\" uri-template=\"/doctors\"><inSequence><respond/></inSequence></resource>"
                + "<resource methods=\"POST\" uri-template=\"/doctors\"><inSequence><respond/></inSequence></resource>"
                + "</api>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "DuplicateResourceUriTemplate");
        assertTrue(diags.isEmpty());
    }

    @Test
    public void testDuplicateResourceUrlMapping() {
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\" url-mapping=\"/doctors/*\"><inSequence><respond/></inSequence></resource>"
                + "<resource methods=\"GET\" url-mapping=\"/doctors/*\"><inSequence><respond/></inSequence></resource>"
                + "</api>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "DuplicateResourceUriTemplate");
        assertEquals(2, diags.size());
    }

    @Test
    public void testUniqueResourcesNoDuplicate() {
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"TestAPI\" context=\"/test\">"
                + "<resource methods=\"GET\" uri-template=\"/doctors\"><inSequence><respond/></inSequence></resource>"
                + "<resource methods=\"GET\" uri-template=\"/patients\"><inSequence><respond/></inSequence></resource>"
                + "</api>";
        List<Diagnostic> diags = diagnosticsWithCode(diagnose(xml), "DuplicateResourceUriTemplate");
        assertTrue(diags.isEmpty());
    }

    // ===== New-pattern hints (not testable via unit tests since they require project path) =====
    // Hints are gated on is440Plus which requires Utils.getServerVersion() with a real pom.xml.
    // Integration tests in the MI extension validate hint behavior end-to-end.

    // ===== Cross-project reference resolution (pom.xml dependencies) =====

    private String originalUserHome;

    @AfterEach
    public void restoreUserHome() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
            originalUserHome = null;
        }
        SynapseLanguageService.setLoadedResourceFinder(null);
    }

    /**
     * Simulates what {@link SynapseLanguageService#init} does for dependent projects:
     * loads them via a real finder and publishes it so the diagnostics participant
     * can see the resulting map through {@link SynapseLanguageService#getLoadedDependentResources()}.
     */
    private void loadDependentResourcesForProject(Path projectPath) {
        NewProjectResourceFinder finder = new NewProjectResourceFinder();
        finder.loadDependentResources(projectPath.toString());
        SynapseLanguageService.setLoadedResourceFinder(finder);
    }

    /**
     * Runs diagnostics against an XML document whose URI is a real file path under
     * {@code <projectRoot>/src/main/wso2mi/artifacts/...}, so
     * {@link SynapseDiagnosticsParticipant#deriveProjectPath} can resolve the project root
     * and trigger cross-file reference validation.
     */
    private List<Diagnostic> diagnoseAtPath(String xml, Path xmlFilePath) throws Exception {
        Files.createDirectories(xmlFilePath.getParent());
        Files.writeString(xmlFilePath, xml);
        TextDocument textDocument = new TextDocument(xml, xmlFilePath.toUri().toString());
        DOMDocument document = DOMParser.getInstance().parse(textDocument, null);
        List<Diagnostic> diagnostics = new ArrayList<>();
        SynapseDiagnosticsParticipant participant = new SynapseDiagnosticsParticipant();
        participant.doDiagnostics(document, diagnostics, null, () -> {});
        return diagnostics;
    }

    @Test
    public void testSequenceKeyFromPomDependencyResolves(@TempDir Path tempDir) throws Exception {
        // Redirect user.home so loadDependentResources looks inside tempDir
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        // Consumer project references a sequence defined only in a pom dependency
        Path consumer = tempDir.resolve("consumer");
        Path apiXml = consumer.resolve("src/main/wso2mi/artifacts/apis/cvh.xml");
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"cvh\" context=\"/cvh\">"
                + "<resource methods=\"GET\" uri-template=\"/\">"
                + "<inSequence><sequence key=\"fromDep\"/></inSequence>"
                + "</resource></api>";

        // Fake extracted dependency project containing the referenced sequence
        String hash = Utils.getHash(consumer.toString());
        Path depSequence = tempDir.resolve(".wso2-mi/integration-project-dependencies")
                .resolve("consumer_" + hash)
                .resolve("Extracted/dep/src/main/wso2mi/artifacts/sequences/fromDep-1.0.0.xml");
        Files.createDirectories(depSequence.getParent());
        Files.writeString(depSequence,
                "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"fromDep\"><log/></sequence>");

        loadDependentResourcesForProject(consumer);
        List<Diagnostic> diags = diagnoseAtPath(xml, apiXml);
        List<Diagnostic> unresolved = diagnosticsWithCode(diags, "UnresolvedArtifactReference");
        assertTrue(unresolved.isEmpty(),
                "Sequence 'fromDep' is defined in a pom dependency and should resolve, but got: "
                        + unresolved.stream().map(Diagnostic::getMessage).collect(Collectors.toList()));
    }

    @Test
    public void testUnknownSequenceStillFlaggedWithDependencies(@TempDir Path tempDir) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        Path consumer = tempDir.resolve("consumer");
        Path apiXml = consumer.resolve("src/main/wso2mi/artifacts/apis/cvh.xml");
        String xml = "<api xmlns=\"" + SYNAPSE_NS + "\" name=\"cvh\" context=\"/cvh\">"
                + "<resource methods=\"GET\" uri-template=\"/\">"
                + "<inSequence><sequence key=\"reallyDoesNotExist\"/></inSequence>"
                + "</resource></api>";

        // A dependency with a different sequence name — should not mask the typo
        String hash = Utils.getHash(consumer.toString());
        Path depSequence = tempDir.resolve(".wso2-mi/integration-project-dependencies")
                .resolve("consumer_" + hash)
                .resolve("Extracted/dep/src/main/wso2mi/artifacts/sequences/somethingElse-1.0.0.xml");
        Files.createDirectories(depSequence.getParent());
        Files.writeString(depSequence,
                "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"somethingElse\"><log/></sequence>");

        loadDependentResourcesForProject(consumer);
        List<Diagnostic> diags = diagnoseAtPath(xml, apiXml);
        List<Diagnostic> unresolved = diagnosticsWithCode(diags, "UnresolvedArtifactReference");
        assertEquals(1, unresolved.size());
        assertTrue(unresolved.get(0).getMessage().contains("reallyDoesNotExist"));
    }
}
