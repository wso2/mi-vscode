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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.bam.Bam;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.bam.BamServerProfile;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.bam.BamServerProfileStreamConfig;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public class BamFactory extends AbstractMediatorFactory {

    private static final String BAM = "bam";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Bam bam = new Bam();
        bam.elementNode(element);
        populateAttributes(bam, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.SERVER_PROFILE)) {
                    BamServerProfile serverProfile = createBamServerProfile(child);
                    bam.setServerProfile(serverProfile);
                }
            }
        }
        return bam;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Bam) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Bam) node).setTraceFilter(traceFilter);
        }
    }

    private BamServerProfile createBamServerProfile(DOMNode node) {

        BamServerProfile bamServerProfile = new BamServerProfile();
        bamServerProfile.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            bamServerProfile.setName(name);
        }
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.STREAM_CONFIG)) {
                    BamServerProfileStreamConfig streamConfig = createBamServerProfileStreamConfig(child);
                    bamServerProfile.setStreamConfig(streamConfig);
                }
            }
        }
        return bamServerProfile;
    }

    private BamServerProfileStreamConfig createBamServerProfileStreamConfig(DOMNode node) {

        BamServerProfileStreamConfig bamServerProfileStreamConfig = new BamServerProfileStreamConfig();
        bamServerProfileStreamConfig.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            bamServerProfileStreamConfig.setName(name);
        }
        String version = node.getAttribute(Constant.VERSION);
        if (version != null) {
            bamServerProfileStreamConfig.setVersion(version);
        }
        return bamServerProfileStreamConfig;
    }

    @Override
    public String getTagName() {

        return BAM;
    }
}
