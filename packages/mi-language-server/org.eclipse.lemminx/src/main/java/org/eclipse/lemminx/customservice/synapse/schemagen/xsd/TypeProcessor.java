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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSParticle;

/**
 * The TypeProcessor interface defines methods for processing XML Schema types and converting them into JSON Schema nodes.
 */
public interface TypeProcessor {

    /**
     * Processes an XML Schema element declaration and updates the provided JSON Schema object node.
     *
     * @param element          the XML Schema element declaration
     * @param elementStructure the XML Schema particle structure
     * @param node             the JSON Schema object node to update
     * @param id               the identifier for the JSON Schema node
     * @param addTitle         flag indicating if the title should be added
     */
    void processType(XSElementDeclaration element, XSParticle elementStructure, ObjectNode node, String id,
                     boolean addTitle);

    /**
     * Processes the root XML Schema element declaration and updates the provided JSON Schema object node.
     *
     * @param element  the XML Schema element declaration
     * @param node     the JSON Schema object node to update
     * @param id       the identifier for the JSON Schema node
     * @param addTitle flag indicating if the title should be added
     */
    void processRootType(XSElementDeclaration element, ObjectNode node, String id, boolean addTitle);
}
