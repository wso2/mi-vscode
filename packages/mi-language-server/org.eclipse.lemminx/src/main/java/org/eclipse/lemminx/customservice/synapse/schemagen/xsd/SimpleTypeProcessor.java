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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;

import static org.eclipse.lemminx.customservice.synapse.schemagen.xsd.Utils.getTypeName;
import static org.eclipse.lemminx.customservice.synapse.schemagen.xsd.Utils.isElementArray;

/**
 * The SimpleTypeProcessor class processes XML Schema simple type definitions and converts them into JSON Schema nodes.
 */
public class SimpleTypeProcessor implements TypeProcessor {

    /**
     * Processes an XML Schema element declaration and updates the provided JSON Schema object node.
     *
     * @param element          the XML Schema element declaration
     * @param elementStructure the XML Schema particle structure
     * @param node             the JSON Schema object node to update
     * @param id               the identifier for the JSON Schema node
     * @param addTitle         flag indicating if the title should be added
     */
    @Override
    public void processType(XSElementDeclaration element, XSParticle elementStructure, ObjectNode node, String id,
                            boolean addTitle) {

        JsonSchemaNode jsonSchemaNode = new JsonSchemaNode(node, id);
        SimpleTypeProcessor.processSimpleType(element, elementStructure, jsonSchemaNode);
    }

    /**
     * Processes the root XML Schema element declaration and updates the provided JSON Schema object node.
     *
     * @param element  the XML Schema element declaration
     * @param node     the JSON Schema object node to update
     * @param id       the identifier for the JSON Schema node
     * @param addTitle flag indicating if the title should be added
     */
    @Override
    public void processRootType(XSElementDeclaration element, ObjectNode node, String id, boolean addTitle) {

        JsonSchemaObjectNode rootObject = new JsonSchemaObjectNode(node, id, Utils.ROOT);
        JsonSchemaNode property = new JsonSchemaNode(id + Utils.ID_VALUE_SEPERATOR + element.getName());
        rootObject.addProperty(element.getName(), property.getNode());
        rootObject.addRequiredElement(element.getName());
        processSimpleType(element, null, property);
        rootObject.update(true);
    }

    /**
     * Processes an XML Schema simple type definition and updates the provided JSON Schema node.
     *
     * @param element          the XML Schema element declaration
     * @param elementStructure the XML Schema particle structure
     * @param schemaNode       the JSON Schema node to update
     */
    private static void processSimpleType(XSElementDeclaration element, XSParticle elementStructure,
                                         JsonSchemaNode schemaNode) {

        boolean isArray = isElementArray(elementStructure);
        XSSimpleTypeDefinition simpleType = (XSSimpleTypeDefinition) element.getTypeDefinition();
        String type = Utils.mapXsdTypeToJsonType(getTypeName(simpleType));

        if (isArray) {
            JsonSchemaArrayNode jsonSchemaArray = new JsonSchemaArrayNode(schemaNode.getId(), schemaNode.getNode());
            JsonSchemaNode item = new JsonSchemaNode(schemaNode.getId() + Utils.ARRAY_FIRST_ELEMENT_IDENTIFIER, type);
            jsonSchemaArray.addItem(item);
            schemaNode = item;
        } else {
            schemaNode.setType(type);
        }

        StringList lexicalEnumeration = simpleType.getLexicalEnumeration();
        if (lexicalEnumeration.getLength() > 0) {
            ArrayNode enumArray = JsonNodeFactory.instance.arrayNode();
            for (Object enumValue : lexicalEnumeration) {
                if (enumValue instanceof String) {
                    enumArray.add((String) enumValue);
                }
            }
            schemaNode.set(Utils.ENUM, enumArray);
        }
    }
}
