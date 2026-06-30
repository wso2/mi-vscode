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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.qos;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.ntlm.Ntlm;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class NTLMMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Ntlm ntlm = (Ntlm) m;
        OMElement ntlmElt = fac.createOMElement("NTLM", synNS);

        serializeAttributes(ntlmElt, ntlm);

        return ntlmElt;
    }

    private void serializeAttributes(OMElement ntlmElt, Ntlm ntlm) {

        if (ntlm.getDomain() != null) {
            ntlmElt.addAttribute("domain", ntlm.getDomain(), nullNS);
        }
        if (ntlm.getHost() != null) {
            ntlmElt.addAttribute("host", ntlm.getHost(), nullNS);
        }
        if (ntlm.getUsername() != null) {
            ntlmElt.addAttribute("username", ntlm.getUsername(), nullNS);
        }
        if (ntlm.getPassword() != null) {
            ntlmElt.addAttribute("password", ntlm.getPassword(), nullNS);
        }
        if (ntlm.getNtlmVersion() != null) {
            ntlmElt.addAttribute("ntlmVersion", ntlm.getNtlmVersion(), nullNS);
        }
        if (ntlm.getDescription() != null) {
            ntlmElt.addAttribute("description", ntlm.getDescription(), nullNS);
        }
    }

    @Override
    public String getMediatorClassName() {

        return Ntlm.class.getName();
    }
}
