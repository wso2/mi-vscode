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

package org.eclipse.lemminx.customservice.synapse.parser;

/**
 * Exception thrown when the versioned deployment status is different of the parent and child projects.
 */
public class VersioningTypeMismatchException extends Exception {

    public VersioningTypeMismatchException(String message) {

        super(message);
    }

    public VersioningTypeMismatchException(String message, Throwable cause) {

        super(message, cause);
    }
}
