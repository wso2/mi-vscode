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
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.NewProjectResourceFinder;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ArtifactResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.RegistryResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.Resource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ResourceResponse;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationSettings;
import org.eclipse.lemminx.services.extensions.diagnostics.IDiagnosticsParticipant;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import org.eclipse.lemminx.dom.DOMAttr;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Synapse-specific diagnostic participant that validates MI configuration XML
 * for semantic rules that XSD schemas cannot express.
 */
public class SynapseDiagnosticsParticipant implements IDiagnosticsParticipant {

    private static final Logger LOGGER = Logger.getLogger(SynapseDiagnosticsParticipant.class.getName());

    private static final String SYNAPSE_NS = "http://ws.apache.org/ns/synapse";
    private static final String SOURCE = "synapse";
    // Use File.separator for the OS-native path (used after converting URI to Path)
    private static final String SRC_MAIN_WSO2MI = "src" + java.io.File.separator + "main"
            + java.io.File.separator + "wso2mi";
    // Also keep a forward-slash variant for file URIs that may not be converted to Path
    private static final String SRC_MAIN_WSO2MI_URI = "src/main/wso2mi";

    private static final long ARTIFACT_CACHE_TTL_MS = 5000; // 5 seconds
    private final Map<String, CachedArtifactIndex> artifactIndexCache = new ConcurrentHashMap<>();

    /** Template name -> absolute file path, populated during artifact index building. */
    private volatile Map<String, String> templateFilePaths = java.util.Collections.emptyMap();
    /** Artifact names that appear in multiple files (duplicates). */
    private volatile Set<String> duplicateArtifactNames = java.util.Collections.emptySet();
    /** Artifact names that participate in direct circular references (A->B->A). */
    private volatile Set<String> cyclicArtifacts = java.util.Collections.emptySet();

    private static final Set<String> SYNAPSE_ROOT_ELEMENTS = new HashSet<>(Arrays.asList(
            "api", "proxy", "endpoint", "sequence", "inboundEndpoint", "template",
            "task", "localEntry", "messageStore", "messageProcessor", "registry"
    ));

    private static final Set<String> TERMINAL_MEDIATORS = new HashSet<>(Arrays.asList(
            "respond", "drop", "loopback"
    ));

    private static final Set<String> SEQUENCE_CONTAINERS = new HashSet<>(Arrays.asList(
            "inSequence", "outSequence", "faultSequence", "sequence", "then", "else",
            "case", "default", "onComplete", "onAccept", "onReject"
    ));

    /**
     * Pattern for validating fully-qualified Java class names (e.g., com.example.MyClass).
     * Requires at least one dot (package separator).
     */
    private static final Pattern FQN_PATTERN = Pattern.compile(
            "^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+$");

    /**
     * Pattern for validating comma-separated error codes (integers, possibly negative).
     */
    private static final Pattern ERROR_CODES_PATTERN = Pattern.compile(
            "^\\s*-?\\d+(\\s*,\\s*-?\\d+)*\\s*$");

    private static final Set<String> VALID_RESPONSE_ACTIONS = new HashSet<>(Arrays.asList(
            "discard", "fault", "never"
    ));

    /**
     * Regex to extract variable names from vars.X references in Synapse expressions.
     * Matches: vars.someName, vars["someName"], vars['someName']
     */
    private static final Pattern VARS_REF_PATTERN = Pattern.compile(
            "vars\\.([a-zA-Z_][a-zA-Z_0-9-]*)" +
            "|vars\\[\"([^\"]+)\"\\]" +
            "|vars\\['([^']+)'\\]"
    );

