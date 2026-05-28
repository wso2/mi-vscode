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

// Source: https://github.com/wso2/carbon-mediation/blob/master/components/mediation-admin/org.wso2.carbon.rest.api/src/main/java/org/wso2/carbon/rest/api/APIException.java

/**
 * Class <code>APIException</code> creates a custom exception for
 * Rest API admin
 */
public class APIException extends Exception {

    private static final long serialVersionUID = -6272463911272868928L;

    public APIException() {

    }

    public APIException(String message) {

        super(message);
    }

    public APIException(Throwable cause) {

        super(cause);
    }

    public APIException(String message, Throwable cause) {

        super(message, cause);
    }

}
