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

package org.eclipse.lemminx.customservice.synapse.expression.pojo;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.LineIndentInfo;
import org.eclipse.lemminx.extensions.contentmodel.utils.XMLGenerator;
import org.eclipse.lemminx.services.extensions.completion.ICompletionRequest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class ExpressionCompletionRequest implements ICompletionRequest {

    private final DOMDocument xmlDocument;
    private final Position position;
    private int offset;

    public ExpressionCompletionRequest(DOMDocument xmlDocument, Position position) {

        this.xmlDocument = xmlDocument;
        this.position = position;
    }

    public ExpressionCompletionRequest(DOMDocument xmlDocument, Position position, int offset) {

        this.xmlDocument = xmlDocument;
        this.position = position;
        this.offset = offset;
    }

    @Override
    public int getOffset() {

        return offset;
    }

    @Override
    public Position getPosition() {

        return position;
    }

    @Override
    public DOMNode getNode() {

        throw new UnsupportedOperationException();
    }

    @Override
    public DOMElement getParentElement() {

        throw new UnsupportedOperationException();
    }

    @Override
    public DOMDocument getXMLDocument() {

        return xmlDocument;
    }

    @Override
    public String getCurrentTag() {

        throw new UnsupportedOperationException();
    }

    @Override
    public DOMAttr getCurrentAttribute() {

        throw new UnsupportedOperationException();
    }

    @Override
    public String getCurrentAttributeName() {

        throw new UnsupportedOperationException();
    }

    @Override
    public LineIndentInfo getLineIndentInfo() throws BadLocationException {

        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getComponent(Class clazz) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canSupportMarkupKind(String kind) {

        return false;
    }

    @Override
    public SharedSettings getSharedSettings() {

        return null;
    }

    @Override
    public Range getReplaceRange() {

        return null;
    }

    @Override
    public Range getReplaceRangeForTagName() {

        return null;
    }

    @Override
    public XMLGenerator getXMLGenerator() throws BadLocationException {

        return null;
    }

    @Override
    public String getFilterForStartTagName(String tagName) {

        return null;
    }

    @Override
    public String getInsertAttrValue(String value) {

        return null;
    }

    @Override
    public boolean isCompletionSnippetsSupported() {

        return false;
    }

    @Override
    public boolean isAutoCloseTags() {

        return false;
    }

    @Override
    public boolean isResolveDocumentationSupported() {

        return false;
    }

    @Override
    public boolean isResolveAdditionalTextEditsSupported() {

        return false;
    }

    @Override
    public InsertTextFormat getInsertTextFormat() {

        return null;
    }
}
