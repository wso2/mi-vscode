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

import java.util.ArrayList;
import java.util.List;

public class ExpressionValidationResponse {

    private boolean isValid;
    private List<ExpressionError> errors;

    public ExpressionValidationResponse(boolean isValid, List<ExpressionError> errors) {

        this.isValid = isValid;
        this.errors = new ArrayList<>();
        for (ExpressionError error : errors) {
            // Ignore exceptions as they are not needed in the response
            this.errors.add(new ExpressionError(error.getExpression(), error.getLine(), error.getCharPositionInLine(),
                    error.getMessage(), error.getOffendingSymbol().toString(), null));
        }
    }

    public boolean isValid() {

        return isValid;
    }

    public void setValid(boolean valid) {

        isValid = valid;
    }

    public List<ExpressionError> getErrorMessage() {

        return errors;
    }

    public void setErrorMessage(List<ExpressionError> errors) {

        this.errors = errors;
    }
}
