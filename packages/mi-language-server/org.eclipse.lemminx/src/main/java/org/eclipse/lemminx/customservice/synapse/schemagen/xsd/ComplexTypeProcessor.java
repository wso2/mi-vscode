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
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;

import static org.eclipse.lemminx.customservice.synapse.schemagen.xsd.Utils.isElementArray;

/**
 * The ComplexTypeProcessor class is responsible for processing complex types in an XML Schema
 * and converting them to JSON Schema.
 */
public class ComplexTypeProcessor implements TypeProcessor {

    /**
     * Processes a complex type element and adds it to the JSON Schema node.
     *
     * @param element          The complex type element to be processed.
     * @param elementStructure The particle associated with the element.
     * @param node             The JSON Schema node.
     * @param id               The ID of the node.
     * @param addTitle         Flag indicating whether to add a title to the JSON Schema node.
     */
    @Override
    public void processType(XSElementDeclaration element, XSParticle elementStructure, ObjectNode node, String id,
                            boolean addTitle) {

        ComplexTypeProcessor.processComplexType(element, elementStructure, node, id, addTitle);
    }

    /**
     * Processes a root complex type element and adds it to the JSON Schema node.
     *
     * @param element  The root complex type element to be processed.
     * @param node     The JSON Schema node.
     * @param id       The ID of the node.
     * @param addTitle Flag indicating whether to add a title to the JSON Schema node.
     */
    @Override
    public void processRootType(XSElementDeclaration element, ObjectNode node, String id, boolean addTitle) {

        ComplexTypeProcessor.processComplexType(element, null, node, id, addTitle);
    }

    /**
     * Processes a complex type element and adds it to the JSON Schema node.
     *
     * @param element          The complex type element to be processed.
     * @param elementStructure The particle associated with the element.
     * @param node             The JSON Schema node.
     * @param id               The ID of the node.
     * @param addTitle         Flag indicating whether to add a title to the JSON Schema node.
     */
    private static void processComplexType(XSElementDeclaration element, XSParticle elementStructure, ObjectNode node,
                                          String id, boolean addTitle) {

        String name = element.getName();
        XSComplexTypeDefinition complexType = (XSComplexTypeDefinition) element.getTypeDefinition();
        XSParticle childElementStructure = complexType.getParticle();
        if (isElementArray(elementStructure)) {
            processArrayType(node, id, childElementStructure, complexType);
        } else {
            processObjectType(node, id, childElementStructure, complexType, addTitle, name);
        }
    }

    /**
     * Checks if the given particle has a choice group.
     *
     * @param elementStructure The particle to be checked.
     * @return True if the particle has a choice group, false otherwise.
     */
    private static boolean hasChoiceGroup(XSParticle elementStructure) {

        return elementStructure != null && elementStructure.getTerm() instanceof XSModelGroup &&
                ((XSModelGroup) elementStructure.getTerm()).getCompositor() == XSModelGroup.COMPOSITOR_CHOICE;
    }

    /**
     * Checks if the given complex type has simple content.
     *
     * @param complexType The complex type to be checked.
     * @return True if the complex type has simple content, false otherwise.
     */
    private static boolean hasSimpleContent(XSComplexTypeDefinition complexType) {

        return complexType.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE;
    }

    /**
     * Processes an array type and adds it to the parent JSON Schema node.
     *
     * @param node             The parent JSON Schema node.
     * @param id               The ID of the node.
     * @param elementStructure The particle associated with the array type.
     * @param complexType      The complex type definition.
     */
    private static void processArrayType(ObjectNode node, String id, XSParticle elementStructure,
                                         XSComplexTypeDefinition complexType) {

        JsonSchemaArrayNode jsonSchemaArray = new JsonSchemaArrayNode(id, node);
        if (hasChoiceGroup(elementStructure)) {
            JsonSchemaOneOfNode item = new JsonSchemaOneOfNode();
            processChoiceGroup(elementStructure, item, jsonSchemaArray.getId());
            jsonSchemaArray.addItem(item);
        } else {
            JsonSchemaObjectNode item = new JsonSchemaObjectNode(jsonSchemaArray.getId());
            jsonSchemaArray.addItem(item);
            processInnerContent(elementStructure, complexType, item);
            item.update(false);
        }
    }

    /**
     * Processes an object type and adds it to the JSON Schema node.
     *
     * @param node             The JSON Schema node.
     * @param id               The ID of the node.
     * @param elementStructure The particle associated with the object type.
     * @param complexType      The complex type definition.
     * @param addTitle         Flag indicating whether to add a title to the JSON Schema node.
     * @param name             The name of the object type.
     */
    private static void processObjectType(ObjectNode node, String id, XSParticle elementStructure,
                                          XSComplexTypeDefinition complexType, boolean addTitle, String name) {

        if (hasChoiceGroup(elementStructure)) {
            JsonSchemaOneOfNode schemaNode = new JsonSchemaOneOfNode(node, name);
            processChoiceGroup(elementStructure, schemaNode, id);
            schemaNode.update(addTitle);
        } else {
            JsonSchemaObjectNode jsonSchemaObject = new JsonSchemaObjectNode(node, id, name);
            processInnerContent(elementStructure, complexType, jsonSchemaObject);
            jsonSchemaObject.update(addTitle);
        }
    }

