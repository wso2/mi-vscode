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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault.Makefault;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault.MakefaultCode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault.MakefaultDetail;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault.MakefaultReason;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class FaultMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Makefault makefault = (Makefault) m;
        OMElement makefaultElt = fac.createOMElement("makefault", synNS);

        serializeAttributes(makefaultElt, makefault);
        serializeCode(makefaultElt, makefault.getCode());
        serializeReason(makefaultElt, makefault.getReason());
        serializeNode(makefaultElt, makefault.getNode());
        serializeRole(makefaultElt, makefault.getRole());
        serializeDetail(makefaultElt, makefault.getDetail());

        return makefaultElt;
    }

    private void serializeAttributes(OMElement makefaultElt, Makefault makefault) {

        if (makefault.getVersion() != null) {
            makefaultElt.addAttribute("version", makefault.getVersion().name(), nullNS);
        }
        if (makefault.isResponse()) {
            makefaultElt.addAttribute("response", "true", nullNS);
        }
        if (makefault.getDescription() != null) {
            makefaultElt.addAttribute("description", makefault.getDescription(), nullNS);
        }
    }

    private void serializeCode(OMElement makefaultElt, MakefaultCode code) {

        if (code != null) {
            OMElement codeElt = fac.createOMElement("code", synNS);
            if (code.getExpression() != null) {
                SerializerUtils.serializeExpression(code.getExpression(), codeElt, "expression", code);
            } else if (code.getValue() != null) {
                SerializerUtils.serializeExpression(code.getValue(), codeElt, "value", code);
            }
            makefaultElt.addChild(codeElt);
        }
    }

    private void serializeReason(OMElement makefaultElt, MakefaultReason reason) {

        if (reason != null) {
            OMElement reasonElt = fac.createOMElement("reason", synNS);
            if (reason.getExpression() != null) {
                SerializerUtils.serializeExpression(reason.getExpression(), reasonElt, "expression", reason);
            } else {
                reasonElt.setText(reason.getTextNode());
            }
            makefaultElt.addChild(reasonElt);
        }
    }

    private void serializeNode(OMElement makefaultElt, STNode node) {

        if (node != null) {
            OMElement nodeElt = fac.createOMElement("node", synNS);
            nodeElt.setText(node.getTextNode());
            makefaultElt.addChild(nodeElt);
        }
    }

    private void serializeRole(OMElement makefaultElt, STNode role) {

        if (role != null) {
            OMElement roleElt = fac.createOMElement("role", synNS);
            roleElt.setText(role.getTextNode());
            makefaultElt.addChild(roleElt);
        }
    }

    private void serializeDetail(OMElement makefaultElt, MakefaultDetail detail) {

        if (detail != null) {
            OMElement detailElt = fac.createOMElement("detail", synNS);
            if (detail.getExpression() != null) {
                SerializerUtils.serializeExpression(detail.getExpression(), detailElt, "expression", detail);
            } else {
                detailElt.setText(detail.getTextNode());
            }
            makefaultElt.addChild(detailElt);
        }
    }

    @Override
    public String getMediatorClassName() {

        return Makefault.class.getName();
    }
}
