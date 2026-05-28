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
package org.eclipse.lemminx.customservice.synapse.expression;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a listener that captures syntax errors during the parsing of Synapse Expressions.
 */
public class SyntaxErrorListener extends BaseErrorListener {

    private boolean hasErrors = false;

    // List to store error details
    private final List<ExpressionError> errors = Collections.synchronizedList(new ArrayList<>());

    /**
     * Check if there are any syntax errors captured.
     *
     * @return true if any errors were captured, false otherwise.
     */
    public boolean hasErrors() {

        return hasErrors;
    }

    /**
     * Get the list of syntax errors.
     *
     * @return List of SyntaxError objects containing error details.
     */
    public List<ExpressionError> getErrors() {

        return Collections.unmodifiableList(errors);
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                            String msg, RecognitionException e) {

        hasErrors = true;

        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Syntax error at line ").append(line)
                .append(", column ").append(charPositionInLine).append(": ");
        if (offendingSymbol != null) {
            errorMessage.append("offending symbol '").append(offendingSymbol).append("'. ");
        }
        errorMessage.append("Reason: ").append(msg);

        errors.add(new ExpressionError(line, charPositionInLine, errorMessage.toString(), offendingSymbol, e));
    }
}
