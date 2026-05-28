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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators;

import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.CacheFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.CloneFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.DBLookupFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.DBReportFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.DataServiceFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.EnqueueFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.EventFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.TransactionFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.ai.AIAddToKnowledgeFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.ai.AIAgentConnectorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.ai.AIChatConnectorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.ai.AIGetFromKnowledgeFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.ai.AIRAGChatConnectorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.CallFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.CallOutFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.CallTemplateFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.DropFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.HeaderFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.LogFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.LoopbackFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.PropertyFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.PropertyGroupFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.RespondFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.SendFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.StoreFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.ThrowErrorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.ValidateFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.VariableFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.eip.AggregateFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.eip.ForeachFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.eip.IterateFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.eip.ScatterGatherFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.extension.BeanFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.extension.ClassFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.extension.EjbFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.extension.PojoCommandFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.extension.ScriptFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.extension.SpringFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.filter.ConditionalRouterFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.filter.FilterFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.filter.SwitchFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.filter.ThrottleFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other.BamFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other.BuilderFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other.EntitlementFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other.NtlmFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other.OauthServiceFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other.PublishEventFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other.RuleFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.DataMapperFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.EnrichFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.FastXSLTFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.FaultFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.JsonTransformFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.PayloadFactoryFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.RewriteFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.SmooksFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.XqueryFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.XsltFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//adapted from org.apache.synapse.config.xml.MediatorFactoryFinder
public class MediatorFactoryFinder {

    private static final Logger log = Logger.getLogger(MediatorFactoryFinder.class.getName());
    private static final Class[] mediatorFactories = {
            CacheFactory.class,
            CloneFactory.class,
            DataServiceFactory.class,
            DBLookupFactory.class,
            DBReportFactory.class,
            EnqueueFactory.class,
            EventFactory.class,
            TransactionFactory.class,
            CallTemplateFactory.class,
            CallFactory.class,
            CallOutFactory.class,
            DropFactory.class,
            HeaderFactory.class,
            LogFactory.class,
            LoopbackFactory.class,
            PropertyFactory.class,
            PropertyGroupFactory.class,
            RespondFactory.class,
            SendFactory.class,
            StoreFactory.class,
            ValidateFactory.class,
            AggregateFactory.class,
            ForeachFactory.class,
            IterateFactory.class,
            BeanFactory.class,
            ClassFactory.class,
            PojoCommandFactory.class,
            EjbFactory.class,
            ScriptFactory.class,
            SpringFactory.class,
            ConditionalRouterFactory.class,
            FilterFactory.class,
            SwitchFactory.class,
            ThrottleFactory.class,
            BamFactory.class,
            BuilderFactory.class,
            OauthServiceFactory.class,
            EntitlementFactory.class,
            PublishEventFactory.class,
            RuleFactory.class,
            DataMapperFactory.class,
            EnrichFactory.class,
            FastXSLTFactory.class,
            FaultFactory.class,
            JsonTransformFactory.class,
            PayloadFactoryFactory.class,
            SmooksFactory.class,
            XqueryFactory.class,
            XsltFactory.class,
            NtlmFactory.class,
            RewriteFactory.class,
            SequenceMediatorFactory.class,
            ConnectorFactory.class,
            VariableFactory.class,
            ScatterGatherFactory.class,
            ThrowErrorFactory.class,
            AIAgentConnectorFactory.class,
            AIChatConnectorFactory.class,
            AIRAGChatConnectorFactory.class,
            AIGetFromKnowledgeFactory.class,
            AIAddToKnowledgeFactory.class
    };

    private final static MediatorFactoryFinder instance = new MediatorFactoryFinder();
    private Map<String, AbstractMediatorFactory> factoryMap = new HashMap<>();
    private ConnectorHolder connectorHolder;
    private boolean initialized = false;
    private String miVersion;
    private String projectPath;

    public static synchronized void init(String miVersion, String projectPath, ConnectorHolder connectorHolder) {

        if (!instance.initialized) {
            instance.setMiVersion(miVersion);
            instance.setProjectPath(projectPath);
            instance.setConnectorHolder(connectorHolder);
            instance.loadMediatorFactories();
        }
    }

    public static synchronized MediatorFactoryFinder getInstance() {

        if (!instance.initialized) {
            instance.loadMediatorFactories();
        }
        return instance;
    }

    private MediatorFactoryFinder() {

    }

    private void loadMediatorFactories() {

        for (Class c : mediatorFactories) {
            try {
                AbstractMediatorFactory fac = (AbstractMediatorFactory) c.newInstance();
                fac.setMiVersion(miVersion);
                fac.setProjectPath(projectPath);
                factoryMap.put(fac.getTagName().toLowerCase(), fac);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error instantiating " + c.getName(), e);
            }
        }
        initialized = true;
    }

    public Mediator getMediator(DOMNode node) {

        if (node != null && node instanceof DOMElement && node.getNodeName() != null) {
            AbstractMediatorFactory factory = getMediatorFactory(node.getNodeName().toLowerCase());
            if (factory != null) {
                Mediator mediator = (Mediator) factory.create((DOMElement) node);
                mediator.elementNode((DOMElement) node);
                return mediator;
            } else {
                Mediator invalidMediator = (Mediator) new InvalidMediatorFactory().create((DOMElement) node);
                return invalidMediator;
            }
        }
        return null;
    }

    private AbstractMediatorFactory getMediatorFactory(String mediatorName) {

        if (factoryMap.containsKey(mediatorName)) {
            return factoryMap.get(mediatorName);
        }
        if (mediatorName.contains(Constant.DOT) && connectorHolder.isValidConnector(mediatorName)) {
            return factoryMap.get(Constant.CONNECTOR);
        }
        return null;
    }

    public void setConnectorHolder(ConnectorHolder connectorHolder) {

        this.connectorHolder = connectorHolder;
    }

    public void setMiVersion(String miVersion) {

        this.miVersion = miVersion;
    }

    public void setProjectPath(String projectPath) {

        this.projectPath = projectPath;
    }
}
