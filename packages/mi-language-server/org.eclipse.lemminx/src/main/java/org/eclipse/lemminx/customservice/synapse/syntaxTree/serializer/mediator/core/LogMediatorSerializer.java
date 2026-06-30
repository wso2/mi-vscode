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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Log;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.LogCategory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.LogLevel;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class LogMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Log log = (Log) m;
        OMElement logElt = fac.createOMElement("log", synNS);
        if (log.getLevel() != null && log.getLevel() != LogLevel.simple) {
            logElt.addAttribute("level", log.getLevel().name(), nullNS);
        }

        if (log.getCategory() != null && log.getCategory() != LogCategory.INFO) {
            logElt.addAttribute("category", log.getCategory().name(), nullNS);
        }

        if (log.getSeparator() != null) {
            logElt.addAttribute("separator", log.getSeparator(), nullNS);
        }

        if (log.getDescription() != null) {
            logElt.addAttribute("description", log.getDescription(), nullNS);
        }

        serializeMediatorProperties(logElt, log.getProperty());

        return logElt;
    }

    @Override
    public String getMediatorClassName() {

        return Log.class.getName();
    }
}
