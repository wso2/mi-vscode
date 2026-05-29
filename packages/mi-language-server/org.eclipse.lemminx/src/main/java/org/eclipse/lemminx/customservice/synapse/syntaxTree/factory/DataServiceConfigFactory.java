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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Attribute;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.AuthorizationProvider;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.AuthorizationProviderProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.CallQuery;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.CallQueryWithParam;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Config;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Data;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.DataPolicy;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Element;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.EventTrigger;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.EventTriggerSubscriptions;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Expression;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Operation;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Param;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.ParamElements;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.ParamValidateCustom;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.ParamValidateDoubleRange;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.ParamValidateLength;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.ParamValidateLongRange;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.ParamValidatePattern;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Property;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.PropertyConfiguration;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.PropertyConfigurationEntry;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.PropertyProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Query;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.QueryProperties;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Resource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Result;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.ResultElements;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Sparql;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Sql;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DataServiceConfigFactory extends AbstractFactory {

    private static final String SECRET_ALIAS_NS = "http://org.wso2.securevault/configuration";

    @Override
    public STNode create(DOMElement element) {

        Data dataService = new Data();
        dataService.elementNode(element);
        populateAttributes(dataService, element);

        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.DESCRIPTION.equalsIgnoreCase(childName)) {
                    STNode description = new STNode();
                    description.elementNode((DOMElement) child);
                    dataService.setDescription(description);
                } else if (Constant.CONFIG.equalsIgnoreCase(childName)) {
                    Config config = createConfig(child);
                    dataService.addConfig(config);
                } else if (Constant.QUERY.equalsIgnoreCase(childName)) {
                    Query query = createQuery(child);
                    dataService.addQuery(query);
                } else if (Constant.OPERATION.equalsIgnoreCase(childName)) {
                    Operation operation = createOperation(child);
                    dataService.addOperation(operation);
                } else if (Constant.RESOURCE.equalsIgnoreCase(childName)) {
                    Resource resource = createResource(child);
                    dataService.addResource(resource);
                } else if (Constant.POLICY.equalsIgnoreCase(childName)) {
                    DataPolicy policy = createPolicy(child);
                    dataService.setPolicy(policy);
                } else if (Constant.EVENT_TRIGGER.equalsIgnoreCase(childName)) {
                    EventTrigger eventTrigger = createEventTrigger(child);
                    dataService.addEventTrigger(eventTrigger);
                } else if (Constant.ENABLE_SEC.equalsIgnoreCase(childName)) {
                    STNode enableSec = new STNode();
                    enableSec.elementNode((DOMElement) child);
                    dataService.setEnableSec(enableSec);
                } else if (Constant.AUTHORIZATION_PROVIDER.equalsIgnoreCase(childName)) {
                    AuthorizationProvider authorizationProvider = createAuthorizationProvider(child);
                    dataService.setAuthorizationProvider(authorizationProvider);
                }
            }
        }
        return dataService;
    }

    private Config createConfig(DOMNode element) {

        Config config = new Config();
        config.elementNode((DOMElement) element);
        String id = element.getAttribute(Constant.ID);
        if (id != null) {
            config.setId(id);
        }
        String enableOData = element.getAttribute(Constant.ENABLE_ODATA);
        if (enableOData != null) {
            config.setEnableOData(Boolean.parseBoolean(enableOData));
        }
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            List<Property> propertyList = new ArrayList<>();
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.PROPERTY.equalsIgnoreCase(childName)) {
                    Property property = createProperty(child);
                    propertyList.add(property);
                }
            }
            config.setProperty(propertyList.toArray(new Property[propertyList.size()]));
        }
        return config;
    }

    private Query createQuery(DOMNode element) {

        Query query = new Query();
        query.elementNode((DOMElement) element);
        String id = element.getAttribute(Constant.ID);
        if (id != null) {
            query.setId(id);
        }
        String useConfig = element.getAttribute(Constant.USE_CONFIG);
        if (useConfig != null) {
            query.setUseConfig(useConfig);
        }
        String returnGeneratedKeys = element.getAttribute(Constant.RETURN_GENERATED_KEYS);
        if (returnGeneratedKeys != null) {
            query.setReturnGeneratedKeys(Boolean.parseBoolean(returnGeneratedKeys));
        }
        String inputEventTrigger = element.getAttribute(Constant.INPUT_EVENT_TRIGGER);
        if (inputEventTrigger != null) {
            query.setInputEventTrigger(inputEventTrigger);
        }
        String keyColumns = element.getAttribute(Constant.KEY_COLUMNS);
        if (keyColumns != null) {
            query.setKeyColumns(keyColumns);
        }
        String returnUpdatedRowCount = element.getAttribute(Constant.RETURN_UPDATED_ROW_COUNT);
        if (returnUpdatedRowCount != null) {
            query.setReturnUpdatedRowCount(returnUpdatedRowCount);
        }

        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            List<Param> queryParamList = new ArrayList<>();
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.SQL.equalsIgnoreCase(childName)) {
                    Sql sql = createSql(child);
                    query.setSql(sql);
                } else if (Constant.EXPRESSION.equalsIgnoreCase(childName)) {
                    Expression expression = createExpression(child);
                    query.setExpression(expression);
                } else if (Constant.SPARQL.equalsIgnoreCase(childName)) {
                    Sparql sparql = createSparql(child);
                    query.setSparql(sparql);
                } else if (Constant.PROPERTIES.equalsIgnoreCase(childName)) {
                    QueryProperties properties = createQueryProperties(child);
                    query.setProperties(properties);
                } else if (Constant.RESULT.equalsIgnoreCase(childName)) {
                    Result queryResult = createQueryResult(child);
                    query.setResult(queryResult);
                } else if (Constant.PARAM.equalsIgnoreCase(childName)) {
                    Param param = createParam(child);
                    queryParamList.add(param);
                }
            }
            query.setParams(queryParamList.toArray(new Param[queryParamList.size()]));
        }
        return query;
    }

    private Sql createSql(DOMNode element) {

        Sql sql = new Sql();
        sql.elementNode((DOMElement) element);
        String dialect = element.getAttribute(Constant.DIALECT);
        if (dialect != null) {
            sql.setDialect(dialect);
        }
        DOMNode child = element.getFirstChild();
        String value = Utils.getInlineString(child);
        if (value != null) {
            sql.setValue(value);
        }
        return sql;
    }

    private Expression createExpression(DOMNode element) {

        Expression expression = new Expression();
        expression.elementNode((DOMElement) element);
        String dialect = element.getAttribute(Constant.DIALECT);
        if (dialect != null) {
            expression.setDialect(dialect);
        }
        DOMNode child = element.getFirstChild();
        String value = Utils.getInlineString(child);
        if (value != null) {
            expression.setValue(value);
        }
        return expression;
    }

    private Sparql createSparql(DOMNode element) {

        Sparql sparql = new Sparql();
        sparql.elementNode((DOMElement) element);
        DOMNode child = element.getFirstChild();
        String value = Utils.getInlineString(child);
        if (value != null) {
            sparql.setValue(value);
        }
        return sparql;
    }

    private QueryProperties createQueryProperties(DOMNode element) {

        QueryProperties properties = new QueryProperties();
        properties.elementNode((DOMElement) element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            List<Property> propertyList = new ArrayList<>();
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.PROPERTY.equalsIgnoreCase(childName)) {
                    Property property = createProperty(child);
                    property.elementNode((DOMElement) child);
                    propertyList.add(property);
                }
            }
            properties.setProperty(propertyList.toArray(new Property[propertyList.size()]));
        }
        return properties;
    }

    private Property createProperty(DOMNode element) {

        Property property = new Property();
        property.elementNode((DOMElement) element);
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            property.setName(name);
        }
        if (((DOMElement) element).hasAttributeNS(SECRET_ALIAS_NS, "secretAlias")) {
            String secretAlias = ((DOMElement) element).getAttributeNS(SECRET_ALIAS_NS, "secretAlias");
            property.setTextNode(secretAlias);
        }
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            List<PropertyProperty> propertyList = new ArrayList<>();
            List<PropertyConfiguration> configurationList = new ArrayList<>();
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.CONFIGURATION.equalsIgnoreCase(childName)) {
                    PropertyConfiguration configuration = createPropertyConfiguration(child);
                    configurationList.add(configuration);
                } else if (Constant.PROPERTY.equalsIgnoreCase(childName)) {
                    PropertyProperty propertyProperty = createPropertyProperty(child);
                    propertyList.add(propertyProperty);
                }
            }
            property.setProperty(propertyList.toArray(new PropertyProperty[propertyList.size()]));
            property.setConfiguration(configurationList.toArray(new PropertyConfiguration[configurationList.size()]));
        }
        return property;
    }

    private PropertyConfiguration createPropertyConfiguration(DOMNode element) {

        PropertyConfiguration configuration = new PropertyConfiguration();
        configuration.elementNode((DOMElement) element);

        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            List<PropertyConfigurationEntry> entryList = new ArrayList<>();
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.ENTRY.equalsIgnoreCase(childName)) {
                    PropertyConfigurationEntry entry = createPropertyConfigurationEntry(child);
                    entryList.add(entry);
                }
            }
            configuration.setEntry(entryList.toArray(new PropertyConfigurationEntry[entryList.size()]));
        }
        return configuration;
    }

    private PropertyConfigurationEntry createPropertyConfigurationEntry(DOMNode element) {

        PropertyConfigurationEntry entry = new PropertyConfigurationEntry();
        entry.elementNode((DOMElement) element);
        String request = element.getAttribute(Constant.REQUEST);
        if (request != null) {
            entry.setRequest(request);
        }
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.USERNAME.equalsIgnoreCase(childName)) {
                    STNode username = new STNode();
                    username.elementNode((DOMElement) child);
                    entry.setUsername(username);
                } else if (Constant.PASSWORD.equalsIgnoreCase(childName)) {
                    STNode password = new STNode();
                    password.elementNode((DOMElement) child);
                    entry.setPassword(password);
                }
            }
        }
        return entry;
    }

    private PropertyProperty createPropertyProperty(DOMNode element) {

        PropertyProperty propertyProperty = new PropertyProperty();
        propertyProperty.elementNode((DOMElement) element);
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            propertyProperty.setName(name);
        }
        DOMNode child = element.getFirstChild();
        String value = Utils.getInlineString(child);
        if (value != null) {
            propertyProperty.setValue(value);
        }
        return propertyProperty;
    }

    private Result createQueryResult(DOMNode node) {

        Result result = new Result();
        result.elementNode((DOMElement) node);

        String elementName = node.getAttribute(Constant.ELEMENT);
        if (elementName != null) {
            result.setElement(elementName);
        }
        String rowName = node.getAttribute(Constant.ROW_NAME);
        if (rowName != null) {
            result.setRowName(rowName);
        }
        String defaultNamespace = node.getAttribute(Constant.DEFAULT_NAMESPACE);
        if (defaultNamespace != null) {
            result.setDefaultNamespace(defaultNamespace);
        }

        String useColumnNumbers = node.getAttribute(Constant.USE_COLUMN_NUMBERS);
        if (useColumnNumbers != null) {
            result.setUseColumnNumbers(Boolean.parseBoolean(useColumnNumbers));
        }

        String escapeNonPrintableChar = node.getAttribute(Constant.ESCAPE_NON_PRINTABLE_CHAR);
        if (escapeNonPrintableChar != null) {
            result.setEscapeNonPrintableChar(Boolean.parseBoolean(escapeNonPrintableChar));
        }

        String xsltPath = node.getAttribute(Constant.XSLT_PATH);
        if (xsltPath != null) {
            result.setXsltPath(xsltPath);
        }

        String rdfBaseURI = node.getAttribute(Constant.RDF_BASE_URI);
        if (rdfBaseURI != null) {
            result.setRdfBaseURI(rdfBaseURI);
        }

        String outputType = node.getAttribute(Constant.OUTPUT_TYPE);
        if (outputType != null) {
            result.setOutputType(outputType);
        }

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.ELEMENT.equalsIgnoreCase(childName)) {
                    Element element = createElement(child);
                    result.addElement(element);
                } else if (Constant.ATTRIBUTE.equalsIgnoreCase(childName)) {
                    Attribute attribute = createAttribute(child);
                    result.addAttribute(attribute);
                } else if (Constant.CALL_QUERY.equalsIgnoreCase(childName)) {
                    CallQuery callQuery = createCallQuery(child);
                    result.addCallQuery(callQuery);
                }
            }
        }

        return result;
    }

    private List<ResultElements> createResultElements(DOMNode node) {

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            List<ResultElements> resultElements = new ArrayList<>();
            for (DOMNode child : children) {
                ResultElements resultElement = new ResultElements();
                String childName = child.getNodeName();
                if (Constant.ELEMENT.equalsIgnoreCase(childName)) {
                    Element element = createElement(child);
                    resultElement.setElement(Optional.ofNullable(element));
                } else if (Constant.ATTRIBUTE.equalsIgnoreCase(childName)) {
                    Attribute attribute = createAttribute(child);
                    resultElement.setAttribute(Optional.ofNullable(attribute));
                } else if (Constant.CALL_QUERY.equalsIgnoreCase(childName)) {
                    CallQuery callQuery = createCallQuery(child);
                    resultElement.setCall_query(Optional.ofNullable(callQuery));
                }
                resultElements.add(resultElement);
            }
            return resultElements;
        }
        return null;
    }

    private Element createElement(DOMNode node) {

        Element element = new Element();
        element.elementNode((DOMElement) node);

        populateElementAttributes(element, node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            String inlineXml = "";
            for (int i = 0; i < children.size(); i++) {
                DOMNode child = children.get(i);
                String childName = child.getNodeName();
                if (Constant.ELEMENT.equalsIgnoreCase(childName)) {
                    Element resultElement = createElement(child);
                    element.addElement(resultElement);
                } else if (Constant.ATTRIBUTE.equalsIgnoreCase(childName)) {
                    Attribute attribute = createAttribute(child);
                    element.addAttribute(attribute);
                } else if (Constant.CALL_QUERY.equalsIgnoreCase(childName)) {
                    CallQuery callQuery = createCallQuery(child);
                    element.addCallQuery(callQuery);
                }
                inlineXml += Utils.getInlineString(child, Boolean.FALSE);
                if (i != children.size() - 1) {
                    inlineXml += "\n";
                }
            }
            element.setInlineXml(inlineXml);
        }
        return element;
    }

    private void populateElementAttributes(Element element, DOMNode node) {

        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            element.setName(name);
        }
        String column = node.getAttribute(Constant.COLUMN);
        if (column != null) {
            element.setColumn(column);
        }
        String requiredRoles = node.getAttribute(Constant.REQUIRED_ROLES);
        if (requiredRoles != null) {
            element.setRequiredRoles(requiredRoles);
        }
        String export = node.getAttribute(Constant.EXPORT);
        if (export != null) {
            element.setExport(export);
        }
        String exportType = node.getAttribute(Constant.EXPORT_TYPE);
        if (exportType != null) {
            element.setExportType(exportType);
        }
        String xsdType = node.getAttribute(Constant.XSD_TYPE);
        if (xsdType != null) {
            element.setXsdType(xsdType);
        }
        String namespace = node.getAttribute(Constant.NAMESPACE);
        if (namespace != null) {
            element.setNamespace(namespace);
        }
        String optional = node.getAttribute(Constant.OPTIONAL);
        if (optional != null) {
            element.setOptional(Boolean.parseBoolean(optional));
        }
        String arrayName = node.getAttribute(Constant.ARRAY_NAME);
        if (arrayName != null) {
            element.setArrayName(arrayName);
        }
        String queryParam = node.getAttribute(Constant.QUERY_PARAM);
        if (queryParam != null) {
            element.setQueryParam(queryParam);
        }
    }

    private Attribute createAttribute(DOMNode node) {

        Attribute attribute = new Attribute();
        attribute.elementNode((DOMElement) node);
        populateAttributeAttributes(attribute, node);
        return attribute;
    }

    private void populateAttributeAttributes(Attribute attribute, DOMNode node) {

        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            attribute.setName(name);
        }
        String column = node.getAttribute(Constant.COLUMN);
        if (column != null) {
            attribute.setColumn(column);
        }
        String requiredRoles = node.getAttribute(Constant.REQUIRED_ROLES);
        if (requiredRoles != null) {
            attribute.setRequiredRoles(requiredRoles);
        }
        String export = node.getAttribute(Constant.EXPORT);
        if (export != null) {
            attribute.setExport(export);
        }
        String exportType = node.getAttribute(Constant.EXPORT_TYPE);
        if (exportType != null) {
            attribute.setExportType(exportType);
        }
        String xsdType = node.getAttribute(Constant.XSD_TYPE);
        if (xsdType != null) {
            attribute.setXsdType(xsdType);
        }
        String namespace = node.getAttribute(Constant.NAMESPACE);
        if (namespace != null) {
            attribute.setNamespace(namespace);
        }
        String optional = node.getAttribute(Constant.OPTIONAL);
        if (optional != null) {
            attribute.setOptional(Boolean.parseBoolean(optional));
        }
        String arrayName = node.getAttribute(Constant.ARRAY_NAME);
        if (arrayName != null) {
            attribute.setArrayName(arrayName);
        }
        String queryParam = node.getAttribute(Constant.QUERY_PARAM);
        if (queryParam != null) {
            attribute.setQueryParam(queryParam);
        }
    }

    private Param createParam(DOMNode element) {

        Param param = new Param();
        param.elementNode((DOMElement) element);
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            param.setName(name);
        }
        String sqlType = element.getAttribute(Constant.SQL_TYPE);
        if (sqlType != null) {
            param.setSqlType(sqlType);
        }
        String paramType = element.getAttribute(Constant.PARAM_TYPE);
        if (paramType != null) {
            param.setParamType(paramType);
        }
        String type = element.getAttribute(Constant.TYPE);
        if (type != null) {
            param.setType(type);
        }
        String ordinal = element.getAttribute(Constant.ORDINAL);
        if (ordinal != null) {
            param.setOrdinal(Utils.parseInt(ordinal));
        }
        String defaultValue = element.getAttribute(Constant.DEFAULT_VALUE);
        if (defaultValue != null) {
            param.setDefaultValue(defaultValue);
        }
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            List<ParamElements> elementList = new ArrayList<>();
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                ParamElements paramElements = new ParamElements();
                if (Constant.VALIDATE_CUSTOM.equalsIgnoreCase(childName)) {
                    ParamValidateCustom validateCustom = createParamValidateCustom(child);
                    paramElements.setValidateCustom(Optional.ofNullable(validateCustom));
                } else if (Constant.VALIDATE_LENGTH.equalsIgnoreCase(childName)) {
                    ParamValidateLength validateLength = createParamValidateLength(child);
                    paramElements.setValidateLength(Optional.ofNullable(validateLength));
                } else if (Constant.VALIDATE_PATTERN.equalsIgnoreCase(childName)) {
                    ParamValidatePattern validatePattern = createParamValidatePattern(child);
                    paramElements.setValidatePattern(Optional.ofNullable(validatePattern));
                } else if (Constant.VALIDATE_LONG_RANGE.equalsIgnoreCase(childName)) {
                    ParamValidateLongRange validateLongRange = createParamValidateLongRange(child);
                    paramElements.setValidateLongRange(Optional.ofNullable(validateLongRange));
                } else if (Constant.VALIDATE_DOUBLE_RANGE.equalsIgnoreCase(childName)) {
                    ParamValidateDoubleRange validateDoubleRange = createParamValidateDoubleRange(child);
                    paramElements.setValidateDoubleRange(Optional.ofNullable(validateDoubleRange));
                }
                elementList.add(paramElements);
            }
            param.setParamElements(elementList.toArray(new ParamElements[elementList.size()]));
        }
        return param;
    }

    private ParamValidateCustom createParamValidateCustom(DOMNode element) {

        ParamValidateCustom validateCustom = new ParamValidateCustom();
        validateCustom.elementNode((DOMElement) element);
        String className = element.getAttribute(Constant.CLASS);
        if (className != null) {
            validateCustom.setClazz(className);
        }
        return validateCustom;
    }

    private ParamValidateLength createParamValidateLength(DOMNode element) {

        ParamValidateLength validateLength = new ParamValidateLength();
        validateLength.elementNode((DOMElement) element);
        String minimum = element.getAttribute(Constant.MINIMUM);
        if (minimum != null) {
            validateLength.setMinimum(Utils.parseInt(minimum));
        }
        String maximum = element.getAttribute(Constant.MAXIMUM);
        if (maximum != null) {
            validateLength.setMaximum(Utils.parseInt(maximum));
        }
        return validateLength;
    }

    private ParamValidatePattern createParamValidatePattern(DOMNode element) {

        ParamValidatePattern validatePattern = new ParamValidatePattern();
        validatePattern.elementNode((DOMElement) element);
        String pattern = element.getAttribute(Constant.PATTERN);
        if (pattern != null) {
            validatePattern.setPattern(pattern);
        }
        return validatePattern;
    }

    private ParamValidateLongRange createParamValidateLongRange(DOMNode element) {

        ParamValidateLongRange validateLongRange = new ParamValidateLongRange();
        validateLongRange.elementNode((DOMElement) element);
        String minimum = element.getAttribute(Constant.MINIMUM);
        if (minimum != null) {
            validateLongRange.setMinimum(Utils.parseInt(minimum));
        }
        String maximum = element.getAttribute(Constant.MAXIMUM);
        if (maximum != null) {
            validateLongRange.setMaximum(Utils.parseInt(maximum));
        }
        return validateLongRange;
    }

    private ParamValidateDoubleRange createParamValidateDoubleRange(DOMNode element) {

        ParamValidateDoubleRange validateDoubleRange = new ParamValidateDoubleRange();
        validateDoubleRange.elementNode((DOMElement) element);
        String minimum = element.getAttribute(Constant.MINIMUM);
        if (minimum != null) {
            validateDoubleRange.setMinimum(Utils.parseInt(minimum));
        }
        String maximum = element.getAttribute(Constant.MAXIMUM);
        if (maximum != null) {
            validateDoubleRange.setMaximum(Utils.parseInt(maximum));
        }
        return validateDoubleRange;
    }

    private Operation createOperation(DOMNode element) {

        Operation operation = new Operation();
        operation.elementNode((DOMElement) element);
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            operation.setName(name);
        }
        String disableStreaming = element.getAttribute(Constant.DISABLE_STREAMING);
        if (disableStreaming != null) {
            operation.setDisableStreaming(Boolean.parseBoolean(disableStreaming));
        }
        String returnRequestStatus = element.getAttribute(Constant.RETURN_REQUEST_STATUS);
        if (returnRequestStatus != null) {
            operation.setReturnRequestStatus(Boolean.parseBoolean(returnRequestStatus));
        }
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.DESCRIPTION.equalsIgnoreCase(childName)) {
                    STNode description = new STNode();
                    description.elementNode((DOMElement) child);
                    operation.setDescription(description);
                } else if (Constant.CALL_QUERY.equalsIgnoreCase(childName)) {
                    CallQuery callQuery = createCallQuery(child);
                    operation.setCallQuery(callQuery);
                }
            }
        }
        return operation;
    }

    private Resource createResource(DOMNode element) {

        Resource resource = new Resource();
        resource.elementNode((DOMElement) element);
        String path = element.getAttribute(Constant.PATH);
        if (path != null) {
            resource.setPath(path);
        }
        String method = element.getAttribute(Constant.METHOD);
        if (method != null) {
            resource.setMethod(method);
        }
        String disableStreaming = element.getAttribute(Constant.DISABLE_STREAMING);
        if (disableStreaming != null) {
            resource.setDisableStreaming(Boolean.parseBoolean(disableStreaming));
        }
        String returnRequestStatus = element.getAttribute(Constant.RETURN_REQUEST_STATUS);
        if (returnRequestStatus != null) {
            resource.setReturnRequestStatus(Boolean.parseBoolean(returnRequestStatus));
        }
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.DESCRIPTION.equalsIgnoreCase(childName)) {
                    STNode description = new STNode();
                    description.elementNode((DOMElement) child);
                    resource.setDescription(description);
                } else if (Constant.CALL_QUERY.equalsIgnoreCase(childName)) {
                    CallQuery callQuery = createCallQuery(child);
                    resource.setCallQuery(callQuery);
                }
            }
        }
        return resource;
    }

    private CallQuery createCallQuery(DOMNode element) {

        CallQuery callQuery = new CallQuery();
        callQuery.elementNode((DOMElement) element);
        String href = element.getAttribute(Constant.HREF);
        if (href != null) {
            callQuery.setHref(href);
        }
        String requiredRoles = element.getAttribute(Constant.REQUIRED_ROLES);
        if (requiredRoles != null) {
            callQuery.setRequiredRoles(requiredRoles);
        }
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            List<STNode> withParamList = new ArrayList<>();
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.WITH_PARAM.equalsIgnoreCase(childName)) {
                    CallQueryWithParam withParam = createCallQueryWithParam(child);
                    withParamList.add(withParam);
                }
            }
            callQuery.setWithParam(withParamList.toArray(new CallQueryWithParam[withParamList.size()]));
        }
        return callQuery;
    }

    private CallQueryWithParam createCallQueryWithParam(DOMNode element) {

        CallQueryWithParam withParam = new CallQueryWithParam();
        withParam.elementNode((DOMElement) element);
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            withParam.setName(name);
        }
        String query_param = element.getAttribute(Constant.QUERY_PARAM);
        if (query_param != null) {
            withParam.setQueryParam(query_param);
        }
        String column = element.getAttribute(Constant.COLUMN);
        if (column != null) {
            withParam.setColumn(column);
        }
        String param = element.getAttribute(Constant.PARAM);
        if (param != null) {
            withParam.setParam(param);
        }
        return withParam;
    }

    private DataPolicy createPolicy(DOMNode element) {

        DataPolicy policy = new DataPolicy();
        policy.elementNode((DOMElement) element);
        String key = element.getAttribute(Constant.KEY);
        if (key != null) {
            policy.setKey(key);
        }
        return policy;
    }

    private EventTrigger createEventTrigger(DOMNode element) {

        EventTrigger eventTrigger = new EventTrigger();
        eventTrigger.elementNode((DOMElement) element);
        String id = element.getAttribute(Constant.ID);
        if (id != null) {
            eventTrigger.setId(id);
        }
        String language = element.getAttribute(Constant.LANGUAGE);
        if (language != null) {
            eventTrigger.setLanguage(language);
        }
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            List<STNode> eventTriggerElements = new ArrayList<>();
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.EXPRESSION.equalsIgnoreCase(childName)) {
                    STNode expression = new STNode();
                    expression.elementNode((DOMElement) child);
                    eventTriggerElements.add(expression);
                } else if (Constant.TARGET_TOPIC.equalsIgnoreCase(childName)) {
                    STNode targetTopic = new STNode();
                    targetTopic.elementNode((DOMElement) child);
                    eventTriggerElements.add(targetTopic);
                } else if (Constant.SUBSCRIPTIONS.equalsIgnoreCase(childName)) {
                    EventTriggerSubscriptions subscriptions = createEventTriggerSubscriptions(child);
                    eventTriggerElements.add(subscriptions);
                }
            }
            eventTrigger.setEventTriggerElements(eventTriggerElements);
        }
        return eventTrigger;
    }

    private EventTriggerSubscriptions createEventTriggerSubscriptions(DOMNode element) {

        EventTriggerSubscriptions subscriptions = new EventTriggerSubscriptions();
        subscriptions.elementNode((DOMElement) element);
        DOMNode child = element.getFirstChild();
        if (child != null && child instanceof DOMElement) {
            STNode subscription = new STNode();
            subscription.elementNode((DOMElement) child);
            subscriptions.setSubscription(subscription);
        }
        return subscriptions;
    }

    private AuthorizationProvider createAuthorizationProvider(DOMNode element) {

        AuthorizationProvider authorizationProvider = new AuthorizationProvider();
        authorizationProvider.elementNode((DOMElement) element);
        String clazz = element.getAttribute(Constant.CLASS);
        if (clazz != null) {
            authorizationProvider.setClazz(clazz);
        }
        List<DOMNode> children = element.getChildren();
        if (children != null) {
            List<STNode> properties = new ArrayList<>();
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.PROPERTY.equalsIgnoreCase(childName)) {
                    AuthorizationProviderProperty property = createAuthorizationProviderProperty(child);
                    properties.add(property);
                }
            }
            authorizationProvider.setProperty(properties.toArray(new AuthorizationProviderProperty[properties.size()]));
        }
        return authorizationProvider;
    }

    private AuthorizationProviderProperty createAuthorizationProviderProperty(DOMNode element) {

        AuthorizationProviderProperty property = new AuthorizationProviderProperty();
        property.elementNode((DOMElement) element);
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            property.setName(name);
        }
        DOMNode child = element.getFirstChild();
        String value = Utils.getInlineString(child);
        if (value != null) {
            property.setValue(value);
        }
        return property;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String baseURI = element.getAttribute(Constant.BASE_URI);
        if (baseURI != null) {
            ((Data) node).setBaseURI(baseURI);
        }
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            ((Data) node).setName(name);
        }
        String enableBatchRequests = element.getAttribute(Constant.ENABLE_BATCH_REQUESTS);
        if (enableBatchRequests != null) {
            ((Data) node).setEnableBatchRequests(Boolean.parseBoolean(enableBatchRequests));
        }
        String enableBoxcarring = element.getAttribute(Constant.ENABLE_BOXCARRING);
        if (enableBoxcarring != null) {
            ((Data) node).setEnableBoxcarring(Boolean.parseBoolean(enableBoxcarring));
        }
        String disableLegacyBoxcarringMode = element.getAttribute(Constant.DISABLE_LEGACY_BOXCARRING_MODE);
        if (disableLegacyBoxcarringMode != null) {
            ((Data) node).setDisableLegacyBoxcarringMode(Boolean.parseBoolean(disableLegacyBoxcarringMode));
        }
        String disableStreaming = element.getAttribute(Constant.DISABLE_STREAMING);
        if (disableStreaming != null) {
            ((Data) node).setDisableStreaming(Boolean.parseBoolean(disableStreaming));
        }
        String txManagerJNDIName = element.getAttribute(Constant.TX_MANAGER_JNDI_NAME);
        if (txManagerJNDIName != null) {
            ((Data) node).setTxManagerJNDIName(txManagerJNDIName);
        }
        String serviceNamespace = element.getAttribute(Constant.SERVICE_NAMESPACE);
        if (serviceNamespace != null) {
            ((Data) node).setServiceNamespace(serviceNamespace);
        }
        String serviceGroup = element.getAttribute(Constant.SERVICE_GROUP);
        if (serviceGroup != null) {
            ((Data) node).setServiceGroup(serviceGroup);
        }
        String publishSwagger = element.getAttribute(Constant.PUBLISH_SWAGGER);
        if (publishSwagger != null) {
            ((Data) node).setPublishSwagger(publishSwagger);
        }
        String transports = element.getAttribute(Constant.TRANSPORTS);
        if (transports != null) {
            ((Data) node).setTransports(transports);
        }
        String serviceStatus = element.getAttribute(Constant.SERVICE_STATUS);
        if (serviceStatus != null) {
            ((Data) node).setServiceStatus(serviceStatus);
        }
    }
}
