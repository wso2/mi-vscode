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

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action handler that removes an element along with its surrounding
 * whitespace to avoid leaving blank lines. Optionally supports removing
 * all subsequent sibling elements (for "remove all unreachable" variant).
 */
public class RemoveElementCodeAction implements ICodeActionParticipant {

    private final String titlePrefix;
    private final boolean offerRemoveAllSiblings;

    public RemoveElementCodeAction(String titlePrefix, boolean offerRemoveAllSiblings) {
        this.titlePrefix = titlePrefix;
        this.offerRemoveAllSiblings = offerRemoveAllSiblings;
    }

    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions,
                             CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();
        try {
            int offset = document.offsetAt(diagnostic.getRange().getStart());
            DOMNode node = document.findNodeAt(offset);
            if (!(node instanceof DOMElement)) {
                return;
            }
            DOMElement element = (DOMElement) node;
            String text = document.getText();

            // Single element removal
            Range singleRange = computeRemovalRange(element, text, document);
            String tagName = element.getLocalName();
            if (tagName == null) {
                tagName = element.getTagName();
            }
            codeActions.add(CodeActionFactory.remove(
                    titlePrefix + " '" + tagName + "'",
                    singleRange, document.getTextDocument(), diagnostic));

            // "Remove all" variant: remove this element and all following siblings
            if (offerRemoveAllSiblings) {
                DOMNode parent = element.getParentNode();
                if (parent instanceof DOMElement) {
                    List<DOMNode> siblings = parent.getChildren();
                    int idx = siblings.indexOf(element);
                    if (idx >= 0 && idx < siblings.size() - 1) {
                        // Find the last sibling element (skip text nodes at the end)
                        DOMNode lastSibling = null;
                        for (int i = siblings.size() - 1; i >= idx; i--) {
                            if (siblings.get(i) instanceof DOMElement) {
                                lastSibling = siblings.get(i);
                                break;
                            }
                        }
                        if (lastSibling != null && lastSibling != element) {
                            int bulkStart = expandStartBackwards(element.getStart(), text);
                            int bulkEnd = expandEndForwards(lastSibling.getEnd(), text);
                            Range bulkRange = new Range(
                                    document.positionAt(bulkStart),
                                    document.positionAt(bulkEnd));
                            codeActions.add(CodeActionFactory.remove(
                                    "Remove all unreachable mediators",
                                    bulkRange, document.getTextDocument(), diagnostic));
                        }
                    }
                }
            }
        } catch (BadLocationException e) {
            // ignore
        }
    }

    private Range computeRemovalRange(DOMElement element, String text, DOMDocument document)
            throws BadLocationException {
        int start = expandStartBackwards(element.getStart(), text);
        int end = expandEndForwards(element.getEnd(), text);
        return new Range(document.positionAt(start), document.positionAt(end));
    }

    /**
     * Walk backwards from the element start, consuming preceding whitespace
     * on the same line, then consume the preceding newline if present.
     */
    private int expandStartBackwards(int start, String text) {
        int pos = start;
        while (pos > 0 && (text.charAt(pos - 1) == ' ' || text.charAt(pos - 1) == '\t')) {
            pos--;
        }
        if (pos > 0 && text.charAt(pos - 1) == '\n') {
            pos--;
            if (pos > 0 && text.charAt(pos - 1) == '\r') {
                pos--;
            }
        }
        return pos;
    }

    /**
     * Walk forwards from the element end, consuming trailing whitespace
     * up to and including the next newline.
     */
    private int expandEndForwards(int end, String text) {
        int pos = end;
        while (pos < text.length() && (text.charAt(pos) == ' ' || text.charAt(pos) == '\t')) {
            pos++;
        }
        if (pos < text.length() && text.charAt(pos) == '\r') {
            pos++;
        }
        if (pos < text.length() && text.charAt(pos) == '\n') {
            pos++;
        }
        return pos;
    }
}
