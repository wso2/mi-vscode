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
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action handler for the {@code UndefinedVariable} diagnostic.
 * Extracts the variable name from the diagnostic message and inserts a
 * {@code <variable>} definition before the element that references it.
 */
public class InsertVariableDefinitionCodeAction implements ICodeActionParticipant {

    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions,
                             CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();

        Object data = diagnostic.getData();
        if (!(data instanceof String) || ((String) data).isEmpty()) {
            return;
        }
        String varName = (String) data;

        try {
            // The diagnostic range is on the attribute value; findNodeAt returns the element
            int offset = document.offsetAt(diagnostic.getRange().getStart());
            DOMNode node = document.findNodeAt(offset);
            if (!(node instanceof DOMElement)) {
                return;
            }
            DOMElement element = (DOMElement) node;

            int elementStartLine = document.positionAt(element.getStart()).getLine();
            LineIndentInfo indentInfo = document.getLineIndentInfo(elementStartLine);
            String indent = indentInfo.getWhitespacesIndent();
            String nl = indentInfo.getLineDelimiter();
            if (nl == null || nl.isEmpty()) {
                nl = System.lineSeparator();
            }

            Position insertPos = document.positionAt(element.getStart());
            String variableDecl = "<variable name=\"" + escapeXmlAttr(varName)
                    + "\" type=\"STRING\" value=\"\"/>" + nl + indent;

            codeActions.add(CodeActionFactory.insert(
                    "Define variable '" + varName + "'",
                    insertPos,
                    variableDecl,
                    document.getTextDocument(),
                    diagnostic));
        } catch (BadLocationException e) {
            // ignore
        }
    }

    private static String escapeXmlAttr(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }
}
