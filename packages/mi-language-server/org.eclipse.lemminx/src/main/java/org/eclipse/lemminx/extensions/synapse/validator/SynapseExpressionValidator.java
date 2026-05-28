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

package org.eclipse.lemminx.extensions.synapse.validator;

import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.XMLErrorReporter;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLComponent;
import org.apache.xerces.xni.parser.XMLComponentManager;
import org.apache.xerces.xni.parser.XMLConfigurationException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLDocumentSource;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionError;
import org.eclipse.lemminx.customservice.synapse.expression.ExpressionValidator;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;
import org.eclipse.lemminx.extensions.xerces.xmlmodel.msg.XMLModelMessageFormatter;

import java.nio.file.Path;
import java.util.List;

public class SynapseExpressionValidator implements XMLComponent, XMLDocumentFilter {

    public static final String INVALID_SYNAPSE_EXPRESSION_IN_ATTRIBUTE = "InvalidSynapseExpressionInAttribute";
    public static final String INVALID_EXPRESSION_IN_CONTENT = "InvalidSynapseExpressionInContent";
    public static final String ERROR_REPORTER = Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    private XMLErrorReporter errorReporterForXML;
    private XMLLocator locator;
    private XMLDocumentHandler documentHandler;
    private XMLDocumentSource documentSource;
    private boolean isValidationEnabled;

    public void startDocument(XMLLocator locator, String encoding, NamespaceContext namespaceContext,
                              Augmentations augs) throws XNIException {

        setLocator(locator);
        if (isFileInArtifacts(locator.getBaseSystemId())) {
            isValidationEnabled = true;
        }
        if (documentHandler != null) {
            documentHandler.startDocument(locator, encoding, namespaceContext, augs);
        }
    }

    private boolean isFileInArtifacts(String baseSystemId) {

        return baseSystemId.contains(TryOutConstants.PROJECT_ARTIFACT_PATH.toString());
    }

    @Override
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {

        if (documentHandler != null) {
            documentHandler.xmlDecl(version, encoding, standalone, augs);
        }
    }

    @Override
    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs)
            throws XNIException {

        if (documentHandler != null) {
            documentHandler.doctypeDecl(rootElement, publicId, systemId, augs);
        }
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {

        if (documentHandler != null) {
            documentHandler.comment(text, augs);
        }
    }

    @Override
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {

        if (documentHandler != null) {
            documentHandler.processingInstruction(target, data, augs);
        }
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {

        if (isValidationEnabled) {
            validateElementAttributes(attributes);
        }
        if (documentHandler != null) {
            documentHandler.startElement(element, attributes, augs);
        }
    }

    private void validateElementAttributes(XMLAttributes attributes) {

        for (int i = 0; i < attributes.getLength(); i++) {
            String value = attributes.getValue(i);
            if (isExpression(value)) {
                String expression = extractExpression(value);
                List<ExpressionError> errors = getExpressionSyntaxErrors(expression);
                if (!errors.isEmpty()) {
                    for (ExpressionError error : errors) {
                        short severity = error.isWarning()
                                ? XMLErrorReporter.SEVERITY_WARNING
                                : XMLErrorReporter.SEVERITY_ERROR;
                        errorReporterForXML.reportError(locator, XMLModelMessageFormatter.XML_MODEL_DOMAIN,
                                INVALID_SYNAPSE_EXPRESSION_IN_ATTRIBUTE,
                                new Object[]{attributes.getLocalName(i), error.getFullMessage(), error},
                                severity);
                    }
                }
            }
        }
    }

    private boolean isExpression(String text) {

        return (text.startsWith("${") && text.endsWith("}")) || (text.startsWith("{${") && text.endsWith("}}"));
    }

    private String extractExpression(String text) {

        if (text.startsWith("{${") && text.endsWith("}}")) {
            return text.substring(3, text.length() - 2);
        } else if (text.startsWith("${") && text.endsWith("}")) {
            return text.substring(2, text.length() - 1);
        }
        return text;
    }

    private List<ExpressionError> getExpressionSyntaxErrors(String expression) {

        return ExpressionValidator.validate(expression);
    }

    public void setLocator(XMLLocator locator) {

        this.locator = locator;
    }

    @Override
    public void reset(XMLComponentManager componentManager) throws XMLConfigurationException {

        // Get error reporter for XML.
        try {
            isValidationEnabled = false;
            errorReporterForXML = (XMLErrorReporter) componentManager.getProperty(ERROR_REPORTER);
        } catch (XMLConfigurationException e) {
            errorReporterForXML = null;
        }
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {

        if (isValidationEnabled) {
            validateElementAttributes(attributes);
        }
        if (documentHandler != null) {
            documentHandler.emptyElement(element, attributes, augs);
        }
    }

    @Override
    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs)
            throws XNIException {

        if (documentHandler != null) {
            documentHandler.startGeneralEntity(name, identifier, encoding, augs);
        }
    }

    @Override
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {

        if (documentHandler != null) {
            documentHandler.textDecl(version, encoding, augs);
        }
    }

    @Override
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {

        if (documentHandler != null) {
            documentHandler.endGeneralEntity(name, augs);
        }
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {

        if (isValidationEnabled) {
            String xmlString = text.toString();
            if (isExpression(xmlString)) {
                String expression = extractExpression(xmlString);
                List<ExpressionError> errors = getExpressionSyntaxErrors(expression);
                if (!errors.isEmpty()) {
                    for (ExpressionError error : errors) {
                        short severity = error.isWarning()
                                ? XMLErrorReporter.SEVERITY_WARNING
                                : XMLErrorReporter.SEVERITY_ERROR;
                        errorReporterForXML.reportError(locator, XMLModelMessageFormatter.XML_MODEL_DOMAIN,
                                INVALID_EXPRESSION_IN_CONTENT, new Object[]{xmlString, error.getFullMessage(), error},
                                severity);
                    }
                }
            }
        }
        if (documentHandler != null) {
            documentHandler.characters(text, augs);
        }
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {

        if (documentHandler != null) {
            documentHandler.ignorableWhitespace(text, augs);
        }
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {

        if (documentHandler != null) {
            documentHandler.endElement(element, augs);
        }
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {

        if (documentHandler != null) {
            documentHandler.startCDATA(augs);
        }
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {

        if (documentHandler != null) {
            documentHandler.endCDATA(augs);
        }
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {

        if (documentHandler != null) {
            documentHandler.endDocument(augs);
        }
    }

    @Override
    public XMLDocumentSource getDocumentSource() {

        return documentSource;
    }

    @Override
    public void setDocumentSource(XMLDocumentSource source) {

        documentSource = source;
    }

    @Override
    public void setDocumentHandler(XMLDocumentHandler handler) {

        documentHandler = handler;
    }

    @Override
    public XMLDocumentHandler getDocumentHandler() {

        return documentHandler;
    }

    @Override
    public String[] getRecognizedFeatures() {

        return null;
    }

    @Override
    public void setFeature(String featureId, boolean state) throws XMLConfigurationException {

    }

    @Override
    public String[] getRecognizedProperties() {

        return null;
    }

    @Override
    public void setProperty(String propertyId, Object value) throws XMLConfigurationException {

    }

    @Override
    public Boolean getFeatureDefault(String featureId) {

        return null;
    }

    @Override
    public Object getPropertyDefault(String propertyId) {

        return null;
    }
}
