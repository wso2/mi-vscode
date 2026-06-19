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

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@code synapse/codeDiagnostic} document-URI plumbing.
 *
 * <p>The MI Copilot agent validates in-memory (unsaved) code via {@code synapse/codeDiagnostic}.
 * Before the fix this path parsed the code with the literal URI {@code "temp"}, which made the
 * URI-gated {@code SynapseExpressionValidator} (it only runs for files under
 * {@code src/main/wso2mi/artifacts}) silently skip every expression diagnostic. The fix lets the
 * caller supply the real file name as the document URI (with {@code "temp"} as the backward
 * compatible fallback).
 *
 * <p>These tests exercise the exact pipeline {@code SynapseLanguageService.codeDiagnostic()} runs
 * internally — {@code Utils.getDOMDocument(code, uri, resolver)} followed by
 * {@code XMLLanguageService.doDiagnostics(...)} — and assert the operator-precedence warning is
 * surfaced only when the URI is under the artifacts path.
 */
public class CodeDiagnosticFileNameTest extends AbstractCacheBasedTest {

    private static final String SYNAPSE_NS = "http://ws.apache.org/ns/synapse";
    private static final String SYNAPSE_CATALOG_440 =
            "src/main/resources/org/eclipse/lemminx/schemas/440/catalog.xml";
    // An absolute path under the project artifacts directory, as the MI extension sends.
    private static final String ARTIFACT_URI =
            "/home/proj/src/main/wso2mi/artifacts/sequences/Test.xml";

    // <property> with an unparenthesized comparison/logical mix — the precedence pitfall.
    private static final String UNPARENTHESIZED = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
            + "<property name=\"p\" expression=\"${payload.id &lt;= 0 or payload.id &gt; 10}\"/>"
            + "</sequence>";
    // The corrected, explicitly parenthesized form.
    private static final String PARENTHESIZED = "<sequence xmlns=\"" + SYNAPSE_NS + "\" name=\"test\">"
            + "<property name=\"p\" expression=\"${(payload.id &lt;= 0) or (payload.id &gt; 10)}\"/>"
            + "</sequence>";

    /**
     * Reproduces the codeDiagnostic pipeline: build the DOM document with the given URI (via the
     * fileName-aware overload the fix added), then run the full XML diagnostics pipeline.
     */
    private List<Diagnostic> codeDiagnostic(String code, String fileName) {
        XMLLanguageService ls = new XMLLanguageService();
        String uri = fileName != null ? fileName : "temp";
        DOMDocument document = Utils.getDOMDocument(code, uri, ls.getResolverExtensionManager());
        ls.setDocumentProvider(u -> document);

        ContentModelSettings settings = new ContentModelSettings();
        settings.setUseCache(false);
        XMLValidationRootSettings validation = new XMLValidationRootSettings();
        validation.setNoGrammar("ignore");
        settings.setValidation(validation);
        settings.setCatalogs(new String[]{SYNAPSE_CATALOG_440});
        ls.doSave(new XMLAssert.SettingsSaveContext(settings));

        return ls.doDiagnostics(document, settings.getValidation(), Collections.emptyMap(), () -> {});
    }

    private List<Diagnostic> precedenceWarnings(List<Diagnostic> diagnostics) {
        return diagnostics.stream()
                .filter(d -> d.getMessage() != null && d.getMessage().contains("Operator precedence"))
                .collect(Collectors.toList());
    }

    @Test
    public void testPrecedenceWarningReturnedForArtifactsPath() {
        // With a real artifacts file name, the expression validator runs and reports the warning.
        List<Diagnostic> diags = codeDiagnostic(UNPARENTHESIZED, ARTIFACT_URI);
        assertFalse(precedenceWarnings(diags).isEmpty(),
                "codeDiagnostic with an artifacts file name should surface the operator-precedence warning");
    }

    @Test
    public void testNoPrecedenceWarningForParenthesizedExpression() {
        // The corrected, parenthesized form must not be flagged even on the artifacts path.
        List<Diagnostic> diags = codeDiagnostic(PARENTHESIZED, ARTIFACT_URI);
        assertTrue(precedenceWarnings(diags).isEmpty(),
                "Parenthesized comparisons should produce no operator-precedence warning");
    }

    @Test
    public void testTempFallbackDocumentsUnchangedBehavior() {
        // Backward compatibility: a null file name falls back to "temp", which is not under the
        // artifacts path, so the expression validator stays disabled (unchanged pre-fix behavior).
        List<Diagnostic> diags = codeDiagnostic(UNPARENTHESIZED, null);
        assertTrue(precedenceWarnings(diags).isEmpty(),
                "The \"temp\" fallback keeps the expression validator disabled, as before the fix");
    }

    // ===== URI plumbing unit checks (deterministic, no validation pipeline) =====

    @Test
    public void testGetDOMDocumentUsesProvidedUri() {
        DOMDocument document = Utils.getDOMDocument(UNPARENTHESIZED, ARTIFACT_URI, null);
        assertEquals(ARTIFACT_URI, document.getDocumentURI(),
                "The 3-arg overload should assign the supplied URI to the document");
    }

    @Test
    public void testGetDOMDocumentDefaultStillUsesTemp() {
        // The legacy overloads (which delegate with no URI) must keep the "temp" URI unchanged.
        DOMDocument document = Utils.getDOMDocument(UNPARENTHESIZED);
        assertEquals("temp", document.getDocumentURI(),
                "The legacy overloads should keep the \"temp\" URI for backward compatibility");
    }

    @Test
    public void testCodeDiagnosticRequestCarriesFileName() {
        org.eclipse.lemminx.customservice.synapse.CodeDiagnosticRequest request =
                new org.eclipse.lemminx.customservice.synapse.CodeDiagnosticRequest();
        request.setCode(UNPARENTHESIZED);
        request.setFileName(ARTIFACT_URI);
        assertEquals(UNPARENTHESIZED, request.getCode());
        assertEquals(ARTIFACT_URI, request.getFileName(),
                "CodeDiagnosticRequest must carry the fileName sent by the extension");
    }
}
