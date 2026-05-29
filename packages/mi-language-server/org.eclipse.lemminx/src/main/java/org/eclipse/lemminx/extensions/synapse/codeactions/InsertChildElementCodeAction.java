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
import org.eclipse.lemminx.dom.LineIndentInfo;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action handler that inserts a child element into a parent element.
 * Handles both self-closed ({@code <foo/>}) and open-close ({@code <foo></foo>})
 * parent elements with proper indentation.
 */
public class InsertChildElementCodeAction implements ICodeActionParticipant {

    private final String actionTitle;
    private final String childTemplate;

    public InsertChildElementCodeAction(String actionTitle, String childTemplate) {
        this.actionTitle = actionTitle;
        this.childTemplate = childTemplate;
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

            int elementLine = diagnostic.getRange().getStart().getLine();
            LineIndentInfo indentInfo = document.getLineIndentInfo(elementLine);
            String parentIndent = indentInfo.getWhitespacesIndent();
            String childIndent = parentIndent + "    ";
            String nl = indentInfo.getLineDelimiter();
            if (nl == null || nl.isEmpty()) {
                nl = System.lineSeparator();
            }

            if (element.isSelfClosed()) {
                // Convert <foo attr="x"/> to <foo attr="x">\n    CHILD\n</foo>
                String text = document.getText();
                int elementStart = element.getStart();
                int elementEnd = element.getEnd();

                // Extract the start tag content without the self-close "/>"
                // e.g. from "<log level=\"custom\"/>" get "<log level=\"custom\""
                String startTagContent = text.substring(elementStart, elementEnd - 2);
                String tagName = element.getTagName();

                String replacement = startTagContent + ">" + nl
                        + childIndent + childTemplate + nl
                        + parentIndent + "</" + tagName + ">";

                Range replaceRange = new Range(
                        document.positionAt(elementStart),
                        document.positionAt(elementEnd));
                codeActions.add(CodeActionFactory.replace(
                        actionTitle, replaceRange, replacement,
                        document.getTextDocument(), diagnostic));
            } else {
                // Insert child after the opening tag's ">"
                int insertOffset = element.getStartTagCloseOffset() + 1;
                Position insertPos = document.positionAt(insertOffset);
                String insertText = nl + childIndent + childTemplate;
                codeActions.add(CodeActionFactory.insert(
                        actionTitle, insertPos, insertText,
                        document.getTextDocument(), diagnostic));
            }
        } catch (BadLocationException e) {
            // ignore
        }
    }
}
