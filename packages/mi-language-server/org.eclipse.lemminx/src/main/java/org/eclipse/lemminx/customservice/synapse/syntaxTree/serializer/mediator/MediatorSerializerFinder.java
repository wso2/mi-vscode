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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Loopback;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.CallMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.CallTemplateMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.DropMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.LogMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.PropertyGroupMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.PropertyMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.RespondMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.SendMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.SequenceMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.data.DBLookupMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.data.DBReportMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.data.DataServiceCallMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.extension.ScriptMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.AggregateMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.CloneMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.FilterMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.ForeachMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.IterateMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.SwitchMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.ValidateMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.other.LoopbackMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.qos.CacheMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.qos.EntitlementMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.qos.NTLMMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.qos.OauthMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.qos.ThrottleMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.DatamapperMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.EnrichMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.HeaderMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.JsonTransformMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.PayloadFactoryMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.StoreMediatorSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.XsltMediatorSerializer;

import java.util.HashMap;
import java.util.Map;

public class MediatorSerializerFinder {

    private static final Class[] mediatorSerializers = {
            ConnectorSerializer.class,
            //Core Mediators
            CallMediatorSerializer.class,
            CallTemplateMediatorSerializer.class,
            DropMediatorSerializer.class,
            LogMediatorSerializer.class,
            PropertyMediatorSerializer.class,
            PropertyGroupMediatorSerializer.class,
            RespondMediatorSerializer.class,
            SendMediatorSerializer.class,
            SequenceMediatorSerializer.class,
            //Data Mediators
            DataServiceCallMediatorSerializer.class,
            DBLookupMediatorSerializer.class,
            DBReportMediatorSerializer.class,
            //Extension Mediators
            CallMediatorSerializer.class,
            ScriptMediatorSerializer.class,
            //Flow Control Mediators
            AggregateMediatorSerializer.class,
            CloneMediatorSerializer.class,
            FilterMediatorSerializer.class,
            ForeachMediatorSerializer.class,
            IterateMediatorSerializer.class,
            SwitchMediatorSerializer.class,
            ValidateMediatorSerializer.class,
            //QOS
            CacheMediatorSerializer.class,
            EntitlementMediatorSerializer.class,
            NTLMMediatorSerializer.class,
            OauthMediatorSerializer.class,
            ThrottleMediatorSerializer.class,
            //Transformation Mediators
            DatamapperMediatorSerializer.class,
            EnrichMediatorSerializer.class,
            HeaderMediatorSerializer.class,
            JsonTransformMediatorSerializer.class,
            PayloadFactoryMediatorSerializer.class,
            StoreMediatorSerializer.class,
            XsltMediatorSerializer.class,
            //Other Mediators
            LoopbackMediatorSerializer.class,
            CommentMediatorSerializer.class,
    };

    private final static MediatorSerializerFinder instance = new MediatorSerializerFinder();
    private final Map<String, AbstractMediatorSerializer> serializerMap
            = new HashMap<>();

    public static synchronized MediatorSerializerFinder getInstance() {

        return instance;
    }

    public AbstractMediatorSerializer getSerializer(Mediator mediator) {

        return serializerMap.get(mediator.getClass().getName());
    }

    private MediatorSerializerFinder() {

        for (Class c : mediatorSerializers) {
            try {
                AbstractMediatorSerializer ser = (AbstractMediatorSerializer) c.newInstance();
                serializerMap.put(ser.getMediatorClassName(), ser);
            } catch (Exception e) {
                //handle exception
            }
        }
    }

    public Map<String, AbstractMediatorSerializer> getSerializerMap() {

        return serializerMap;
    }
}
