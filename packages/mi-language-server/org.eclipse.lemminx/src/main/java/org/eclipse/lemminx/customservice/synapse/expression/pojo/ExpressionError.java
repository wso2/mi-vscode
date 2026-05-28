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

import org.antlr.v4.runtime.RecognitionException;

/**
 * Represents an exception that occurs during the parsing of Synapse Expressions.
 */
public class ExpressionError {

    private final String expression;
    private final int line;
    private final int charPositionInLine;
    private final String message;
    private final Object offendingSymbol;
    private final RecognitionException exception;
    private boolean warning;

    public ExpressionError(String expression, int line, int charPositionInLine, String message, Object offendingSymbol,
                           RecognitionException exception) {

        this.expression = expression;
        this.line = line;
        this.charPositionInLine = charPositionInLine;
        this.message = message;
        this.offendingSymbol = offendingSymbol;
        this.exception = exception;
        this.warning = false;
    }

    public ExpressionError(int line, int charPositionInLine, String message, Object offendingSymbol,
                           RecognitionException exception) {

        this(null, line, charPositionInLine, message, offendingSymbol, exception);
    }

    public ExpressionError(String expression, ExpressionError error) {

        this(expression, error.getLine(), error.getCharPositionInLine(),
                error.getMessage(), error.getOffendingSymbol(), error.getException());
        this.warning = error.isWarning();
    }

    public int getLine() {

        return line;
    }

    public int getCharPositionInLine() {

        return charPositionInLine;
    }

    public String getMessage() {

        return message;
    }

    public Object getOffendingSymbol() {

        return offendingSymbol;
    }

    public RecognitionException getException() {

        return exception;
    }

    public String getExpression() {

        return expression;
    }

    public boolean isWarning() {

        return warning;
    }

    public void setWarning(boolean warning) {

        this.warning = warning;
    }

    public String getFullMessage() {

        return String.format("%s (Offending symbol: %s)", getMessage(), getOffendingSymbol());
    }

    @Override
    public String toString() {

        return "SyntaxError{" +
                "line=" + line +
                ", charPositionInLine=" + charPositionInLine +
                ", message='" + message + '\'' +
                ", offendingSymbol=" + offendingSymbol +
                ", exception=" + (exception != null ? exception.getClass().getSimpleName() : "None") +
                '}';
    }
}
