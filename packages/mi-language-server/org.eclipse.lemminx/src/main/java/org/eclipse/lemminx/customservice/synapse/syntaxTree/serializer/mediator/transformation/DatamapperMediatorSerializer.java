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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.Datamapper;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class DatamapperMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Datamapper datamapper = (Datamapper) m;
        OMElement datamapperElt = fac.createOMElement("datamapper", synNS);

        if (datamapper.getInputType() != null) {
            datamapperElt.addAttribute("inputType", datamapper.getInputType().name(), null);
        }
        if (datamapper.getInputSchema() != null) {
            datamapperElt.addAttribute("inputSchema", datamapper.getInputSchema(), null);
        }
        if (datamapper.getOutputType() != null) {
            datamapperElt.addAttribute("outputType", datamapper.getOutputType().name(), null);
        }
        if (datamapper.getOutputSchema() != null) {
            datamapperElt.addAttribute("outputSchema", datamapper.getOutputSchema(), null);
        }
        if (datamapper.getConfig() != null) {
            datamapperElt.addAttribute("config", datamapper.getConfig(), null);
        }
        if (datamapper.getXsltStyleSheet() != null) {
            datamapperElt.addAttribute("xsltStyleSheet", datamapper.getXsltStyleSheet(), null);
        }
        if (datamapper.getDescription() != null) {
            datamapperElt.addAttribute("description", datamapper.getDescription(), null);
        }
        return datamapperElt;
    }

    @Override
    public String getMediatorClassName() {

        return Datamapper.class.getName();
    }
}