    /**
     * Processes the inner content of a complex type and adds it to the JSON Schema object node.
     *
     * @param elementStructure The element structure to be handled.
     * @param complexType      The complex type definition.
     * @param jsonSchemaObject The JSON Schema object node.
     */
    private static void processInnerContent(XSParticle elementStructure, XSComplexTypeDefinition complexType,
                                            JsonSchemaObjectNode jsonSchemaObject) {

        handleAttributes(complexType, jsonSchemaObject);
        if (hasSimpleContent(complexType)) {
            processSimpleContent(complexType, jsonSchemaObject);
        } else {
            processParticle(elementStructure, jsonSchemaObject);
        }
    }

    /**
     * Processes a choice group and adds it to the JSON Schema object node.
     *
     * @param elementStructure The particle associated with the choice group.
     * @param schemaNode       The JSON Schema one of node.
     * @param id               The ID of the node.
     */
    private static void processChoiceGroup(XSParticle elementStructure, JsonSchemaOneOfNode schemaNode, String id) {

        XSModelGroup choiceGroup = (XSModelGroup) elementStructure.getTerm();
        XSObjectList choices = choiceGroup.getParticles();
        ArrayNode oneOfArray = JsonNodeFactory.instance.arrayNode();
        for (Object choice : choices) {
            if (choice instanceof XSParticle) {
                JsonSchemaObjectNode choiceSchemaNode = new JsonSchemaObjectNode(id);
                processParticle((XSParticle) choice, choiceSchemaNode);
                choiceSchemaNode.update(false);
                oneOfArray.add(choiceSchemaNode.getNode());
            }
        }
        schemaNode.setOneOf(oneOfArray);
    }

    /**
     * Processes the attributes of the given complex type definition and adds them to the provided JSON Schema object
     * node.
     *
     * @param complexType      The complex type definition containing the attributes to be processed.
     * @param schemaObjectNode The JSON Schema object node to which the attributes will be added.
     */
    private static void handleAttributes(XSComplexTypeDefinition complexType, JsonSchemaObjectNode schemaObjectNode) {

        if (complexType.getAttributeUses().getLength() > 0) {
            for (Object attributeUseObj : complexType.getAttributeUses()) {
                if (attributeUseObj instanceof XSAttributeUse) {
                    XSAttributeUse attributeUse = (XSAttributeUse) attributeUseObj;
                    XSAttributeDeclaration attributeDeclaration = attributeUse.getAttrDeclaration();

                    String attributeName = Utils.ATTRIBUTE_PREFIX + attributeDeclaration.getName();
                    XSSimpleTypeDefinition simpleType = attributeDeclaration.getTypeDefinition();
                    String attributeType = Utils.mapXsdTypeToJsonType(Utils.getTypeName(simpleType));
                    JsonSchemaNode attributeNode =
                            new JsonSchemaNode(schemaObjectNode.getId() + Utils.ID_VALUE_SEPERATOR + attributeName,
                                    attributeType);

                    StringList lexicalEnumeration = simpleType.getLexicalEnumeration();
                    if (lexicalEnumeration.getLength() > 0) {
                        ArrayNode enumArray = JsonNodeFactory.instance.arrayNode();
                        for (Object enumValue : lexicalEnumeration) {
                            if (enumValue instanceof String) {
                                enumArray.add((String) enumValue);
                            }
                        }
                        attributeNode.set(Utils.ENUM, enumArray);
                    }
                    schemaObjectNode.addProperty(attributeName, attributeNode.getNode());
                }
            }
        }
    }

    /**
     * Processes the simple content of an XML Schema complex type definition and updates the provided JSON Schema object node.
     *
     * @param complexType         the XML Schema complex type definition
     * @param simpleContentObject the JSON Schema object node to update
     */
    private static void processSimpleContent(XSComplexTypeDefinition complexType,
                                            JsonSchemaObjectNode simpleContentObject) {

        XSSimpleTypeDefinition simpleType = complexType.getSimpleType();
        String jsonType = Utils.mapXsdTypeToJsonType(simpleType.getName());
        JsonSchemaElementValueNode valueNode = new JsonSchemaElementValueNode(simpleContentObject.getId(), jsonType);
        simpleContentObject.addProperty(Utils.ELEMENT_VALUE, valueNode.getNode());
    }

    /**
     * Processes an XML Schema particle and updates the provided JSON Schema object node.
     *
     * @param elementStructure the XML Schema particle to process
     * @param jsonSchemaObject the JSON Schema object node to update
     */
    private static void processParticle(XSParticle elementStructure, JsonSchemaObjectNode jsonSchemaObject) {

        if (elementStructure != null) {
            XSTerm term = elementStructure.getTerm();
            if (term instanceof XSModelGroup) {
                XSModelGroup modelGroup = (XSModelGroup) term;
                XSObjectList childStructures = modelGroup.getParticles();
                for (Object childStructureObj : childStructures) {
                    if (childStructureObj instanceof XSParticle) {
                        XSParticle childStructure = (XSParticle) childStructureObj;
                        processParticle(childStructure, jsonSchemaObject);
                    }
                }
            } else if (term instanceof XSElementDeclaration) {
                XSElementDeclaration element = (XSElementDeclaration) term;
                ObjectNode emptyObjectNode = JsonNodeFactory.instance.objectNode();
                jsonSchemaObject.addProperty(element.getName(), emptyObjectNode);

                TypeProcessor processor = TypeProcessorFactory.getTypeProcessor(element);
                processor.processType(element, elementStructure, emptyObjectNode,
                        jsonSchemaObject.getId() + Utils.ID_VALUE_SEPERATOR + element.getName(), false);

                if (elementStructure.getMinOccurs() > 0) {
                    jsonSchemaObject.addRequiredElement(element.getName());
                }
            }
        }
    }
}
