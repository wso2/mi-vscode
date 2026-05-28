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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.OauthService;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class OauthMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        OauthService oauthService = (OauthService) m;
        OMElement oauthServiceElt = fac.createOMElement("oauthService", synNS);

        serializeAttributes(oauthService, oauthServiceElt);

        return oauthServiceElt;
    }

    private void serializeAttributes(OauthService oauthService, OMElement oauthServiceElt) {

        if (oauthService.getRemoteServiceUrl() != null) {
            oauthServiceElt.addAttribute("remoteServiceUrl", oauthService.getRemoteServiceUrl(), nullNS);
        }
        if (oauthService.getUsername() != null) {
            oauthServiceElt.addAttribute("username", oauthService.getUsername(), nullNS);
        }
        if (oauthService.getPassword() != null) {
            oauthServiceElt.addAttribute("password", oauthService.getPassword(), nullNS);
        }
        if (oauthService.getDescription() != null) {
            oauthServiceElt.addAttribute("description", oauthService.getDescription(), nullNS);
        }
    }

    @Override
    public String getMediatorClassName() {

        return OauthService.class.getName();
    }
}