    /**
     * Regex to extract ${...} expressions from attribute values.
     * Uses a non-greedy match with a lookahead to handle nested braces (e.g., JSONPath filters).
     * Also handles the {${...}} form used in some attribute values.
     */
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
            "\\$\\{(.+?)\\}(?!\\})" +     // ${...} — non-greedy, not followed by another }
            "|\\{\\$\\{(.+?)\\}\\}"        // {${...}} — wrapped form
    );

    /**
     * Regex to find $N placeholders in PayloadFactory format strings.
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$(\\d+)");

    /**
     * Elements whose 'key' attribute references a named artifact (endpoint, sequence, etc.).
     */
    private static final Set<String> KEY_REF_ELEMENTS = new HashSet<>(Arrays.asList(
            "endpoint", "sequence"
    ));

    /**
     * Elements whose 'onError' attribute references a named sequence.
     */
    private static final Set<String> ON_ERROR_ELEMENTS = new HashSet<>(Arrays.asList(
            "api", "proxy", "sequence", "inboundEndpoint", "resource"
    ));

    @Override
    public void doDiagnostics(DOMDocument xmlDocument, List<Diagnostic> diagnostics,
                              XMLValidationSettings validationSettings, CancelChecker cancelChecker) {
        DOMElement root = xmlDocument.getDocumentElement();
        if (root == null) {
            return;
        }

        // Check if this is a Synapse XML file
        String rootName = root.getLocalName();
        String namespace = root.getNamespaceURI();

        if (SYNAPSE_NS.equals(namespace)) {
            // Valid Synapse file — run all validations
            Set<String> definedVariables = new HashSet<>();
            Set<String> knownArtifacts = buildArtifactNameIndex(xmlDocument, cancelChecker);

            // Detect MI runtime version for new-pattern hints
            String projectPath = deriveProjectPath(xmlDocument);
            boolean is440Plus = false;
            if (projectPath != null) {
                try {
                    String version = Utils.getServerVersion(projectPath, Constant.DEFAULT_MI_VERSION);
                    is440Plus = version != null && !Constant.MI_430_VERSION.equals(version);
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Could not detect MI runtime version for hints", e);
                }
            }

            validateElement(root, diagnostics, xmlDocument, definedVariables, knownArtifacts, is440Plus, cancelChecker);

            // P1-19: Check if this document's root artifact name is a duplicate
            validateDuplicateArtifactName(root, diagnostics);
        } else if (rootName != null && SYNAPSE_ROOT_ELEMENTS.contains(rootName) && namespace == null) {
            // Looks like a Synapse file but missing namespace
            Range range = XMLPositionUtility.selectStartTagName(root);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "Synapse namespace 'http://ws.apache.org/ns/synapse' is missing on root element '" +
                                rootName + "'. XML validation is disabled without the correct namespace. " +
                                "Add xmlns=\"http://ws.apache.org/ns/synapse\" to enable validation.",
                        DiagnosticSeverity.Warning, "MissingSynapseNamespace");
            }
        } else if (rootName != null && SYNAPSE_ROOT_ELEMENTS.contains(rootName)
                && namespace != null && !SYNAPSE_NS.equals(namespace)) {
            // Synapse root element with wrong namespace
            Range range = XMLPositionUtility.selectStartTagName(root);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "Root element '" + rootName + "' has namespace '" + namespace +
                                "' but Synapse requires 'http://ws.apache.org/ns/synapse'. " +
                                "XML validation may not work correctly with the wrong namespace.",
                        DiagnosticSeverity.Warning, "WrongSynapseNamespace");
            }
        }
    }

    private void validateElement(DOMNode node, List<Diagnostic> diagnostics, DOMDocument document,
                                Set<String> definedVariables, Set<String> knownArtifacts,
                                boolean is440Plus, CancelChecker cancelChecker) {
        if (cancelChecker != null) {
            cancelChecker.checkCanceled();
        }
        if (!(node instanceof DOMElement)) {
            return;
        }
        DOMElement element = (DOMElement) node;
        String name = element.getLocalName();
        if (name == null) {
            return;
        }

        // Track variable definitions BEFORE checking references in this element
        collectVariableDefinition(element, definedVariables);

        // Validate variable references in expression attributes
        validateVariableReferences(element, diagnostics, document, definedVariables);

        // Cross-file reference validation
        if (knownArtifacts != null) {
            validateCrossReferences(element, diagnostics, knownArtifacts);
        }

        switch (name) {
            case "resource":
                validateAPIResource(element, diagnostics, document);
                break;
            case "filter":
                validateFilterMediator(element, diagnostics, document);
                break;
            case "property":
                validatePropertyMediator(element, diagnostics, document);
                break;
            case "header":
                validateHeaderMediator(element, diagnostics, document);
                break;
            case "log":
                validateLogMediator(element, diagnostics, document);
                break;
            case "switch":
                validateSwitchMediator(element, diagnostics, document);
                break;
            case "inboundEndpoint":
                validateInboundEndpoint(element, diagnostics, document);
                break;
            case "source":
                validateEnrichSource(element, diagnostics, document);
                break;
            case "with-param":
                validateWithParam(element, diagnostics, document);
                break;
            case "trigger":
                validateTaskTrigger(element, diagnostics, document);
                break;
            case "parameter":
                validateDbParameter(element, diagnostics, document);
                break;
            case "payloadFactory":
                validatePayloadFactory(element, diagnostics, document);
                break;
            case "enrich":
                validateEnrichCompatibility(element, diagnostics, document);
                break;
            case "throttle":
                validateThrottleMediator(element, diagnostics, document);
                break;
            case "target":
                validateCloneIterateTarget(element, diagnostics, document);
                break;
            case "store":
                validateStoreMediator(element, diagnostics, knownArtifacts);
                break;
            case "schema":
                validateSchemaKey(element, diagnostics, knownArtifacts);
                break;
            case "script":
                validateScriptMediator(element, diagnostics, document);
                break;
            case "call-template":
                validateCallTemplateParams(element, diagnostics);
                break;
            case "bean":
                validateBeanMediator(element, diagnostics, document);
                break;
            case "class":
                validateClassMediator(element, diagnostics, document);
                break;
            case "suspendOnFailure":
            case "markForSuspension":
                validateEndpointSuspendConfig(element, diagnostics, document);
                break;
            case "timeout":
                validateEndpointTimeout(element, diagnostics, document);
                break;
            case "enqueue":
                validateEnqueueMediator(element, diagnostics, knownArtifacts);
                break;
            case "onComplete":
                validateOnCompleteSequenceRef(element, diagnostics, knownArtifacts);
                break;
            case "dataServiceCall":
                validateDataServiceCall(element, diagnostics, knownArtifacts);
                break;
            case "action":
                validateRewriteAction(element, diagnostics, document);
                break;
            case "on-fail":
                validateOnFail(element, diagnostics, document);
                break;
            case "variable":
                validateVariableMediator(element, diagnostics, document);
                break;
            case "foreach":
                validateForEachMediator(element, diagnostics, document);
                break;
            case "api":
                validateApiDuplicateResources(element, diagnostics, document);
                break;
        }

        // Check for unreachable code in sequence containers
        if (SEQUENCE_CONTAINERS.contains(name)) {
            validateUnreachableCode(element, diagnostics, document);
        }

        // New-pattern hints (MI 4.4.0+ only)
        if (is440Plus) {
            emitNewPatternHints(element, name, diagnostics);
        }

        // Recurse into children
        List<DOMNode> children = element.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                validateElement(child, diagnostics, document, definedVariables, knownArtifacts, is440Plus, cancelChecker);
            }
        }
    }

    /**
     * API Resource must have either uri-template or url-mapping.
     */
    private void validateAPIResource(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        // Only validate if parent is <api>
        DOMNode parent = element.getParentNode();
        if (parent == null || !(parent instanceof DOMElement) ||
                !"api".equals(((DOMElement) parent).getLocalName())) {
            return;
        }
        String uriTemplate = element.getAttribute("uri-template");
        String urlMapping = element.getAttribute("url-mapping");
        if (uriTemplate == null && urlMapping == null) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "API resource must have either 'uri-template' or 'url-mapping' attribute.",
                        DiagnosticSeverity.Error, "ResourceMissingUriTemplateOrUrlMapping");
            }
        } else if (uriTemplate != null && urlMapping != null) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "API resource has both 'uri-template' and 'url-mapping' attributes. " +
                                "Only one should be specified; 'url-mapping' will be ignored at runtime.",
                        DiagnosticSeverity.Warning, "ResourceBothUriTemplateAndUrlMapping");
            }
        }
    }

    /**
     * Filter mediator must have (source + regex) or xpath.
     */
    private void validateFilterMediator(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        String source = element.getAttribute("source");
        String regex = element.getAttribute("regex");
        String xpath = element.getAttribute("xpath");

        boolean hasSourceRegex = source != null && regex != null;
        boolean hasXpath = xpath != null;

        if (!hasSourceRegex && !hasXpath) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "Filter mediator requires a condition: either both 'source' and 'regex' attributes, " +
                                "or an 'xpath' attribute.",
                        DiagnosticSeverity.Error, "FilterMissingCondition");
            }
        }

        // P2-21: Validate regex syntax
        if (regex != null) {
            validateRegexAttribute(element, "regex", diagnostics);
        }
    }

    /**
     * Property mediator with action=set (or default) needs value or expression.
     */
    private void validatePropertyMediator(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        // Skip if this is a <property> inside <log> (those are log properties, not the property mediator)
        DOMNode parent = element.getParentNode();
        if (parent instanceof DOMElement && "log".equals(((DOMElement) parent).getLocalName())) {
            return;
        }

        String action = element.getAttribute("action");
        // Default action is "set"
        if (action == null || "set".equals(action)) {
            String value = element.getAttribute("value");
            String expression = element.getAttribute("expression");
            if (value == null && expression == null) {
                // Check if there's inline XML content (child elements)
                boolean hasChildElements = hasChildElements(element);
                if (!hasChildElements) {
                    String propName = element.getAttribute("name");
                    Range range = XMLPositionUtility.selectStartTagName(element);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Property" + (propName != null ? " '" + propName + "'" : "") +
                                        " with action 'set' requires a 'value' or 'expression' attribute.",
                                DiagnosticSeverity.Warning, "PropertySetMissingValue");
                    }
                }
            }
        }

        // Check for both value and expression (expression takes precedence, value is dead config)
        {
            String val = element.getAttribute("value");
            String expr = element.getAttribute("expression");
            if (val != null && expr != null) {
                String propName = element.getAttribute("name");
                Range range = XMLPositionUtility.selectStartTagName(element);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Property" + (propName != null ? " '" + propName + "'" : "") +
                                    " has both 'value' and 'expression' attributes. " +
                                    "Only 'expression' will be evaluated; 'value' will be ignored.",
                            DiagnosticSeverity.Warning, "BothValueAndExpression");
                }
            }
        }

        // P2-25: Type-value mismatch check
        String type = element.getAttribute("type");
        String value = element.getAttribute("value");
        if (type != null && value != null && !isExpression(value)) {
            String mismatchMsg = validateTypeValueMatch(type, value);
            if (mismatchMsg != null) {
                DOMAttr valueAttr = element.getAttributeNode("value");
                if (valueAttr != null) {
                    Range range = XMLPositionUtility.selectAttributeValue(valueAttr);
                    if (range != null) {
                        addDiagnostic(diagnostics, range, mismatchMsg,
                                DiagnosticSeverity.Error, "PropertyTypeMismatch");
                    }
                }
            }
        }
    }

    /**
     * Header mediator with action=set (or default) needs value or expression.
     */
    private void validateHeaderMediator(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        String action = element.getAttribute("action");
        if (action == null || "set".equals(action)) {
            String value = element.getAttribute("value");
            String expression = element.getAttribute("expression");
            if (value == null && expression == null) {
                boolean hasChildElements = hasChildElements(element);
                if (!hasChildElements) {
                    String headerName = element.getAttribute("name");
                    Range range = XMLPositionUtility.selectStartTagName(element);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Header" + (headerName != null ? " '" + headerName + "'" : "") +
                                        " with action 'set' requires a 'value' or 'expression' attribute, " +
                                        "or an inline XML child element.",
                                DiagnosticSeverity.Warning, "HeaderSetMissingValue");
                    }
                }
            }
        }
    }

    /**
     * Log with level="custom" should have at least one property child.
     */
    private void validateLogMediator(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        String level = element.getAttribute("level");
        if ("custom".equals(level)) {
            boolean hasPropertyChild = false;
            List<DOMNode> children = element.getChildren();
            if (children != null) {
                for (DOMNode child : children) {
                    if (child instanceof DOMElement && "property".equals(((DOMElement) child).getLocalName())) {
                        hasPropertyChild = true;
                        break;
                    }
                }
            }
            if (!hasPropertyChild) {
                Range range = XMLPositionUtility.selectStartTagName(element);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Log mediator with level='custom' should have at least one <property> child element.",
                            DiagnosticSeverity.Warning, "LogCustomMissingProperties");
                }
            }
        }
    }

    /**
     * Switch mediator should not have duplicate case regex values.
     */
    private void validateSwitchMediator(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        Set<String> seenRegex = new HashSet<>();
        List<DOMNode> children = element.getChildren();
        if (children == null) return;
        for (DOMNode child : children) {
            if (child instanceof DOMElement && "case".equals(((DOMElement) child).getLocalName())) {
                // P2-21: Validate regex syntax
                validateRegexAttribute((DOMElement) child, "regex", diagnostics);
                String regex = ((DOMElement) child).getAttribute("regex");
                if (regex != null && !seenRegex.add(regex)) {
                    Range range = XMLPositionUtility.selectStartTagName((DOMElement) child);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Duplicate switch case: regex '" + regex + "' already exists in this switch mediator.",
                                DiagnosticSeverity.Warning, "DuplicateSwitchCase");
                    }
                }
            }
        }
    }

    /**
     * InboundEndpoint must have either protocol or class.
     */
    private void validateInboundEndpoint(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        String protocol = element.getAttribute("protocol");
        String clazz = element.getAttribute("class");
        if (protocol == null && clazz == null) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "Inbound endpoint must have either 'protocol' or 'class' attribute.",
                        DiagnosticSeverity.Error, "InboundMissingProtocolOrClass");
            }
        }
    }

    /**
     * Enrich source: type="custom" requires xpath, type="property" requires property attr,
     * type="inline" requires inline XML child content.
     */
    private void validateEnrichSource(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        // Only validate <source> inside <enrich>
        DOMNode parent = element.getParentNode();
        if (parent == null || !(parent instanceof DOMElement) ||
                !"enrich".equals(((DOMElement) parent).getLocalName())) {
            return;
        }

        String type = element.getAttribute("type");
        // Default type is "custom"
        if (type == null) {
            type = "custom";
        }

        Range range = XMLPositionUtility.selectStartTagName(element);
        if (range == null) {
            return;
        }

        switch (type) {
            case "custom":
                if (element.getAttribute("xpath") == null) {
                    addDiagnostic(diagnostics, range,
                            "Enrich source with type='custom' requires an 'xpath' attribute.",
                            DiagnosticSeverity.Error, "EnrichSourceCustomMissingXpath");
                }
                break;
            case "property":
                if (element.getAttribute("property") == null) {
                    addDiagnostic(diagnostics, range,
                            "Enrich source with type='property' requires a 'property' attribute.",
                            DiagnosticSeverity.Error, "EnrichSourcePropertyMissingProperty");
                }
                break;
            case "inline":
                if (!hasContent(element)) {
                    addDiagnostic(diagnostics, range,
                            "Enrich source with type='inline' requires inline content (XML or JSON).",
                            DiagnosticSeverity.Error, "EnrichSourceInlineMissingContent");
                }
                break;
        }
    }

    /**
     * P0-1: with-param must have either 'value' or 'expression' attribute.
     */
    private void validateWithParam(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        String value = element.getAttribute("value");
        String expression = element.getAttribute("expression");
        if (value == null && expression == null) {
            String paramName = element.getAttribute("name");
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "with-param" + (paramName != null ? " '" + paramName + "'" : "") +
                                " requires either a 'value' or 'expression' attribute.",
                        DiagnosticSeverity.Error, "WithParamMissingValueOrExpression");
            }
        }
    }

    /**
     * P0-2: Task trigger must have at least one scheduling mechanism.
     */
    private void validateTaskTrigger(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        // Only validate <trigger> inside <task>
        DOMNode parent = element.getParentNode();
        if (parent == null || !(parent instanceof DOMElement) ||
                !"task".equals(((DOMElement) parent).getLocalName())) {
            return;
        }
        String interval = element.getAttribute("interval");
        String count = element.getAttribute("count");
        String once = element.getAttribute("once");
        String cron = element.getAttribute("cron");
        if (interval == null && count == null && once == null && cron == null) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "Task trigger must have at least one scheduling attribute: " +
                                "'interval', 'count', 'once', or 'cron'.",
                        DiagnosticSeverity.Error, "TriggerMissingSchedule");
            }
        }
    }

    /**
     * P0-5: DB parameter (under statement) must have 'value' or 'expression'.
     */
    private void validateDbParameter(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        // Only validate <parameter> under <statement>
        DOMNode parent = element.getParentNode();
        if (parent == null || !(parent instanceof DOMElement) ||
                !"statement".equals(((DOMElement) parent).getLocalName())) {
            return;
        }
        String value = element.getAttribute("value");
        String expression = element.getAttribute("expression");
        if (value == null && expression == null) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "DB parameter requires a 'value' or 'expression' attribute.",
                        DiagnosticSeverity.Error, "DbParameterMissingValue");
            }
        }
    }

    /**
     * P0-6: PayloadFactory args count must match format $N placeholders.
     */
    private void validatePayloadFactory(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        // Skip freemarker templates — they use different placeholder syntax
        String templateType = element.getAttribute("template-type");
        if ("freemarker".equals(templateType)) {
            return;
        }

        DOMElement formatElement = null;
        DOMElement argsElement = null;
        List<DOMNode> children = element.getChildren();
        if (children == null) return;
        for (DOMNode child : children) {
            if (child instanceof DOMElement) {
                String childName = ((DOMElement) child).getLocalName();
                if ("format".equals(childName)) {
                    formatElement = (DOMElement) child;
                } else if ("args".equals(childName)) {
                    argsElement = (DOMElement) child;
                }
            }
        }

        if (formatElement == null) return;

        // Get format text content (may include child XML for xml payloads)
        String formatText = getFullTextContent(formatElement);
        if (StringUtils.isEmpty(formatText)) return;

        // Find the highest $N placeholder index
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(formatText);
        int maxPlaceholder = 0;
        while (matcher.find()) {
            int idx = Integer.parseInt(matcher.group(1));
            if (idx > maxPlaceholder) {
                maxPlaceholder = idx;
            }
        }

        if (maxPlaceholder == 0) return; // No placeholders found

        // Count <arg> children
        int argCount = 0;
        if (argsElement != null) {
            List<DOMNode> argsChildren = argsElement.getChildren();
            if (argsChildren != null) {
                for (DOMNode argChild : argsChildren) {
                    if (argChild instanceof DOMElement && "arg".equals(((DOMElement) argChild).getLocalName())) {
                        argCount++;
                    }
                }
            }
        }

        if (maxPlaceholder > argCount) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "PayloadFactory format uses placeholder $" + maxPlaceholder +
                                " but only " + argCount + " arg(s) provided. " +
                                "Add the missing arg(s) or remove unused placeholders.",
                        DiagnosticSeverity.Warning, "PayloadFactoryArgsMismatch");
            }
        }
    }

    /**
     * Returns the full text content of an element, including text within child elements.
     * Useful for payloadFactory format elements that may have XML child content.
     */
    private String getFullTextContent(DOMElement element) {
        StringBuilder sb = new StringBuilder();
        collectTextContent(element, sb);
        return sb.toString();
    }

    private void collectTextContent(DOMNode node, StringBuilder sb) {
        List<DOMNode> children = node.getChildren();
        if (children == null) return;
        for (DOMNode child : children) {
            if (child.isText()) {
                String text = child.getTextContent();
                if (text != null) {
                    sb.append(text);
                }
            } else if (child instanceof DOMElement) {
                // For XML payloads, include attribute values and recurse
                DOMElement childElem = (DOMElement) child;
                List<DOMAttr> attrs = childElem.getAttributeNodes();
                if (attrs != null) {
                    for (DOMAttr attr : attrs) {
                        String val = attr.getValue();
                        if (val != null) {
                            sb.append(val);
                        }
                    }
                }
                collectTextContent(child, sb);
            }
        }
    }

    /**
     * P1-11: Enrich source-target type compatibility check.
     */
    private void validateEnrichCompatibility(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        DOMElement sourceElem = null;
        DOMElement targetElem = null;
        List<DOMNode> children = element.getChildren();
        if (children == null) return;
        for (DOMNode child : children) {
            if (child instanceof DOMElement) {
                String childName = ((DOMElement) child).getLocalName();
                if ("source".equals(childName)) sourceElem = (DOMElement) child;
                else if ("target".equals(childName)) targetElem = (DOMElement) child;
            }
        }
        if (sourceElem == null || targetElem == null) return;

        String sourceType = sourceElem.getAttribute("type");
        String targetType = targetElem.getAttribute("type");
        String targetAction = targetElem.getAttribute("action");
        if (sourceType == null) sourceType = "custom";
        if (targetType == null) targetType = "custom";

        // Body-to-body with child action is circular
        if ("body".equals(sourceType) && "body".equals(targetType) && "child".equals(targetAction)) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "Enrich appending body to itself (source type='body', target type='body' action='child') " +
                                "creates a circular reference.",
                        DiagnosticSeverity.Warning, "EnrichCircularBodyReference");
            }
        }

        // Envelope cannot be stored as property
        if ("envelope".equals(sourceType) && "property".equals(targetType)) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "Enrich source type='envelope' cannot be stored in a property target. " +
                                "Use type='body' or type='custom' as the source instead.",
                        DiagnosticSeverity.Warning, "EnrichIncompatibleSourceTarget");
            }
        }
    }

    /**
     * P1-12: Throttle mediator must have a policy (inline or via key).
     */
    private void validateThrottleMediator(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        boolean hasPolicy = false;
        List<DOMNode> children = element.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                if (child instanceof DOMElement && "policy".equals(((DOMElement) child).getLocalName())) {
                    // Policy exists — check it has content (key attr or child elements)
                    DOMElement policyElem = (DOMElement) child;
                    if (policyElem.getAttribute("key") != null || hasContent(policyElem)) {
                        hasPolicy = true;
                    }
                    break;
                }
            }
        }
        if (!hasPolicy) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "Throttle mediator requires a <policy> child with either a 'key' attribute " +
                                "or inline throttle policy content.",
                        DiagnosticSeverity.Warning, "ThrottleMissingPolicy");
            }
        }
    }

    /**
     * P1-13: Clone/iterate target must have sequence or endpoint.
     */
    private void validateCloneIterateTarget(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        // Only validate <target> inside <clone> or <iterate>
        DOMNode parent = element.getParentNode();
        if (parent == null || !(parent instanceof DOMElement)) return;
        String parentName = ((DOMElement) parent).getLocalName();
        if (!"clone".equals(parentName) && !"iterate".equals(parentName)) return;

        // Check for: inline <sequence>, inline <endpoint>, sequence attr, endpoint attr
        String sequenceAttr = element.getAttribute("sequence");
        String endpointAttr = element.getAttribute("endpoint");
        if (sequenceAttr != null || endpointAttr != null) return;

        boolean hasInlineChild = false;
        List<DOMNode> children = element.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                if (child instanceof DOMElement) {
                    String childName = ((DOMElement) child).getLocalName();
                    if ("sequence".equals(childName) || "endpoint".equals(childName)) {
                        hasInlineChild = true;
                        break;
                    }
                }
            }
        }
        if (!hasInlineChild) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "Target in '" + parentName + "' mediator must have at least one of: " +
                                "inline <sequence>, inline <endpoint>, 'sequence' attribute, or 'endpoint' attribute.",
                        DiagnosticSeverity.Warning, "CloneIterateTargetEmpty");
            }
        }
    }

    /**
     * P1-15: Store mediator messageStore reference validation.
     */
    private void validateStoreMediator(DOMElement element, List<Diagnostic> diagnostics, Set<String> knownArtifacts) {
        if (knownArtifacts == null) return;
        String messageStore = element.getAttribute("messageStore");
        if (!StringUtils.isEmpty(messageStore) && !isExpression(messageStore)
                && !knownArtifacts.contains(messageStore)) {
            DOMAttr attr = element.getAttributeNode("messageStore");
            if (attr != null) {
                Range range = XMLPositionUtility.selectAttributeValue(attr);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Referenced message store '" + messageStore + "' not found in the project. " +
                                    "Ensure the message store exists or check for typos.",
                            DiagnosticSeverity.Warning, "UnresolvedMessageStoreReference");
                }
            }
        }
    }

    /**
     * P1-16: Validate mediator schema key must be a registry resource.
     */
    private void validateSchemaKey(DOMElement element, List<Diagnostic> diagnostics, Set<String> knownArtifacts) {
        // Only validate <schema> inside <validate>
        DOMNode parent = element.getParentNode();
        if (parent == null || !(parent instanceof DOMElement)
                || !"validate".equals(((DOMElement) parent).getLocalName())) {
            return;
        }
        if (knownArtifacts == null) return;
        validateRegistryKeyRef(element, "key", "validation schema", diagnostics, knownArtifacts);
    }

    /**
     * P1-17: Script mediator must have key attribute or inline content.
     */
    private void validateScriptMediator(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        String key = element.getAttribute("key");
        if (key != null) return; // Has key — valid

        // Check for inline content (text or child elements)
        if (!hasContent(element)) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "Script mediator must have either a 'key' attribute referencing an external script " +
                                "or inline script content.",
                        DiagnosticSeverity.Error, "ScriptMissingContent");
            }
        }
    }

    /**
     * P1-14: Validate call-template with-param names against template parameter declarations.
     */
    private void validateCallTemplateParams(DOMElement element, List<Diagnostic> diagnostics) {
        String target = element.getAttribute("target");
        if (StringUtils.isEmpty(target) || isExpression(target)) return;

        // P2-22: Warn about circular references
        if (cyclicArtifacts.contains(target)) {
            DOMAttr targetAttr = element.getAttributeNode("target");
            if (targetAttr != null) {
                Range range = XMLPositionUtility.selectAttributeValue(targetAttr);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Circular reference detected: template '" + target +
                                    "' has a mutual dependency that would cause infinite recursion at runtime.",
                            DiagnosticSeverity.Warning, "CircularArtifactReference");
                }
            }
        }

        String templatePath = templateFilePaths.get(target);
        if (templatePath == null) return; // Template not found (cross-ref already warns)

        // Parse template parameters from the file
        Map<String, Boolean> templateParams = parseTemplateParameters(templatePath);
        if (templateParams == null || templateParams.isEmpty()) return;

        // Collect with-param names
        Set<String> providedParams = new HashSet<>();
        List<DOMNode> children = element.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                if (child instanceof DOMElement && "with-param".equals(((DOMElement) child).getLocalName())) {
                    String paramName = ((DOMElement) child).getAttribute("name");
                    if (paramName != null) {
                        providedParams.add(paramName);
                        // Check for unknown parameter names
                        if (!templateParams.containsKey(paramName)) {
                            DOMAttr nameAttr = ((DOMElement) child).getAttributeNode("name");
                            if (nameAttr != null) {
                                Range range = XMLPositionUtility.selectAttributeValue(nameAttr);
                                if (range != null) {
                                    addDiagnostic(diagnostics, range,
                                            "Parameter '" + paramName + "' is not declared in template '" + target +
                                                    "'. Declared parameters: " + templateParams.keySet() + ".",
                                            DiagnosticSeverity.Warning, "UnknownTemplateParameter");
                                }
                            }
                        }
                    }
                }
            }
        }

        // Check for missing mandatory parameters
        for (Map.Entry<String, Boolean> param : templateParams.entrySet()) {
            if (param.getValue() && !providedParams.contains(param.getKey())) {
                Range range = XMLPositionUtility.selectStartTagName(element);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Mandatory parameter '" + param.getKey() + "' of template '" + target +
                                    "' is not provided via <with-param>.",
                            DiagnosticSeverity.Error, "MissingMandatoryTemplateParameter");
                }
            }
        }
    }

    /**
     * Parses a template file to extract parameter declarations.
     * Returns a map of parameter name -> isMandatory.
     */
    private Map<String, Boolean> parseTemplateParameters(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) return null;

            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
            org.eclipse.lemminx.commons.TextDocument textDoc = new org.eclipse.lemminx.commons.TextDocument(content, filePath);
            DOMDocument doc = org.eclipse.lemminx.dom.DOMParser.getInstance().parse(textDoc, null);
            DOMElement root = doc.getDocumentElement();
            if (root == null) return null;

            Map<String, Boolean> params = new HashMap<>();
            // Template parameters are direct children of the root <template> element
            List<DOMNode> children = root.getChildren();
            if (children != null) {
                for (DOMNode child : children) {
                    if (child instanceof DOMElement && "parameter".equals(((DOMElement) child).getLocalName())) {
                        DOMElement paramElem = (DOMElement) child;
                        String name = paramElem.getAttribute("name");
                        if (name != null) {
                            String isMandatory = paramElem.getAttribute("isMandatory");
                            params.put(name, "true".equals(isMandatory));
                        }
                    }
                }
            }
            return params;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not parse template parameters from: " + filePath, e);
            return null;
        }
    }

    /**
     * P1-19: Warn if the current document's root artifact name is duplicated in the project.
     */
    private void validateDuplicateArtifactName(DOMElement root, List<Diagnostic> diagnostics) {
        if (duplicateArtifactNames.isEmpty()) return;
        String name = root.getAttribute("name");
        if (name != null && duplicateArtifactNames.contains(name)) {
            DOMAttr nameAttr = root.getAttributeNode("name");
            if (nameAttr != null) {
                Range range = XMLPositionUtility.selectAttributeValue(nameAttr);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Artifact name '" + name + "' is defined in multiple files in this project. " +
                                    "Duplicate names cause undefined behavior at runtime.",
                            DiagnosticSeverity.Warning, "DuplicateArtifactName");
                }
            }
        }
    }

    /**
     * Detect unreachable code after terminal mediators (respond, drop, loopback).
     */
    private void validateUnreachableCode(DOMElement container, List<Diagnostic> diagnostics, DOMDocument document) {
        List<DOMNode> children = container.getChildren();
        if (children == null) return;

        String terminalMediatorName = null;
        boolean afterTerminal = false;

        for (DOMNode child : children) {
            if (!(child instanceof DOMElement)) continue;
            DOMElement childElement = (DOMElement) child;
            String childName = childElement.getLocalName();
            if (childName == null) continue;

            if (afterTerminal) {
                Range range = XMLPositionUtility.selectStartTagName(childElement);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Unreachable mediator: this '" + childName +
                                    "' will never execute because it follows a '" + terminalMediatorName +
                                    "' mediator.",
                            DiagnosticSeverity.Warning, "UnreachableCode");
                }
            } else if (TERMINAL_MEDIATORS.contains(childName)) {
                afterTerminal = true;
                terminalMediatorName = childName;
            }
        }
    }

    /**
     * Collect variable definitions from &lt;variable&gt; and &lt;property scope="default"&gt; mediators.
     */
    private void collectVariableDefinition(DOMElement element, Set<String> definedVariables) {
        String name = element.getLocalName();
        if ("variable".equals(name)) {
            String varName = element.getAttribute("name");
            String action = element.getAttribute("action");
            // action "set" or absent means variable is being defined
            if (varName != null && (action == null || "set".equals(action))) {
                definedVariables.add(varName);
            }
        } else if ("property".equals(name)) {
            // <property> with scope="default" (or no scope, which defaults to "synapse")
            // defines a variable accessible via vars.X
            String scope = element.getAttribute("scope");
            String action = element.getAttribute("action");
            if ("default".equals(scope) && (action == null || "set".equals(action))) {
                String varName = element.getAttribute("name");
                if (varName != null) {
                    definedVariables.add(varName);
                }
            }
        } else if ("foreach".equals(name) || "iterate".equals(name)) {
            // foreach/iterate implicitly define a loop counter variable
            // and the collection item is accessible in the flow
        } else if ("responseVariable".equals(name)) {
            // <responseVariable>varName</responseVariable> in connector operations and AI mediators
            String varName = getElementTextContent(element);
            if (varName != null) {
                definedVariables.add(varName);
            }
        }
    }

    /**
     * Validate that vars.X references in expression attributes refer to defined variables.
     */
    private void validateVariableReferences(DOMElement element, List<Diagnostic> diagnostics,
                                            DOMDocument document, Set<String> definedVariables) {
        // Check all attributes for ${...} expressions containing vars.X
        List<DOMAttr> attrs = element.getAttributeNodes();
        if (attrs == null) {
            return;
        }
        for (DOMAttr attr : attrs) {
            String attrValue = attr.getValue();
            if (attrValue == null || (!attrValue.contains("vars.") && !attrValue.contains("vars["))) {
                continue;
            }

            // Extract ${...} or {${...}} expressions from the attribute value
            Matcher exprMatcher = EXPRESSION_PATTERN.matcher(attrValue);
            while (exprMatcher.find()) {
                String exprContent = exprMatcher.group(1);
                if (exprContent == null) {
                    exprContent = exprMatcher.group(2); // {${...}} form
                }
                if (exprContent == null) {
                    continue;
                }

                // Find vars.X references within the expression
                Matcher varsMatcher = VARS_REF_PATTERN.matcher(exprContent);
                while (varsMatcher.find()) {
                    // Get the variable name from whichever group matched
                    String varName = varsMatcher.group(1);
                    if (varName == null) varName = varsMatcher.group(2);
                    if (varName == null) varName = varsMatcher.group(3);

                    if (varName != null && !definedVariables.contains(varName)) {
                        Range range = XMLPositionUtility.selectAttributeValue(attr);
                        if (range != null) {
                            Diagnostic d = new Diagnostic();
                            d.setRange(range);
                            d.setMessage(
                                    "Variable '" + varName + "' is referenced but not defined in this file. " +
                                            "If it is defined in a calling sequence, this warning can be ignored. " +
                                            "Otherwise, define it using <variable name=\"" + varName +
                                            "\" .../> before this point.");
                            d.setSeverity(DiagnosticSeverity.Warning);
                            d.setSource(SOURCE);
                            d.setCode("UndefinedVariable");
                            d.setData(varName);
                            diagnostics.add(d);
                        }
                    }
                }
            }
        }
    }

    /**
     * Validate cross-file references (key, target, onError attributes).
     */
    private void validateCrossReferences(DOMElement element, List<Diagnostic> diagnostics,
                                         Set<String> knownArtifacts) {
        String name = element.getLocalName();
        if (name == null) {
            return;
        }

        // Check 'key' attribute on endpoint and sequence elements (inside call/send mediators)
        if (KEY_REF_ELEMENTS.contains(name)) {
            String key = element.getAttribute("key");
            // P2-22: Circular reference check for sequence key references
            if ("sequence".equals(name) && key != null && cyclicArtifacts.contains(key)) {
                DOMAttr keyAttr = element.getAttributeNode("key");
                if (keyAttr != null) {
                    Range range = XMLPositionUtility.selectAttributeValue(keyAttr);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Circular reference detected: sequence '" + key +
                                        "' has a mutual dependency that would cause infinite recursion at runtime.",
                                DiagnosticSeverity.Warning, "CircularArtifactReference");
                    }
                }
            }
            if (!StringUtils.isEmpty(key) && !isExpression(key) && !knownArtifacts.contains(key)) {
                DOMAttr keyAttr = element.getAttributeNode("key");
                if (keyAttr != null) {
                    Range range = XMLPositionUtility.selectAttributeValue(keyAttr);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Referenced " + name + " '" + key + "' not found in the project. " +
                                        "Ensure the artifact exists or check for typos.",
                                DiagnosticSeverity.Warning, "UnresolvedArtifactReference");
                    }
                }
            }
        }

        // Check 'target' attribute on call-template
        if ("call-template".equals(name)) {
            String target = element.getAttribute("target");
            if (!StringUtils.isEmpty(target) && !isExpression(target)
                    && !knownArtifacts.contains(target)) {
                DOMAttr targetAttr = element.getAttributeNode("target");
                if (targetAttr != null) {
                    Range range = XMLPositionUtility.selectAttributeValue(targetAttr);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Referenced template '" + target + "' not found in the project. " +
                                        "Ensure the template exists or check for typos.",
                                DiagnosticSeverity.Warning, "UnresolvedArtifactReference");
                    }
                }
            }
        }

        // Check 'onError' attribute
        if (ON_ERROR_ELEMENTS.contains(name)) {
            String onError = element.getAttribute("onError");
            if (!StringUtils.isEmpty(onError) && !isExpression(onError)
                    && !knownArtifacts.contains(onError)) {
                DOMAttr onErrorAttr = element.getAttributeNode("onError");
                if (onErrorAttr != null) {
                    Range range = XMLPositionUtility.selectAttributeValue(onErrorAttr);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Referenced error sequence '" + onError + "' not found in the project. " +
                                        "Ensure the sequence exists or check for typos.",
                                DiagnosticSeverity.Warning, "UnresolvedArtifactReference");
                    }
                }
            }
        }

        // P2-28: Check throttle onAccept/onReject attribute references to sequences
        if ("throttle".equals(name)) {
            for (String attrName : new String[]{"onAccept", "onReject"}) {
                String val = element.getAttribute(attrName);
                if (!StringUtils.isEmpty(val) && !isExpression(val) && !knownArtifacts.contains(val)) {
                    DOMAttr attr = element.getAttributeNode(attrName);
                    if (attr != null) {
                        Range range = XMLPositionUtility.selectAttributeValue(attr);
                        if (range != null) {
                            addDiagnostic(diagnostics, range,
                                    "Referenced sequence '" + val + "' not found in the project. " +
                                            "Ensure the sequence exists or check for typos.",
                                    DiagnosticSeverity.Warning, "UnresolvedArtifactReference");
                        }
                    }
                }
            }
        }

        // Check 'configKey' attribute on connector operations (elements with "." in name)
        if (name.contains(".")) {
            String configKey = element.getAttribute("configKey");
            if (!StringUtils.isEmpty(configKey) && !isExpression(configKey)
                    && !knownArtifacts.contains(configKey)) {
                DOMAttr configKeyAttr = element.getAttributeNode("configKey");
                if (configKeyAttr != null) {
                    Range range = XMLPositionUtility.selectAttributeValue(configKeyAttr);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Referenced connection '" + configKey + "' not found in the project. " +
                                        "Ensure the local entry for this connection exists or check for typos.",
                                DiagnosticSeverity.Warning, "UnresolvedConfigKeyReference");
                    }
                }
            }
        }

        // Check 'key' attribute on xslt and xquery mediators (registry references)
        if ("xslt".equals(name) || "xquery".equals(name)) {
            validateRegistryKeyRef(element, "key", name + " stylesheet", diagnostics, knownArtifacts);
        }

        // Check datamapper config, inputSchema, outputSchema attributes
        if ("datamapper".equals(name)) {
            validateRegistryKeyRef(element, "config", "datamapper configuration", diagnostics, knownArtifacts);
            validateRegistryKeyRef(element, "inputSchema", "datamapper input schema", diagnostics, knownArtifacts);
            validateRegistryKeyRef(element, "outputSchema", "datamapper output schema", diagnostics, knownArtifacts);
            validateRegistryKeyRef(element, "xsltStyleSheet", "datamapper XSLT stylesheet", diagnostics,
                    knownArtifacts);
        }
    }

    /**
     * Validates a registry key reference attribute against the known artifacts index.
     */
    private void validateRegistryKeyRef(DOMElement element, String attrName, String description,
                                        List<Diagnostic> diagnostics, Set<String> knownArtifacts) {
        String value = element.getAttribute(attrName);
        if (StringUtils.isEmpty(value) || isExpression(value)) {
            return;
        }
        if (!knownArtifacts.contains(value)) {
            DOMAttr attr = element.getAttributeNode(attrName);
            if (attr != null) {
                Range range = XMLPositionUtility.selectAttributeValue(attr);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Referenced " + description + " '" + value + "' not found in the project. " +
                                    "Ensure the resource exists or check for typos.",
                            DiagnosticSeverity.Warning, "UnresolvedRegistryReference");
                }
            }
        }
    }

    /**
     * Checks if a string is a Synapse expression (${...} or {${...}}).
     */
    private boolean isExpression(String value) {
        return (value.startsWith("${") && value.endsWith("}"))
                || (value.startsWith("{${") && value.endsWith("}}"));
    }

    /**
     * Builds a set of all known artifact names and registry keys in the project.
     * Results are cached for ARTIFACT_CACHE_TTL_MS to avoid filesystem scanning on every keystroke.
     * Returns null if the project path cannot be determined.
     */
    private Set<String> buildArtifactNameIndex(DOMDocument document, CancelChecker cancelChecker) {
        String projectPath = deriveProjectPath(document);
        if (projectPath == null) {
            return null;
        }

        // Check cache first
        CachedArtifactIndex cached = artifactIndexCache.get(projectPath);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < ARTIFACT_CACHE_TTL_MS) {
            // Restore all derived state so cross-reference checks see the same
            // template paths, duplicates, and cycles as a fresh build would.
            this.templateFilePaths = cached.templateFilePaths;
            this.duplicateArtifactNames = cached.duplicateArtifactNames;
            this.cyclicArtifacts = cached.cyclicArtifacts;
            return cached.artifactNames;
        }

        Set<String> artifactNames = new HashSet<>();
        Map<String, String> templatePaths = new HashMap<>();
        Map<String, List<String>> nameToFiles = new HashMap<>();
        try {
            if (cancelChecker != null) {
                cancelChecker.checkCanceled();
            }
            NewProjectResourceFinder resourceFinder = new NewProjectResourceFinder();
            Map<String, ResourceResponse> allResources = resourceFinder.findAllResources(projectPath);
            collectResourceNames(allResources, artifactNames, templatePaths, nameToFiles);

            // Dependent-project artifacts were loaded once by SynapseLanguageService at init;
            // read through the published finder instead of re-loading on every cache miss.
            collectResourceNames(SynapseLanguageService.getLoadedDependentResources(),
                    artifactNames, templatePaths, nameToFiles);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to build artifact name index for cross-reference validation", e);
            return null;
        }

        // Build duplicate names set
        Set<String> duplicates = new HashSet<>();
        for (Map.Entry<String, List<String>> nameEntry : nameToFiles.entrySet()) {
            if (nameEntry.getValue().size() > 1) {
                duplicates.add(nameEntry.getKey());
            }
        }

        // P2-22: Build dependency map and detect direct circular references
        Set<String> cycles = detectDirectCycles(templatePaths, artifactNames);

        // Update instance-level fields and cache
        this.templateFilePaths = templatePaths;
        this.duplicateArtifactNames = duplicates;
        this.cyclicArtifacts = cycles;
        artifactIndexCache.put(projectPath, new CachedArtifactIndex(
                artifactNames, templatePaths, duplicates, cycles, System.currentTimeMillis()));
        return artifactNames;
    }

    /**
     * Walks a resource map (from either {@code findAllResources} or {@code getDependentResourcesMap})
     * and populates the artifact-name index, template-path map, duplicate-detection map, and the
     * normalized registry keys used for registry reference validation.
     */
    private void collectResourceNames(Map<String, ResourceResponse> resources,
                                      Set<String> artifactNames,
                                      Map<String, String> templatePaths,
                                      Map<String, List<String>> nameToFiles) {
        if (resources == null) {
            return;
        }
        for (Map.Entry<String, ResourceResponse> entry : resources.entrySet()) {
            String resourceType = entry.getKey();
            ResourceResponse response = entry.getValue();
            if (response == null) {
                continue;
            }
            if (response.getResources() != null) {
                for (Resource resource : response.getResources()) {
                    if (resource.getName() != null) {
                        artifactNames.add(resource.getName());
                        // Track template file paths for parameter validation
                        if (resource instanceof ArtifactResource &&
                                (resourceType.contains("emplate") || resourceType.contains("template"))) {
                            String absPath = ((ArtifactResource) resource).getAbsolutePath();
                            if (absPath != null) {
                                templatePaths.put(resource.getName(), absPath);
                            }
                        }
                        // Track all artifact names for duplicate detection
                        if (resource instanceof ArtifactResource) {
                            String absPath = ((ArtifactResource) resource).getAbsolutePath();
                            nameToFiles.computeIfAbsent(resource.getName(), k -> new ArrayList<>())
                                    .add(absPath != null ? absPath : "unknown");
                        }
                    }
                }
            }
            // Also collect registry keys for xslt/xquery/datamapper references
            if (response.getRegistryResources() != null) {
                for (Resource resource : response.getRegistryResources()) {
                    if (resource instanceof RegistryResource) {
                        String regKey = ((RegistryResource) resource).getRegistryKey();
                        if (regKey != null) {
                            artifactNames.add(regKey);
                            // Normalize: add both gov:path and gov:/path variants
                            if (regKey.contains(":") && !regKey.contains(":/")) {
                                String prefix = regKey.substring(0, regKey.indexOf(':') + 1);
                                String path = regKey.substring(regKey.indexOf(':') + 1);
                                artifactNames.add(prefix + "/" + path);
                            } else if (regKey.contains(":/")) {
                                String normalized = regKey.replace(":/", ":");
                                artifactNames.add(normalized);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * P2-22: Detect direct circular references (A->B->A) among templates and sequences.
     * Parses each artifact file to find outgoing references (call-template target, sequence key),
     * then checks for mutual references.
     */
    private Set<String> detectDirectCycles(Map<String, String> templatePaths, Set<String> allArtifactNames) {
        // Build dependency map: artifactName -> set of referenced artifact names
        Map<String, Set<String>> dependencies = new HashMap<>();

        for (Map.Entry<String, String> entry : templatePaths.entrySet()) {
            String artifactName = entry.getKey();
            String filePath = entry.getValue();
            Set<String> refs = extractArtifactReferences(filePath);
            if (refs != null && !refs.isEmpty()) {
                dependencies.put(artifactName, refs);
            }
        }

        // Check for direct cycles: if A references B and B references A
        Set<String> cyclic = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            String a = entry.getKey();
            for (String b : entry.getValue()) {
                Set<String> bRefs = dependencies.get(b);
                if (bRefs != null && bRefs.contains(a)) {
                    cyclic.add(a);
                    cyclic.add(b);
                }
            }
        }
        return cyclic;
    }

    /**
     * Parses an artifact file and extracts outgoing references (call-template target, sequence key).
     */
    private Set<String> extractArtifactReferences(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) return null;

            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
            org.eclipse.lemminx.commons.TextDocument textDoc =
                    new org.eclipse.lemminx.commons.TextDocument(content, filePath);
            DOMDocument doc = org.eclipse.lemminx.dom.DOMParser.getInstance().parse(textDoc, null);
            DOMElement root = doc.getDocumentElement();
            if (root == null) return null;

            Set<String> refs = new HashSet<>();
            collectReferences(root, refs);
            return refs;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not extract references from: " + filePath, e);
            return null;
        }
    }

    private void collectReferences(DOMNode node, Set<String> refs) {
        if (!(node instanceof DOMElement)) return;
        DOMElement element = (DOMElement) node;
        String name = element.getLocalName();

        if ("call-template".equals(name)) {
            String target = element.getAttribute("target");
            if (!StringUtils.isEmpty(target) && !isExpression(target)) {
                refs.add(target);
            }
        } else if ("sequence".equals(name)) {
            String key = element.getAttribute("key");
            if (!StringUtils.isEmpty(key) && !isExpression(key)) {
                refs.add(key);
            }
        }

        List<DOMNode> children = element.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                collectReferences(child, refs);
            }
        }
    }

    /**
     * Derives the project root path from the document URI by looking for the
     * src/main/wso2mi path segment.
     */
    private String deriveProjectPath(DOMDocument document) {
        String docUri = document.getDocumentURI();
        if (docUri == null) {
            return null;
        }
        try {
            Path filePath;
            if (docUri.startsWith("file:")) {
                filePath = Paths.get(new URI(docUri));
            } else {
                filePath = Paths.get(docUri);
            }
            String pathStr = filePath.toString();
            // Try OS-native separator first, then forward-slash for URI-style paths
            int idx = pathStr.indexOf(SRC_MAIN_WSO2MI);
            if (idx <= 0) {
                idx = pathStr.indexOf(SRC_MAIN_WSO2MI_URI);
            }
            if (idx > 0) {
                return pathStr.substring(0, idx - 1); // -1 to remove trailing separator
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not derive project path from document URI: " + docUri, e);
        }
        return null;
    }

    // ===== P2 Validations =====

    /**
     * P2-20: Bean mediator conditional required attributes.
     */
    private void validateBeanMediator(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        String action = element.getAttribute("action");
        if (action == null) return;

        Range range = XMLPositionUtility.selectStartTagName(element);
        if (range == null) return;

        switch (action) {
            case "CREATE":
                if (element.getAttribute("class") == null) {
                    addDiagnostic(diagnostics, range,
                            "Bean mediator with action='CREATE' requires a 'class' attribute specifying " +
                                    "the fully-qualified class name.",
                            DiagnosticSeverity.Error, "BeanCreateMissingClass");
                }
                break;
            case "SET_PROPERTY":
                if (element.getAttribute("property") == null) {
                    addDiagnostic(diagnostics, range,
                            "Bean mediator with action='SET_PROPERTY' requires a 'property' attribute.",
                            DiagnosticSeverity.Error, "BeanPropertyActionMissingProperty");
                } else if (element.getAttribute("value") == null) {
                    addDiagnostic(diagnostics, range,
                            "Bean mediator with action='SET_PROPERTY' requires a 'value' attribute.",
                            DiagnosticSeverity.Error, "BeanSetPropertyMissingValue");
                }
                break;
            case "GET_PROPERTY":
                if (element.getAttribute("property") == null) {
                    addDiagnostic(diagnostics, range,
                            "Bean mediator with action='GET_PROPERTY' requires a 'property' attribute.",
                            DiagnosticSeverity.Error, "BeanPropertyActionMissingProperty");
                }
                break;
        }
    }

    /**
     * P2-23: Class mediator fully-qualified name validation.
     */
    private void validateClassMediator(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        String name = element.getAttribute("name");
        if (StringUtils.isEmpty(name) || isExpression(name)) return;

        if (!FQN_PATTERN.matcher(name).matches()) {
            DOMAttr nameAttr = element.getAttributeNode("name");
            if (nameAttr != null) {
                Range range = XMLPositionUtility.selectAttributeValue(nameAttr);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Class mediator 'name' should be a fully-qualified Java class name " +
                                    "(e.g., 'com.example.MyMediator'). Got: '" + name + "'.",
                            DiagnosticSeverity.Warning, "InvalidClassFQN");
                }
            }
        }
    }

    /**
     * P2-21: Validates a regex attribute value for syntax errors.
     */
    private void validateRegexAttribute(DOMElement element, String attrName, List<Diagnostic> diagnostics) {
        String regex = element.getAttribute(attrName);
        if (StringUtils.isEmpty(regex) || isExpression(regex)) return;

        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            DOMAttr attr = element.getAttributeNode(attrName);
            if (attr != null) {
                Range range = XMLPositionUtility.selectAttributeValue(attr);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Invalid regex pattern '" + regex + "': " + e.getDescription() + ".",
                            DiagnosticSeverity.Error, "InvalidRegexPattern");
                }
            }
        }
    }

    /**
     * P2-25: Validates that a property mediator's literal value matches its declared type.
     * Returns an error message if invalid, null if valid.
     */
    private String validateTypeValueMatch(String type, String value) {
        try {
            switch (type) {
                case "INTEGER":
                    Integer.parseInt(value);
                    break;
                case "LONG":
                    Long.parseLong(value);
                    break;
                case "SHORT":
                    Short.parseShort(value);
                    break;
                case "FLOAT":
                    float f = Float.parseFloat(value);
                    if (Float.isNaN(f) || Float.isInfinite(f)) {
                        return "Value '" + value + "' is not a valid FLOAT.";
                    }
                    break;
                case "DOUBLE":
                    double d = Double.parseDouble(value);
                    if (Double.isNaN(d) || Double.isInfinite(d)) {
                        return "Value '" + value + "' is not a valid DOUBLE.";
                    }
                    break;
                case "BOOLEAN":
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        return "Value '" + value + "' is not a valid BOOLEAN. Expected 'true' or 'false'.";
                    }
                    break;
                default:
                    // STRING, OM, JSON — always valid
                    break;
            }
        } catch (NumberFormatException e) {
            return "Value '" + value + "' is not a valid " + type + ".";
        }
        return null;
    }

    /**
     * P2-29: Validate endpoint suspendOnFailure/markForSuspension config.
     */
    private void validateEndpointSuspendConfig(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        // Only validate inside endpoint context
        if (!isInsideEndpoint(element)) return;

        List<DOMNode> children = element.getChildren();
        if (children == null) return;

        for (DOMNode child : children) {
            if (!(child instanceof DOMElement)) continue;
            DOMElement childElem = (DOMElement) child;
            String childName = childElem.getLocalName();
            if (childName == null) continue;

            String textContent = getElementTextContent(childElem);
            if (StringUtils.isEmpty(textContent) || isExpression(textContent)) continue;

            if ("errorCodes".equals(childName)) {
                if (!ERROR_CODES_PATTERN.matcher(textContent).matches()) {
                    Range range = XMLPositionUtility.selectStartTagName(childElem);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Invalid error codes format: '" + textContent + "'. " +
                                        "Expected comma-separated integers (e.g., '101503, 101504').",
                                DiagnosticSeverity.Warning, "InvalidErrorCodesFormat");
                    }
                }
            } else if ("progressionFactor".equals(childName)) {
                try {
                    double factor = Double.parseDouble(textContent);
                    if (factor <= 0) {
                        Range range = XMLPositionUtility.selectStartTagName(childElem);
                        if (range != null) {
                            addDiagnostic(diagnostics, range,
                                    "Progression factor must be greater than 0. Got: " + textContent + ".",
                                    DiagnosticSeverity.Warning, "InvalidProgressionFactor");
                        }
                    }
                } catch (NumberFormatException e) {
                    Range range = XMLPositionUtility.selectStartTagName(childElem);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Progression factor must be a number greater than 0. Got: '" + textContent + "'.",
                                DiagnosticSeverity.Warning, "InvalidProgressionFactor");
                    }
                }
            }
        }
    }

    /**
     * P2-29: Validate endpoint timeout responseAction value.
     */
    private void validateEndpointTimeout(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        // Only validate <timeout> inside endpoint context, not other elements
        if (!isInsideEndpoint(element)) return;

        List<DOMNode> children = element.getChildren();
        if (children == null) return;

        for (DOMNode child : children) {
            if (!(child instanceof DOMElement)) continue;
            DOMElement childElem = (DOMElement) child;
            if ("responseAction".equals(childElem.getLocalName())) {
                String textContent = getElementTextContent(childElem);
                if (!StringUtils.isEmpty(textContent) && !isExpression(textContent)
                        && !VALID_RESPONSE_ACTIONS.contains(textContent)) {
                    Range range = XMLPositionUtility.selectStartTagName(childElem);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Invalid response action '" + textContent + "'. " +
                                        "Valid values are: 'discard', 'fault', 'never'.",
                                DiagnosticSeverity.Warning, "InvalidResponseAction");
                    }
                }
            }
        }
    }

    /**
     * P2-28: Enqueue mediator sequence reference validation.
     */
    private void validateEnqueueMediator(DOMElement element, List<Diagnostic> diagnostics, Set<String> knownArtifacts) {
        if (knownArtifacts == null) return;
        String seq = element.getAttribute("sequence");
        if (!StringUtils.isEmpty(seq) && !isExpression(seq) && !knownArtifacts.contains(seq)) {
            DOMAttr attr = element.getAttributeNode("sequence");
            if (attr != null) {
                Range range = XMLPositionUtility.selectAttributeValue(attr);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Referenced sequence '" + seq + "' not found in the project. " +
                                    "Ensure the sequence exists or check for typos.",
                            DiagnosticSeverity.Warning, "UnresolvedArtifactReference");
                }
            }
        }
    }

    /**
     * P2-28: Aggregate onComplete sequence reference validation.
     */
    private void validateOnCompleteSequenceRef(DOMElement element, List<Diagnostic> diagnostics,
                                               Set<String> knownArtifacts) {
        if (knownArtifacts == null) return;
        // Only validate <onComplete> inside <aggregate>
        DOMNode parent = element.getParentNode();
        if (!(parent instanceof DOMElement) || !"aggregate".equals(((DOMElement) parent).getLocalName())) {
            return;
        }
        String seq = element.getAttribute("sequence");
        if (!StringUtils.isEmpty(seq) && !isExpression(seq) && !knownArtifacts.contains(seq)) {
            DOMAttr attr = element.getAttributeNode("sequence");
            if (attr != null) {
                Range range = XMLPositionUtility.selectAttributeValue(attr);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Referenced sequence '" + seq + "' not found in the project. " +
                                    "Ensure the sequence exists or check for typos.",
                            DiagnosticSeverity.Warning, "UnresolvedArtifactReference");
                }
            }
        }
    }

    /**
     * P2-27: Data service call serviceName validation.
     */
    private void validateDataServiceCall(DOMElement element, List<Diagnostic> diagnostics,
                                         Set<String> knownArtifacts) {
        if (knownArtifacts == null) return;
        String serviceName = element.getAttribute("serviceName");
        if (!StringUtils.isEmpty(serviceName) && !isExpression(serviceName)
                && !knownArtifacts.contains(serviceName)) {
            DOMAttr attr = element.getAttributeNode("serviceName");
            if (attr != null) {
                Range range = XMLPositionUtility.selectAttributeValue(attr);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Referenced data service '" + serviceName + "' not found in the project. " +
                                    "Ensure the data service exists or check for typos.",
                            DiagnosticSeverity.Warning, "UnresolvedDataServiceReference");
                }
            }
        }
    }

    /**
     * Checks if an element is inside an endpoint context (http, address, wsdl, default, loadbalance, failover).
     */
    private boolean isInsideEndpoint(DOMElement element) {
        DOMNode current = element.getParentNode();
        while (current != null) {
            if (current instanceof DOMElement) {
                String name = ((DOMElement) current).getLocalName();
                if ("http".equals(name) || "address".equals(name) || "wsdl".equals(name)
                        || "default".equals(name) || "loadbalance".equals(name)
                        || "failover".equals(name) || "endpoint".equals(name)) {
                    return true;
                }
            }
            current = current.getParentNode();
        }
        return false;
    }

    /**
     * Gets the trimmed text content of an element (direct text children only).
     */
    private String getElementTextContent(DOMElement element) {
        List<DOMNode> children = element.getChildren();
        if (children == null) return null;
        StringBuilder sb = new StringBuilder();
        for (DOMNode child : children) {
            if (child.isText()) {
                String text = child.getTextContent();
                if (text != null) {
                    sb.append(text);
                }
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private boolean hasChildElements(DOMElement element) {
        List<DOMNode> children = element.getChildren();
        if (children == null) return false;
        for (DOMNode child : children) {
            if (child instanceof DOMElement) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an element has child elements OR non-whitespace text content (e.g., inline JSON).
     */
    private boolean hasContent(DOMElement element) {
        if (hasChildElements(element)) {
            return true;
        }
        // Check for text content (e.g., inline JSON like {"key": "value"})
        List<DOMNode> children = element.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                if (child.isText() || child.isCDATA()) {
                    String text = child.getTextContent();
                    if (!StringUtils.isBlank(text)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * P3-31: Rewrite mediator action requires value/xpath/regex depending on type.
     * When type is set/append/prepend (or absent, defaults to set): requires value or xpath.
     * When type is replace: requires value or xpath or regex.
     * When type is remove: no value/xpath/regex needed.
     */
    private void validateRewriteAction(DOMElement element, List<Diagnostic> diagnostics,
                                        DOMDocument document) {
        // Only validate <action> inside <rewriterule>
        DOMNode parent = element.getParentNode();
        if (parent == null || !(parent instanceof DOMElement)
                || !"rewriterule".equals(((DOMElement) parent).getLocalName())) {
            return;
        }
        String type = element.getAttribute("type");
        // type="remove" doesn't need value/xpath/regex
        if ("remove".equals(type)) {
            return;
        }
        String value = element.getAttribute("value");
        String xpath = element.getAttribute("xpath");
        String regex = element.getAttribute("regex");

        boolean hasValueOrXpath = value != null || xpath != null;
        if ("replace".equals(type)) {
            // replace can use value, xpath, or regex
            if (!hasValueOrXpath && regex == null) {
                Range range = XMLPositionUtility.selectStartTagName(element);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Rewrite action with type 'replace' requires at least one of 'value', 'xpath', or 'regex' attribute.",
                            DiagnosticSeverity.Warning, "RewriteActionMissingValue");
                }
            }
        } else {
            // set, append, prepend (or absent = set)
            if (!hasValueOrXpath) {
                String typeLabel = type != null ? type : "set";
                Range range = XMLPositionUtility.selectStartTagName(element);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Rewrite action with type '" + typeLabel + "' requires a 'value' or 'xpath' attribute.",
                            DiagnosticSeverity.Warning, "RewriteActionMissingValue");
                }
            }
        }
    }

    /**
     * P3-47: Validate mediator on-fail must have at least one child mediator.
     * The XSD enforces this via mediatorList minOccurs="1", but this is a safety net.
     */
    private void validateOnFail(DOMElement element, List<Diagnostic> diagnostics,
                                 DOMDocument document) {
        // Only validate <on-fail> inside <validate>
        DOMNode parent = element.getParentNode();
        if (parent == null || !(parent instanceof DOMElement)
                || !"validate".equals(((DOMElement) parent).getLocalName())) {
            return;
        }
        // Check for at least one child element (mediator)
        boolean hasChildElement = false;
        List<DOMNode> children = element.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                if (child instanceof DOMElement) {
                    hasChildElement = true;
                    break;
                }
            }
        }
        if (!hasChildElement) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "The 'on-fail' element must contain at least one mediator to handle validation failures.",
                        DiagnosticSeverity.Warning, "ValidateOnFailEmpty");
            }
        }
    }

    /**
     * Emit Hint-level suggestions for newer MI 4.4.0+ patterns.
     * Only called when is440Plus is true.
     */
    private void emitNewPatternHints(DOMElement element, String name, List<Diagnostic> diagnostics) {
        switch (name) {
            case "property": {
                // Skip <property> inside <log> (those are log properties)
                DOMNode parent = element.getParentNode();
                if (parent instanceof DOMElement && "log".equals(((DOMElement) parent).getLocalName())) {
                    break;
                }
                // Skip runtime properties that need scope/action (they require <property>, not <variable>)
                if (element.getAttribute("scope") != null || element.getAttribute("action") != null) {
                    break;
                }
                Range range = XMLPositionUtility.selectStartTagName(element);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Consider using <variable> instead of <property> for new code. " +
                                    "The <variable> mediator is the preferred approach in MI 4.4.0+.",
                            DiagnosticSeverity.Hint, "PreferVariable");
                }
                break;
            }
            case "log": {
                String level = element.getAttribute("level");
                if (level != null) {
                    Range range = XMLPositionUtility.selectStartTagName(element);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Consider using <log category=\"...\"> with <message> instead of " +
                                        "'level' attribute for new code. The 'category' + <message> pattern " +
                                        "is the preferred approach in MI 4.4.0+.",
                                DiagnosticSeverity.Hint, "PreferLogCategory");
                    }
                }
                break;
            }
            case "clone": {
                Range range = XMLPositionUtility.selectStartTagName(element);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Consider using <scatter-gather> instead of <clone> for new code. " +
                                    "The scatter-gather mediator is the preferred approach in MI 4.4.0+.",
                            DiagnosticSeverity.Hint, "PreferScatterGather");
                }
                break;
            }
            case "iterate": {
                Range range = XMLPositionUtility.selectStartTagName(element);
                if (range != null) {
                    addDiagnostic(diagnostics, range,
                            "Consider using <foreach> instead of <iterate> for new code. " +
                                    "The forEach mediator is the preferred approach in MI 4.4.0+.",
                            DiagnosticSeverity.Hint, "PreferForEach");
                }
                break;
            }
            case "filter": {
                String source = element.getAttribute("source");
                String regex = element.getAttribute("regex");
                String xpath = element.getAttribute("xpath");
                // Only hint when using source+regex pattern (not when xpath is already used)
                if (source != null && regex != null && xpath == null) {
                    Range range = XMLPositionUtility.selectStartTagName(element);
                    if (range != null) {
                        addDiagnostic(diagnostics, range,
                                "Consider using <filter xpath=\"${...}\"> with a Synapse expression " +
                                        "instead of 'source' + 'regex' for new code.",
                                DiagnosticSeverity.Hint, "PreferFilterXpath");
                    }
                }
                break;
            }
        }
    }

    /**
     * Validate that a <variable> mediator does not have both 'value' and 'expression' attributes.
     */
    private void validateVariableMediator(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        String value = element.getAttribute("value");
        String expression = element.getAttribute("expression");
        if (value != null && expression != null) {
            String varName = element.getAttribute("name");
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "Variable" + (varName != null ? " '" + varName + "'" : "") +
                                " has both 'value' and 'expression' attributes. " +
                                "Only 'expression' will be evaluated; 'value' will be ignored.",
                        DiagnosticSeverity.Warning, "BothValueAndExpression");
            }
        }
    }

    /**
     * Validate that a <foreach> mediator has either 'collection' or 'expression' attribute.
     */
    private void validateForEachMediator(DOMElement element, List<Diagnostic> diagnostics, DOMDocument document) {
        String collection = element.getAttribute("collection");
        String expression = element.getAttribute("expression");
        if (collection == null && expression == null) {
            Range range = XMLPositionUtility.selectStartTagName(element);
            if (range != null) {
                addDiagnostic(diagnostics, range,
                        "ForEach mediator requires either a 'collection' or 'expression' attribute " +
                                "to specify what to iterate over.",
                        DiagnosticSeverity.Error, "ForEachMissingCollectionOrExpression");
            }
        }
    }

    /**
     * Validate that an <api> does not have duplicate resource definitions
     * (same uri-template/url-mapping AND overlapping methods).
     */
    private void validateApiDuplicateResources(DOMElement element, List<Diagnostic> diagnostics,
                                                DOMDocument document) {
        List<DOMNode> children = element.getChildren();
        if (children == null) {
            return;
        }
        // Collect resource definitions: key = (path, normalizedMethods)
        Map<String, List<DOMElement>> resourceMap = new HashMap<>();
        for (DOMNode child : children) {
            if (!(child instanceof DOMElement) || !"resource".equals(((DOMElement) child).getLocalName())) {
                continue;
            }
            DOMElement resource = (DOMElement) child;
            String path = resource.getAttribute("uri-template");
            if (path == null) {
                path = resource.getAttribute("url-mapping");
            }
            if (path == null) {
                continue; // No path to compare — already flagged by ResourceMissingUriTemplateOrUrlMapping
            }
            String methods = resource.getAttribute("methods");
            // Normalize methods: sort alphabetically for consistent comparison
            String normalizedMethods = "";
            if (methods != null) {
                String[] parts = methods.trim().split("\\s+");
                Arrays.sort(parts);
                normalizedMethods = String.join(" ", parts);
            }
            String key = path + "|" + normalizedMethods;
            resourceMap.computeIfAbsent(key, k -> new ArrayList<>()).add(resource);
        }
        // Flag duplicates
        for (Map.Entry<String, List<DOMElement>> entry : resourceMap.entrySet()) {
            List<DOMElement> resources = entry.getValue();
            if (resources.size() > 1) {
                // Flag all duplicates (including the first, so the user sees both)
                for (DOMElement resource : resources) {
                    Range range = XMLPositionUtility.selectStartTagName(resource);
                    if (range != null) {
                        String path = resource.getAttribute("uri-template");
                        if (path == null) {
                            path = resource.getAttribute("url-mapping");
                        }
                        String methods = resource.getAttribute("methods");
                        addDiagnostic(diagnostics, range,
                                "Duplicate API resource: another resource with the same " +
                                        (path != null ? "path '" + path + "'" : "path") +
                                        (methods != null ? " and methods '" + methods + "'" : "") +
                                        " already exists in this API.",
                                DiagnosticSeverity.Warning, "DuplicateResourceUriTemplate");
                    }
                }
            }
        }
    }

    private void addDiagnostic(List<Diagnostic> diagnostics, Range range, String message,
                               DiagnosticSeverity severity, String code) {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(range);
        diagnostic.setMessage(message);
        diagnostic.setSeverity(severity);
        diagnostic.setSource(SOURCE);
        diagnostic.setCode(code);
        diagnostics.add(diagnostic);
    }

    /**
     * Cache entry for the artifact name index. Stores every piece of derived state
     * that {@link #buildArtifactNameIndex} writes to instance fields, so cache hits
     * restore the full picture instead of leaving stale data from a prior project.
     */
    private static class CachedArtifactIndex {
        final Set<String> artifactNames;
        final Map<String, String> templateFilePaths;
        final Set<String> duplicateArtifactNames;
        final Set<String> cyclicArtifacts;
        final long timestamp;

        CachedArtifactIndex(Set<String> artifactNames,
                            Map<String, String> templateFilePaths,
                            Set<String> duplicateArtifactNames,
                            Set<String> cyclicArtifacts,
                            long timestamp) {
            this.artifactNames = artifactNames;
            this.templateFilePaths = templateFilePaths;
            this.duplicateArtifactNames = duplicateArtifactNames;
            this.cyclicArtifacts = cyclicArtifacts;
            this.timestamp = timestamp;
        }
    }
}
