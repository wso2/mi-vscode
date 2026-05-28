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

package org.eclipse.lemminx.customservice.synapse.debugger.visitor.stepover;

import org.eclipse.lemminx.customservice.synapse.debugger.entity.Breakpoint;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.StepOverInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.Visitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.VisitorUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.Template;

public class StepOverTemplateVisitor implements Visitor {

    Template syntaxTree;
    Breakpoint breakpoint;
    StepOverInfo stepOverInfo;

    public StepOverTemplateVisitor(Template syntaxTree, Breakpoint breakpoint, StepOverInfo stepOverInfo) {

        this.syntaxTree = syntaxTree;
        this.breakpoint = breakpoint;
        this.stepOverInfo = stepOverInfo;
    }

    @Override
    public void startVisit() {

        traverseNode(syntaxTree, breakpoint);

    }

    private void traverseNode(Template syntaxTree, Breakpoint breakpoint) {

        if (syntaxTree == null) {
            return;
        }
        if (VisitorUtils.checkNodeInRange(syntaxTree, breakpoint)) {
            StepOverMediatorVisitor mediatorVisitor = new StepOverMediatorVisitor(breakpoint, stepOverInfo);
            VisitorUtils.visitMediators(syntaxTree.getSequence().getMediatorList(), mediatorVisitor);
        }
    }
}
