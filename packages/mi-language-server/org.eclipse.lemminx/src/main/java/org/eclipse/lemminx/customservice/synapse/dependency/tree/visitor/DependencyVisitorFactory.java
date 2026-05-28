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

import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.DependencyTree;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

public class DependencyVisitorFactory {

    private final String projectPath;

    public DependencyVisitorFactory(String projectPath) {

        this.projectPath = projectPath;
    }

    /**
     * Create a visitor based on the tag.
     *
     * @param tag            tag of the visitor
     * @param dependencyTree dependency tree
     * @return visitor
     */
    public AbstractDependencyVisitor createVisitor(String tag, DependencyTree dependencyTree) {

        switch (tag) {
            case Constant.API:
                return new APIVisitor(dependencyTree, projectPath);
            case Constant.ENDPOINT:
                return new EndpointVisitor(dependencyTree, projectPath);
            case Constant.SEQUENCE:
                return new SequenceVisitor(dependencyTree, projectPath);
            case Constant.PROXY:
                return new ProxyServiceVisitor(dependencyTree, projectPath);
            case Constant.INBOUND_ENDPOINT:
                return new InboundEndpointVisitor(dependencyTree, projectPath);
            case Constant.MESSAGE_STORE:
                return new MessageStoreVisitor(dependencyTree, projectPath);
            case Constant.MESSAGE_PROCESSOR:
                return new MessageProcessorVisitor(dependencyTree, projectPath);
            case Constant.TASK:
                return new TaskVisitor(dependencyTree, projectPath);
            case Constant.LOCAL_ENTRY:
                return new LocalEntryVisitor(dependencyTree, projectPath);
            case Constant.TEMPLATE:
                return new TemplateVisitor(dependencyTree, projectPath);
            case Constant.DATA:
                return new DataServiceVisitor(dependencyTree, projectPath);
            case Constant.DATA_SOURCE:
                return new DataSourceVisitor(dependencyTree, projectPath);
            default:
                throw new IllegalStateException("Invalid tag: " + tag);
        }
    }
}
