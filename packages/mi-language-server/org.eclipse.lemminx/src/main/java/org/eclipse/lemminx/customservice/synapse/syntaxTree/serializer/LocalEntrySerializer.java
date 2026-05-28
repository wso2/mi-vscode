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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.LocalEntry;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalEntrySerializer {

    private static Logger log = Logger.getLogger(LocalEntrySerializer.class.getName());
    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    public static String serializeLocalEntry(LocalEntry localEntry) {

        OMElement localEntryElt = fac.createOMElement("localEntry", Constant.SYNAPSE_OMNAMESPACE);

        if (localEntry.getKey() != null) {
            localEntryElt.addAttribute("key", localEntry.getKey(), null);
        } else {
            handleException("Local Entry key is required");
        }
        if (localEntry.getSrc() != null) {
            localEntryElt.addAttribute("src", localEntry.getSrc(), null);
        } else if (localEntry.getContent() != null) {
            OMElement contentElt = SerializerUtils.stringToOM(localEntry.getContent().toString());
            localEntryElt.addChild(contentElt);
        }
        return localEntryElt.toString();
    }

    private static void handleException(String s) {

        log.log(Level.SEVERE, s);
        throw new InvalidConfigurationException(s);
    }
}
