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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.HashMap;
import java.util.List;

public class STNode {

    TagRanges range;
    Spaces spaces;
    boolean hasTextNode;
    String textNode;
    boolean selfClosed;
    String tag;
    HashMap<String, String> namespaces;

    public STNode() {

        this.namespaces = new HashMap<>();
    }

    public void elementNode(DOMElement node) {

        this.selfClosed = node.isSelfClosed();
        this.tag = node.getTagName();
        this.range = findRange(node);
        DOMNode firstChild = node.getFirstChild();
        if (firstChild != null && firstChild.isText()) {
            this.hasTextNode = firstChild.isText();
            this.textNode = Utils.unescapeXml(firstChild.getTextContent());
        }
        namespaces = new HashMap<>();
        populateNamespaces(node);
    }

    private TagRanges findRange(DOMElement node) {

        int startTagOpenOffset = node.getStart();
        int startTagCloseOffset = selfClosed ? node.getEnd() : node.getStartTagCloseOffset() + 1;
        int endTagOpenOffset = node.getEndTagOpenOffset();
        int endTagCloseOffset = node.getEnd();

        DOMDocument document = node.getOwnerDocument();
        Position startTagOpenPosition = null;
        Position startTagClosePosition = null;
        Position endTagOpenPosition = null;
        Position endTagClosePosition = null;
        try {
            startTagOpenPosition = document.positionAt(startTagOpenOffset);
            startTagClosePosition = document.positionAt(startTagCloseOffset);
            endTagOpenPosition = document.positionAt(endTagOpenOffset);
            endTagClosePosition = document.positionAt(endTagCloseOffset);
        } catch (BadLocationException e) {
        }

        Range startTagRange = createRange(startTagOpenPosition, startTagClosePosition);
        Range endTagRange = createRange(endTagOpenPosition, endTagClosePosition);
        TagRanges range = new TagRanges(startTagRange, endTagRange);

        if (!node.isOrphanEndTag()) {
            calculateLeadingAndTrailingSpaces(node, range);
        }
        return range;
    }

    private void calculateLeadingAndTrailingSpaces(DOMElement node, TagRanges range) {

        TagSpaces startTagSpaces = calculateStartTagSpaces(node, range);
        TagSpaces endTagSpaces = calculateEndTagSpaces(node, range);
        this.spaces = new Spaces(startTagSpaces, endTagSpaces);
    }

    private TagSpaces calculateStartTagSpaces(DOMElement node, TagRanges range) {

        Space leadingSpace = calculateStartTagLeadingSpaces(node, range.getStartTagRange());
        Space trailingSpace = calculateStartTagTrailingSpaces(node, range.getStartTagRange());
        TagSpaces startTagSpaces = new TagSpaces(leadingSpace, trailingSpace);
        return startTagSpaces;
    }

    private Space calculateStartTagLeadingSpaces(DOMElement node, Range range) {

        DOMDocument document = node.getOwnerDocument();
        Space space = null;
        int startOffsetOfLeadingSpaces;
        if (node.getPreviousNonTextSibling() != null) {
            DOMNode previousNonTextSibling = node.getPreviousNonTextSibling();
            startOffsetOfLeadingSpaces = previousNonTextSibling.getEnd();
        } else if (node.getParentElement() != null) {
            DOMElement parentElement = node.getParentElement();
            startOffsetOfLeadingSpaces = parentElement.getStartTagCloseOffset() + 1;
        } else {
            startOffsetOfLeadingSpaces = 0;
        }
        try {
            Position startLeadingSpacesPosition = document.positionAt(startOffsetOfLeadingSpaces);
            Position endLeadingSpacesPosition = range.getStart();
            Range spaceRange = createRange(startLeadingSpacesPosition, endLeadingSpacesPosition);
            String spaceText = getTextInRange(document, startLeadingSpacesPosition, endLeadingSpacesPosition);
            space = new Space(spaceText, spaceRange);
        } catch (BadLocationException e) {
        }
        return space;
    }

    private Space calculateStartTagTrailingSpaces(DOMElement node, Range range) {

        DOMDocument document = node.getOwnerDocument();
        Space space = null;
        int endOffsetOfTrailingSpaces;
        if (hasChildNodes(node)) {
            DOMNode firstChild = node.getFirstChild();
            endOffsetOfTrailingSpaces = firstChild.getStart();
        } else if (!node.isSelfClosed()) {
            endOffsetOfTrailingSpaces = node.getEndTagOpenOffset();
        } else if (node.getNextSibling() != null) {
            DOMNode nextSibling = node.getNextSibling();
            endOffsetOfTrailingSpaces = nextSibling.getStart();
        } else if (node.getParentElement() != null) {
            endOffsetOfTrailingSpaces = node.getParentElement().getEndTagOpenOffset();
        } else {
            endOffsetOfTrailingSpaces = node.getOwnerDocument().getEnd();
        }
        try {
            Position startTrailingSpacesPosition = range.getEnd();
            Position endTrailingSpacesPosition = document.positionAt(endOffsetOfTrailingSpaces);
            Range spaceRange = createRange(startTrailingSpacesPosition, endTrailingSpacesPosition);
            String spaceText = getTextInRange(document, startTrailingSpacesPosition, endTrailingSpacesPosition);
            space = new Space(spaceText, spaceRange);
        } catch (BadLocationException e) {
        }
        return space;
    }

