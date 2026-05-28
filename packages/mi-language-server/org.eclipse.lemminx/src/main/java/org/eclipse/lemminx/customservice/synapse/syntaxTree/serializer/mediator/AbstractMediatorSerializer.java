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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.CommentMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractMediatorSerializer {

    private static Logger log = Logger.getLogger(AbstractMediatorSerializer.class.getName());
    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS = Constant.SYNAPSE_OMNAMESPACE;
    protected static final OMNamespace nullNS
            = fac.createOMNamespace(Constant.EMPTY_STRING, "");

    /**
     * Return the XML representation of this mediator
     *
     * @param m      mediator to be serialized
     * @param parent the OMElement to which the serialization should be attached
     * @return the serialized mediator XML
     */
    public final OMElement serializeMediator(OMElement parent, Mediator m) {

        if (m instanceof CommentMediator) {
            return ((CommentMediatorSerializer) this).serializeComment(parent, (CommentMediator) m);
        }
        OMElement mediatorElt = serializeSpecificMediator(m);
        if (parent != null) {
            parent.addChild(mediatorElt);
        }
        return mediatorElt;
    }

    protected abstract OMElement serializeSpecificMediator(Mediator m);

    /**
     * Return the class name of the mediator which can be serialized
     *
     * @return the class name
     */
    public abstract String getMediatorClassName();

    protected void serializeMediatorProperties(OMElement mediatorElt, MediatorProperty[] properties) {

        if (properties != null) {
            for (MediatorProperty property : properties) {
                OMElement propertyElt = serializeMediatorProperty(property);
                mediatorElt.addChild(propertyElt);
            }
        }
    }

    private OMElement serializeMediatorProperty(MediatorProperty property) {

        OMElement propertyElt = fac.createOMElement("property", synNS);
        if (property.getName() != null) {
            propertyElt.addAttribute("name", property.getName(), nullNS);
        } else {
            handleException("Mediator property must have a name");
        }
        if (property.getValue() != null) {
            propertyElt.addAttribute("value", property.getValue(), nullNS);
        } else if (property.getExpression() != null) {
            SerializerUtils.serializeExpression(property.getExpression(), propertyElt, "expression", property);
        } else {
            handleException("Mediator property must have a literal value or be an expression");
        }
        return propertyElt;
    }

    protected void handleException(String s) {

        log.log(Level.SEVERE, s);
        throw new InvalidConfigurationException(s);
    }
}