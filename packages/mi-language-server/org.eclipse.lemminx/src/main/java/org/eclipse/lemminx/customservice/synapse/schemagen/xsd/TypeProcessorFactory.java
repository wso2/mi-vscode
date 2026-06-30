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

package org.eclipse.lemminx.customservice.synapse.schemagen.xsd;

import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;

/**
 * The TypeProcessorFactory class provides a method to obtain the appropriate TypeProcessor
 * based on the type of the XML Schema element declaration.
 */
public class TypeProcessorFactory {

    /**
     * Returns the appropriate TypeProcessor for the given XML Schema element declaration.
     *
     * @param element the XML Schema element declaration
     * @return the TypeProcessor for the given element
     */
    public static TypeProcessor getTypeProcessor(XSElementDeclaration element) {

        if (element.getTypeDefinition() instanceof XSComplexTypeDefinition) {
            return new ComplexTypeProcessor();
        } else {
            return new SimpleTypeProcessor();
        }
    }
}
