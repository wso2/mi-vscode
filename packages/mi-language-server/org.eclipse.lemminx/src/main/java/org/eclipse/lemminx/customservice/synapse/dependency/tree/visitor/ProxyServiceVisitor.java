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

package org.eclipse.lemminx.customservice.synapse.dependency.tree.visitor;

import org.eclipse.lemminx.customservice.synapse.dependency.tree.ArtifactType;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.DependencyLookUp;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.DependencyVisitorUtils;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.Dependency;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.DependencyTree;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.Proxy;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.ProxyPolicy;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.ProxyPublishWSDL;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.ProxyTarget;

import java.util.List;

public class ProxyServiceVisitor extends AbstractDependencyVisitor {

    public ProxyServiceVisitor(DependencyTree dependencyTree, String projectPath) {

        super(dependencyTree, projectPath, new DependencyLookUp());
    }

    @Override
    public void visit(STNode node) {

        Proxy proxy = (Proxy) node;

        ProxyTarget target = proxy.getTarget();
        if (target != null) {
            visitProxyTarget(target);
        }

        ProxyPublishWSDL publishWSDL = proxy.getPublishWSDL();
        if (publishWSDL != null) {
            visitPublishWSDL(publishWSDL);
        }

        ProxyPolicy[] policies = proxy.getPolicies();
        if (policies != null) {
            visitPolicies(policies);
        }

    }

    private void visitProxyTarget(ProxyTarget target) {

        processTargetSequence(target.getInSequenceAttribute(), target.getInSequence());
        processTargetSequence(target.getOutSequenceAttribute(), target.getOutSequence());
        processTargetSequence(target.getFaultSequenceAttribute(), target.getFaultSequence());
        processTargetEndpoint(target.getEndpointAttribute(), target.getEndpoint());
    }

    private void processTargetEndpoint(String endpointAttribute, NamedEndpoint endpoint) {

        if (endpointAttribute != null) {
            Dependency dependency =
                    DependencyVisitorUtils.visitEndpoint(endpointAttribute, projectPath, dependencyLookUp);
            addDependency(dependency);
        } else if (endpoint != null) {
            Dependency dependency = DependencyVisitorUtils.visitEndpoint(endpoint, projectPath, dependencyLookUp);
            addDependency(dependency);
        }
    }

    private void processTargetSequence(String sequenceAttribute, Sequence sequence) {

        if (sequenceAttribute != null) {
            Dependency dependency =
                    DependencyVisitorUtils.visitSequence(projectPath, sequenceAttribute, dependencyLookUp);
            addDependency(dependency);
        } else if (sequence != null) {
            List<Dependency> dependencies =
                    DependencyVisitorUtils.visitAnonymousSequence(sequence, projectPath, dependencyLookUp);
            addDependencies(dependencies);
        }
    }

    private void visitPublishWSDL(ProxyPublishWSDL publishWSDL) {

        if (publishWSDL.getUri() != null) {
            Dependency dependency = new Dependency(publishWSDL.getKey(), ArtifactType.WSDL, publishWSDL.getUri());
            addDependency(dependency);
        }

        if (publishWSDL.getKey() != null) {
            String path = DependencyVisitorUtils.getDependencyPath(publishWSDL.getKey(), ArtifactType.WSDL.name(),
                    projectPath);
            if (path != null) {
                Dependency dependency = new Dependency(publishWSDL.getKey(), ArtifactType.WSDL, path);
                addDependency(dependency);
            }
        }

        if (publishWSDL.getEndpoint() != null) {
            Dependency dependency =
                    DependencyVisitorUtils.visitEndpoint(publishWSDL.getEndpoint(), projectPath, dependencyLookUp);
            addDependency(dependency);
        }
    }

    private void visitPolicies(ProxyPolicy[] policies) {

        for (ProxyPolicy policy : policies) {
            visitPolicy(policy);
        }
    }

    private void visitPolicy(ProxyPolicy policy) {

        String policyKey = policy.getKey();
        if (policyKey != null) {
            String path = DependencyVisitorUtils.getDependencyPath(policyKey, ArtifactType.POLICY.name(), projectPath);
            if (path != null) {
                Dependency dependency = new Dependency(policyKey, ArtifactType.POLICY, path);
                addDependency(dependency);
            }
        }
    }
}
