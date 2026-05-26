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

package org.eclipse.lemminx.customservice.synapse.expression.pojo;

import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExpressionCompletionContext {

    private ExpressionCompletionContext parent;
    private List<String> segment;
    private Range range;
    private ExpressionCompletionType type;

    // Whether object traversal is needed for the current or next level.
    private boolean needNext;

    public ExpressionCompletionContext(ExpressionCompletionContext parent) {

        this.parent = parent.deepCopy();
        segment = new ArrayList<>();
    }

    public ExpressionCompletionContext(ExpressionCompletionContext parent, ExpressionCompletionType type) {

        this.parent = parent;
        this.type = type;
        segment = new ArrayList<>();
    }

    public ExpressionCompletionContext() {

        segment = new ArrayList<>();
        type = ExpressionCompletionType.ROOT_LEVEL;
    }

    public ExpressionCompletionContext(ExpressionCompletionContext parent, List<String> segment, Range range) {

        this.parent = parent.deepCopy();
        this.segment = new ArrayList<>(segment);
        this.range = range;
    }

    public void addSegment(String segment) {

        this.segment.add(segment);
    }

    public void setRange(Range range) {

        this.range = range;
    }

    public ExpressionCompletionContext getParent() {

        if (parent == null) {
            return null;
        }
        return parent.deepCopy();
    }

    public List<String> getSegment() {

        return Collections.unmodifiableList(segment);
    }

    public Range getRange() {

        return range;
    }

    public ExpressionCompletionType getType() {

        return type;
    }

    public void setType(ExpressionCompletionType type) {

        this.type = type;
    }

    public boolean isNeedNext() {

        return needNext;
    }

    public void setNeedNext(boolean needNext) {

        this.needNext = needNext;
    }

    public void pop() {

        segment.remove(segment.size() - 1);
    }

    public ExpressionCompletionContext deepCopy() {

        ExpressionCompletionContext clone = new ExpressionCompletionContext();
        clone.segment = new ArrayList<>(segment);
        clone.range = range;
        clone.type = type;
        clone.needNext = needNext;
        if (parent != null) {
            clone.parent = parent.deepCopy();
        }
        return clone;
    }
}