    private TagSpaces calculateEndTagSpaces(DOMElement node, TagRanges range) {

        if (node.isSelfClosed() || !node.hasEndTag()) {
            return null;
        }

        Space leadingSpace = calculateEndTagLeadingSpaces(node, range.getEndTagRange());
        Space trailingSpace = calculateEndTagTrailingSpaces(node, range.getEndTagRange());
        TagSpaces endTagSpaces = new TagSpaces(leadingSpace, trailingSpace);
        return endTagSpaces;
    }

    private Space calculateEndTagLeadingSpaces(DOMElement node, Range range) {

        DOMDocument document = node.getOwnerDocument();
        Space space = null;
        int startOffsetOfLeadingSpaces;
        if (hasChildNodes(node)) {
            DOMNode lastChild = node.getLastChild();
            startOffsetOfLeadingSpaces = lastChild.getEnd();
        } else if (!node.isSelfClosed()) {
            startOffsetOfLeadingSpaces = node.getStartTagCloseOffset() + 1;
        } else if (node.getPreviousNonTextSibling() != null) {
            DOMNode previousNonTextSibling = node.getPreviousNonTextSibling();
            startOffsetOfLeadingSpaces = previousNonTextSibling.getEnd();
        } else if (node.getParentElement() != null) {
            DOMElement parentElement = node.getParentElement();
            startOffsetOfLeadingSpaces = parentElement.getEndTagCloseOffset() + 1;
        } else {
            startOffsetOfLeadingSpaces = 0;
        }
        try {
            Position startLeadingSpacesPosition = document.positionAt(startOffsetOfLeadingSpaces);
            Position endLeadingSpacesPosition = range.getStart();
            Range spaceRange = createRange(startLeadingSpacesPosition, endLeadingSpacesPosition);
            String spaceText = getTextInRange(document, startLeadingSpacesPosition, endLeadingSpacesPosition);
            space = new Space(spaceText, spaceRange);
        } catch (BadLocationException e) {
        }
        return space;
    }

    private Space calculateEndTagTrailingSpaces(DOMElement node, Range range) {

        DOMDocument document = node.getOwnerDocument();
        Space space = null;
        int endOffsetOfTrailingSpaces;

        if (node.getNextSibling() != null) {
            endOffsetOfTrailingSpaces = node.getNextSibling().getStart();
        } else if (node.getParentElement() != null) {
            endOffsetOfTrailingSpaces = node.getParentElement().getEndTagOpenOffset();
        } else {
            endOffsetOfTrailingSpaces = node.getOwnerDocument().getEnd();
        }
        try {
            Position startTrailingSpacesPosition = range.getEnd();
            Position endTrailingSpacesPosition = document.positionAt(endOffsetOfTrailingSpaces);
            Range spaceRange = createRange(startTrailingSpacesPosition, endTrailingSpacesPosition);
            String spaceText = getTextInRange(document, startTrailingSpacesPosition, endTrailingSpacesPosition);
            space = new Space(spaceText, spaceRange);
        } catch (BadLocationException e) {
        }
        return space;
    }

    private boolean hasChildNodes(DOMElement node) {

        DOMElement firstChild = Utils.getFirstElement(node);
        return firstChild != null;
    }

    private void populateNamespaces(DOMElement node) {

        List<DOMAttr> attributes = node.getAttributeNodes();
        if (attributes != null) {
            for (DOMAttr attribute : attributes) {
                if (attribute.getName().startsWith("xmlns")) {
                    namespaces.put(attribute.getName(), attribute.getValue());
                }
            }
        }
    }

    private String getTextInRange(DOMDocument document, Position startTrailingSpacesPosition,
                                  Position endTrailingSpacesPosition) throws BadLocationException {

        String text = document.getText();
        int startOffset = document.offsetAt(startTrailingSpacesPosition);
        int endOffset = document.offsetAt(endTrailingSpacesPosition);

        if (text != null) {
            return text.substring(startOffset, endOffset);
        }
        return null;
    }

    private Range createRange(Position start, Position end) {

        if (start != null && end != null) {
            return new Range(start, end);
        }
        return null;
    }

    public void addNamespace(String prefix, String uri) {

        namespaces.put(prefix, uri);
    }

    public TagRanges getRange() {

        return range;
    }

    public String getTag() {

        return tag;
    }

    public boolean isSelfClosed() {

        return selfClosed;
    }

    public String getTextNode() {

        return textNode;
    }

    public void setTextNode(String textNode) {

        this.textNode = textNode;
    }

    public void setTag(String tag) {

        this.tag = tag;
    }

    public HashMap<String, String> getNamespaces() {

        return namespaces;
    }
}
