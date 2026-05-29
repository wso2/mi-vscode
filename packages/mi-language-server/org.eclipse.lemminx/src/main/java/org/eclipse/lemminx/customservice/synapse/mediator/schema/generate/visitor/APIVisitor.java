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

import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Params;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIResource;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lsp4j.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class APIVisitor implements SchemaVisitor {

    private String projectPath;

    public APIVisitor(String projectPath) {

        this.projectPath = projectPath;
    }

    @Override
    public void visit(STNode node, MediatorTryoutInfo info, MediatorTryoutRequest request) {

        if (node instanceof API) {
            visit((API) node, info, request);
        }
    }

    private void visit(API api, MediatorTryoutInfo info, MediatorTryoutRequest request) {

        int line = request.getLine();
        int column = request.getColumn();
        Position position = new Position(line, column);
        if (Utils.checkNodeInRange(api, position)) {
            APIResource[] resources = api.getResource();
            if (resources != null) {
                for (APIResource resource : resources) {
                    if (Utils.checkNodeInRange(resource, position)) {
                        visitResource(resource, info, position);
                    }
                }
            }
        }
    }

    private void visitResource(APIResource resource, MediatorTryoutInfo info, Position position) {

        updateResourceParams(resource, info);
        if (needToVisit(resource.getInSequence(), position) || needToVisit(resource.getOutSequence(), position)) {
            Utils.visitSequence(projectPath, resource.getInSequence(), info, position);
            Utils.visitSequence(projectPath, resource.getOutSequence(), info, position);
        } else if (needToVisit(resource.getFaultSequence(), position)) {
            Utils.visitSequence(projectPath, resource.getFaultSequence(), info, position);
        }
    }

    private static boolean needToVisit(Sequence sequence, Position position) {

        if (!Utils.checkNodeInRange(sequence, position)) {
            return false;
        }
        List<Mediator> mediatorList = sequence.getMediatorList();
        if (mediatorList.isEmpty()) {
            return false;
        }
        Mediator firstMediator = mediatorList.get(0);
        int mediatorStartLine = firstMediator.getRange().getStartTagRange().getStart().getLine();
        int mediatorStartColumn = firstMediator.getRange().getStartTagRange().getStart().getCharacter();
        return position.getLine() > mediatorStartLine ||
                (position.getLine() == mediatorStartLine && position.getCharacter() >= mediatorStartColumn);
    }

    private void updateResourceParams(APIResource resource, MediatorTryoutInfo info) {

        String uri = resource.getUriTemplate();
        if (uri != null) {
            Params params = new Params();
            params.setPathParams(getPathParams(uri).stream().map(p -> new Property(p, "")).collect(Collectors.toList()));
            params.setQueryParams(
                    getQueryParams(uri).stream().map(p -> new Property(p, "")).collect(Collectors.toList()));
            info.setInputParams(params);
            info.setOutputParams(params.deepCopy());
        }
    }

    private List<String> getPathParams(String uri) {

        List<String> pathParams = new ArrayList<>();
        for (String part : uri.split("\\?")[0].split("/")) {
            if (part.startsWith("{") && part.endsWith("}")) {
                pathParams.add(part.substring(1, part.length() - 1));
            }
        }
        return pathParams;
    }

    private List<String> getQueryParams(String uri) {

        List<String> queryParams = new ArrayList<>();
        if (uri.contains("?")) {
            String queryParamPart = uri.split("\\?")[1];
            for (String part : queryParamPart.split("&")) {
                queryParams.add(part.split("=")[0]);
            }
        }
        return queryParams;
    }
}
