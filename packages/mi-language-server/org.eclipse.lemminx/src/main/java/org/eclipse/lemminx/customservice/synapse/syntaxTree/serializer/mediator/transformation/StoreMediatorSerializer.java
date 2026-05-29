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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Store;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class StoreMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Store storeMediator = (Store) m;
        OMElement storeElt = fac.createOMElement("store", synNS);

        if (storeMediator.getMessageStore() != null) {
            storeElt.addAttribute("messageStore", storeMediator.getMessageStore(), null);
        }
        if (storeMediator.getSequence() != null) {
            storeElt.addAttribute("sequence", storeMediator.getSequence(), null);
        }
        if (storeMediator.getDescription() != null) {
            storeElt.addAttribute("description", storeMediator.getDescription(), null);
        }
        return storeElt;
    }

    @Override
    public String getMediatorClassName() {

        return Store.class.getName();
    }
}
