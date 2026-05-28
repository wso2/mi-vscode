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
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Reusable code action handler that inserts one or more missing attributes
 * into an element's start tag. Each {@link AttributeChoice} produces a
 * separate quick-fix option offered to the user.
 */
public class AddMissingAttributeCodeAction implements ICodeActionParticipant {

    /**
     * A single quick-fix choice: a human-readable title and the raw attribute
     * text to insert (including leading space, e.g. {@code " value=\"\""}).
     */
    public static final class AttributeChoice {
        private final String title;
        private final String attrText;

        public AttributeChoice(String title, String attrText) {
            this.title = title;
            this.attrText = attrText;
        }

        public String getTitle() {
            return title;
        }

        public String getAttrText() {
            return attrText;
        }
    }

    private final List<AttributeChoice> choices;

    public AddMissingAttributeCodeAction(List<AttributeChoice> choices) {
        this.choices = choices;
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

            int insertOffset = element.getOffsetBeforeCloseOfStartTag();
            if (insertOffset == DOMNode.NULL_VALUE) {
                return;
            }
            Position insertPos = document.positionAt(insertOffset);

            for (AttributeChoice choice : choices) {
                codeActions.add(CodeActionFactory.insert(
                        choice.getTitle(),
                        insertPos,
                        choice.getAttrText(),
                        document.getTextDocument(),
                        diagnostic));
            }
        } catch (BadLocationException e) {
            // ignore
        }
    }
}
