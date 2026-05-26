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

import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;

/**
 * The Utils class provides utility methods and constants for JSON Schema generation from XML Schema.
 */
public class Utils {

    public static final String ROOT = "root";
    public static final String ATTRIBUTE_PREFIX = "attr_";
    public static final String ENUM = "enum";
    public static final String ONE_OF = "oneOf";
    public static final String ID = "id";
    public static final String TYPE = "type";
    public static final String PROPERTIES = "properties";
    public static final String REQUIRED = "required";
    public static final String ITEMS = "items";
    public static final String ARRAY = "array";
    public static final String TITLE = "title";
    public static final String OBJECT = "object";
    public static final String ARRAY_FIRST_ELEMENT_IDENTIFIER = "/0";
    public static final String ELEMENT_VALUE = "_ELEMVAL";
    public static final String ID_VALUE_SEPERATOR = "/";

    /**
     * Maps an XML Schema data type to a JSON Schema data type.
     *
     * @param xsdType the XML Schema data type
     * @return the corresponding JSON Schema data type
     */
    public static String mapXsdTypeToJsonType(String xsdType) {

        switch (xsdType) {
            case "int":
            case "integer":
            case "decimal":
            case "float":
            case "double":
            case "byte":
            case "long":
            case "negativeInteger":
            case "nonNegativeInteger":
            case "nonPositiveInteger":
            case "positiveInteger":
            case "short":
            case "unsignedLong":
            case "unsignedInt":
            case "unsignedShort":
            case "unsignedByte":
                return "number";
            case "boolean":
                return "boolean";
            case "string":
            case "date":
            case "dateTime":
            case "time":
            case "duration":
            default:
                return "string";
        }
    }

    /**
     * Determines if the given element structure represents an array.
     *
     * @param elementStructure the element structure to check
     * @return true if the element structure represents an array, false otherwise
     */
    public static boolean isElementArray(XSParticle elementStructure) {

        if (elementStructure == null) {
            return false;
        }
        int minOccurs = elementStructure.getMinOccurs();
        int maxOccurs = elementStructure.getMaxOccurs();
        return minOccurs > 1 || maxOccurs > 1 || maxOccurs == -1;
    }

    /**
     * Retrieves the type name from the given XML Schema simple type definition.
     *
     * @param simpleType the XML Schema simple type definition
     * @return the type name
     */
    public static String getTypeName(XSSimpleTypeDefinition simpleType) {

        String typeName = simpleType.getName();
        if (typeName == null) {
            typeName = simpleType.getBaseType().getName();
            if (typeName == null) {
                typeName = simpleType.getPrimitiveType().getName();
            }
        }
        return typeName;
    }
}
