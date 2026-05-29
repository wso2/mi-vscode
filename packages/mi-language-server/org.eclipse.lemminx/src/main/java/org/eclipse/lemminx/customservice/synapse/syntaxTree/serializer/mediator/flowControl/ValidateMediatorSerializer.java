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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Feature;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.Validate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.ValidateOnFail;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.ValidateProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.ValidateResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.ValidateSchema;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.AnonymousSequenceSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class ValidateMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Validate validate = (Validate) m;
        OMElement validateElt = fac.createOMElement("validate", synNS);

        serializeAttributes(validateElt, validate);
        serializeProperty(validateElt, validate.getProperty());
        serializeSchema(validateElt, validate.getSchema());
        serializeFeature(validateElt, validate.getFeature());
        serializeResource(validateElt, validate.getResource());
        serializeOnFail(validateElt, validate.getOnFail());

        return validateElt;
    }

    private void serializeAttributes(OMElement validateElt, Validate validate) {

        if (validate.isCacheSchema()) {
            validateElt.addAttribute("cache-schema", String.valueOf(validate.isCacheSchema()), nullNS);
        }
        if (validate.getSource() != null) {
            SerializerUtils.serializeExpression(validate.getSource(), validateElt, "source", validate);
        }
        if (validate.getDescription() != null) {
            validateElt.addAttribute("description", validate.getDescription(), nullNS);
        }
    }

    private void serializeProperty(OMElement validateElt, ValidateProperty[] property) {

        if (property != null) {
            for (ValidateProperty prop : property) {
                OMElement propElt = fac.createOMElement("property", synNS);
                if (prop.getName() != null) {
                    propElt.addAttribute("name", prop.getName(), nullNS);
                }
                if (prop.isValue()) {
                    propElt.addAttribute("value", String.valueOf(prop.isValue()), nullNS);
                }
                validateElt.addChild(propElt);
            }
        }
    }

    private void serializeSchema(OMElement validateElt, ValidateSchema[] schemas) {

        if (schemas != null) {
            for (ValidateSchema schema : schemas) {
                OMElement schemaElt = fac.createOMElement("schema", synNS);
                if (schema.getKey() != null) {
                    schemaElt.addAttribute("key", schema.getKey(), nullNS);
                }
                validateElt.addChild(schemaElt);
            }
        }
    }

    private void serializeFeature(OMElement validateElt, Feature[] features) {

        if (features != null) {
            for (Feature feature : features) {
                OMElement featureElt = fac.createOMElement("feature", synNS);
                if (feature.getName() != null) {
                    featureElt.addAttribute("name", feature.getName(), nullNS);
                }
                featureElt.addAttribute("value", String.valueOf(feature.isValue()), nullNS);

                validateElt.addChild(featureElt);
            }
        }
    }

    private void serializeResource(OMElement validateElt, ValidateResource[] resources) {

        if (resources != null) {
            for (ValidateResource resource : resources) {
                OMElement resourceElt = fac.createOMElement("resource", synNS);
                if (resource.getKey() != null) {
                    resourceElt.addAttribute("key", resource.getKey(), nullNS);
                }
                if (resource.getLocation() != null) {
                    resourceElt.addAttribute("location", resource.getLocation(), nullNS);
                }
                validateElt.addChild(resourceElt);
            }
        }
    }

    private void serializeOnFail(OMElement validateElt, ValidateOnFail onFail) {

        OMElement onFailElt = fac.createOMElement("on-fail", synNS);
        if (onFail != null && onFail.getMediatorList() != null) {
            AnonymousSequenceSerializer.serializeAnonymousSequence(onFail.getMediatorList(), onFailElt);
        }
        validateElt.addChild(onFailElt);
    }

    @Override
    public String getMediatorClassName() {

        return Validate.class.getName();
    }
}
