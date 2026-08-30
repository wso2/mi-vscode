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

package org.eclipse.lemminx.customservice.synapse.mediator.schema.generate;

import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutUtils;
import org.eclipse.lemminx.customservice.synapse.mediator.schema.generate.visitor.SchemaVisitor;
import org.eclipse.lemminx.customservice.synapse.mediator.schema.generate.visitor.SchemaVisitorFactory;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpoint;
import org.eclipse.lemminx.customservice.synapse.utils.ConfigFinder;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ServerLessTryoutHandler {

    Path TEMP_FOLDER = Path.of(System.getProperty("user.home"), ".wso2-mi", "expression-temp");
    private static final String TEMP_FILE_NAME = "temp.xml";
    private final String projectUri;
    private final ConnectorHolder connectorHolder;

    public ServerLessTryoutHandler(String projectUri, ConnectorHolder connectorHolder) {

        this.projectUri = projectUri;
        this.connectorHolder = connectorHolder;
    }

    public MediatorTryoutInfo handle(MediatorTryoutRequest request) {

        try {
            String visitFilePath = request.getFile();
            if (request.getEdits() != null) {
                STNode node = getSTNode(request.getFile());
                String documentUri = request.getFile();
                String editFilePath = TEMP_FOLDER.resolve(TEMP_FILE_NAME).toString();
                if (node instanceof InboundEndpoint) {
                    String sequence = ((InboundEndpoint) node).getSequence();
                    if (StringUtils.isNotEmpty(sequence)) {
                        String seqPath = ConfigFinder.findEsbComponentPath(sequence, Constant.SEQUENCES, projectUri);
                        if (StringUtils.isNotEmpty(seqPath)) {
                            documentUri = seqPath;
                        }
                    }
                } else {
                    visitFilePath = editFilePath;
                }
                Utils.copyFile(documentUri, TEMP_FOLDER.toString(), TEMP_FILE_NAME);
                TryOutUtils.doEdits(request.getEdits(), Path.of(editFilePath));
                request = new MediatorTryoutRequest(editFilePath, request.getLine(), request.getColumn() + 1,
                        request.getInputPayload(), null);
            }
            DOMDocument domDocument = Utils.getDOMDocument(new File(visitFilePath));
            STNode node = SyntaxTreeGenerator.buildTree(domDocument.getDocumentElement());
            MediatorTryoutInfo mediatorTryoutInfo = createInitialMediatorTryoutInfo(request);
            if (node != null) {
                visitNode(node, request, mediatorTryoutInfo);
            }
            return mediatorTryoutInfo;
        } catch (IOException | InvalidConfigurationException e) {
            return new MediatorTryoutInfo(e.getMessage());
        }
    }

    private STNode getSTNode(String filePath) throws IOException, InvalidConfigurationException {

        if (StringUtils.isEmpty(filePath)) {
            throw new IllegalArgumentException("FilePath is null");
        }
        DOMDocument domDocument = Utils.getDOMDocument(new File(filePath));
        return SyntaxTreeGenerator.buildTree(domDocument.getDocumentElement());
    }

    private MediatorTryoutInfo createInitialMediatorTryoutInfo(MediatorTryoutRequest request) {

        MediatorInfo mediatorInfo = new MediatorInfo();
        JsonPrimitive payload = null;
        if (request.getInputPayload() != null) {
            payload = new JsonPrimitive(request.getInputPayload());
        }
        mediatorInfo.setPayload(payload);
        return new MediatorTryoutInfo(mediatorInfo, mediatorInfo.deepCopy());
    }

    private void visitNode(STNode node, MediatorTryoutRequest request, MediatorTryoutInfo mediatorTryoutInfo) {

        SchemaVisitor visitor = SchemaVisitorFactory.getSchemaVisitor(node, projectUri, connectorHolder);
        if (visitor != null) {
            visitor.visit(node, mediatorTryoutInfo, request);
        }
    }
}
