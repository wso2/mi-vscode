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

package org.eclipse.lemminx.customservice.synapse.mediator.schema.generate.visitor;

import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Params;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.NamedSequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.Template;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.TemplateParameter;

public class SequenceTemplateVisitor implements SchemaVisitor {

    private String projectPath;

    public SequenceTemplateVisitor(String projectPath) {

        this.projectPath = projectPath;
    }

    @Override
    public void visit(STNode node, MediatorTryoutInfo info, MediatorTryoutRequest request) {

        if (node instanceof Template) {
            visit((Template) node, info, request);
        }
    }

    private void visit(Template node, MediatorTryoutInfo info, MediatorTryoutRequest request) {

        populateFunctionParams(node, info);
        NamedSequence sequence = node.getSequence();
        if (sequence != null) {
            SchemaVisitor visitor = SchemaVisitorFactory.getSchemaVisitor(sequence, projectPath);
            if (visitor != null) {
                visitor.visit(sequence, info, request);
            }
        }
    }

    private void populateFunctionParams(Template node, MediatorTryoutInfo info) {

        TemplateParameter[] parameters = node.getParameter();
        if (parameters != null) {
            Params params = new Params();
            for (TemplateParameter parameter : parameters) {
                String name = parameter.getName();
                String value = parameter.getDefaultValue();
                params.addFunctionParam(new Property(name, value));
            }
            info.setInputParams(params);
            info.setOutputParams(params.deepCopy());
        }
    }
}
