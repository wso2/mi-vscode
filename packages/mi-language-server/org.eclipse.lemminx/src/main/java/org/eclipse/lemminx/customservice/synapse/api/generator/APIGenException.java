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

package org.eclipse.lemminx.customservice.synapse.api.generator;

// Source: https://github.com/wso2/carbon-mediation/blob/master/components/mediation-commons/src/main/java/org/wso2/carbon/mediation/commons/rest/api/swagger/APIGenException.java

/**
 * Custom exception for API generation
 */
public class APIGenException extends Exception {

    public APIGenException() {

    }

    public APIGenException(String message) {

        super(message);
    }

    public APIGenException(String message, Throwable cause) {

        super(message, cause);
    }

    public APIGenException(Throwable cause) {

        super(cause);
    }
}
