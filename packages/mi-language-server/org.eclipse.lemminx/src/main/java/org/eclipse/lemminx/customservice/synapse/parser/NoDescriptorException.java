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
 * Exception thrown when a descriptor.xml file is missing or cannot be parsed
 * from an integration project dependency (.car file).
 */
public class NoDescriptorException extends Exception {

    public NoDescriptorException(String message) {

        super(message);
    }

    public NoDescriptorException(String message, Throwable cause) {

        super(message, cause);
    }
}
