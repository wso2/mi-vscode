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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMComment;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.CommentMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class CommentMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        return null;
    }

    public static OMElement serializeComment(OMElement parent, CommentMediator m) {

        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMComment comment = fac.createOMComment(parent, m.getCommentText());
        if (parent != null) {
            parent.addChild(comment);
        }
        return null;
    }

    @Override
    public String getMediatorClassName() {

        return CommentMediator.class.getName();
    }
}
