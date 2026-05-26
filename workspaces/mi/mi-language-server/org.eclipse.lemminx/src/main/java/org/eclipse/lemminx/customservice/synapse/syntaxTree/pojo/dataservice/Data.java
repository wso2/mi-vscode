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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

import java.util.ArrayList;
import java.util.List;

public class Data extends STNode {

    STNode description;
    List<Config> configs;
    List<Query> queries;
    List<Operation> operations;
    List<Resource> resources;
    List<EventTrigger> eventTriggers;
    DataPolicy policy;
    STNode enableSec;
    AuthorizationProvider authorizationProvider;
    String baseURI;
    String name;
    boolean enableBatchRequests;
    boolean enableBoxcarring;
    boolean disableLegacyBoxcarringMode;
    boolean disableStreaming;
    String txManagerJNDIName;
    String serviceNamespace;
    String serviceGroup;
    String publishSwagger;
    String transports;
    String serviceStatus;

    public STNode getDescription() {

        return description;
    }

    public void setDescription(STNode description) {

        this.description = description;
    }

    public List<Config> getConfigs() {

        return configs;
    }

    public void setConfigs(List<Config> configs) {

        this.configs = configs;
    }

    public List<Query> getQueries() {

        return queries;
    }

    public void setQueries(List<Query> queries) {

        this.queries = queries;
    }

    public List<Operation> getOperations() {

        return operations;
    }

    public void setOperations(List<Operation> operations) {

        this.operations = operations;
    }

    public List<Resource> getResources() {

        return resources;
    }

    public void setResources(List<Resource> resources) {

        this.resources = resources;
    }

    public List<EventTrigger> getEventTriggers() {

        return eventTriggers;
    }

    public void setEventTriggers(List<EventTrigger> eventTriggers) {

        this.eventTriggers = eventTriggers;
    }

    public DataPolicy getPolicy() {

        return policy;
    }

    public void setPolicy(DataPolicy policy) {

        this.policy = policy;
    }

    public STNode getEnableSec() {

        return enableSec;
    }

    public void setEnableSec(STNode enableSec) {

        this.enableSec = enableSec;
    }

    public AuthorizationProvider getAuthorizationProvider() {

        return authorizationProvider;
    }

    public void setAuthorizationProvider(AuthorizationProvider authorizationProvider) {

        this.authorizationProvider = authorizationProvider;
    }

    public String getBaseURI() {

        return baseURI;
    }

    public void setBaseURI(String baseURI) {

        this.baseURI = baseURI;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public boolean isEnableBatchRequests() {

        return enableBatchRequests;
    }

    public void setEnableBatchRequests(boolean enableBatchRequests) {

        this.enableBatchRequests = enableBatchRequests;
    }

    public boolean isEnableBoxcarring() {

        return enableBoxcarring;
    }

    public void setEnableBoxcarring(boolean enableBoxcarring) {

        this.enableBoxcarring = enableBoxcarring;
    }

    public boolean isDisableLegacyBoxcarringMode() {

        return disableLegacyBoxcarringMode;
    }

    public void setDisableLegacyBoxcarringMode(boolean disableLegacyBoxcarringMode) {

        this.disableLegacyBoxcarringMode = disableLegacyBoxcarringMode;
    }

    public boolean isDisableStreaming() {

        return disableStreaming;
    }

    public void setDisableStreaming(boolean disableStreaming) {

        this.disableStreaming = disableStreaming;
    }

    public String getTxManagerJNDIName() {

        return txManagerJNDIName;
    }

    public void setTxManagerJNDIName(String txManagerJNDIName) {

        this.txManagerJNDIName = txManagerJNDIName;
    }

    public String getServiceNamespace() {

        return serviceNamespace;
    }

    public void setServiceNamespace(String serviceNamespace) {

        this.serviceNamespace = serviceNamespace;
    }

    public String getServiceGroup() {

        return serviceGroup;
    }

    public void setServiceGroup(String serviceGroup) {

        this.serviceGroup = serviceGroup;
    }

    public String getPublishSwagger() {

        return publishSwagger;
    }

    public void setPublishSwagger(String publishSwagger) {

        this.publishSwagger = publishSwagger;
    }

    public String getTransports() {

        return transports;
    }

    public void setTransports(String transports) {

        this.transports = transports;
    }

    public String getServiceStatus() {

        return serviceStatus;
    }

    public void setServiceStatus(String serviceStatus) {

        this.serviceStatus = serviceStatus;
    }

    public void addConfig(Config config) {

        if (configs == null) {
            configs = new ArrayList<>();
        }
        configs.add(config);
    }

    public void addQuery(Query query) {

        if (queries == null) {
            queries = new ArrayList<>();
        }
        queries.add(query);
    }

    public void addOperation(Operation operation) {

        if (operations == null) {
            operations = new ArrayList<>();
        }
        operations.add(operation);
    }

    public void addResource(Resource resource) {

        if (resources == null) {
            resources = new ArrayList<>();
        }
        resources.add(resource);
    }

    public void addEventTrigger(EventTrigger eventTrigger) {

        if (eventTriggers == null) {
            eventTriggers = new ArrayList<>();
        }
        eventTriggers.add(eventTrigger);
    }
}
