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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout;

import org.eclipse.lemminx.customservice.synapse.dependency.tree.DependencyLookUp;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.Dependency;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.visitor.MediatorDependencyVisitor;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutUtils;
import org.eclipse.lemminx.customservice.synapse.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.MediatorFactoryFinder;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lsp4j.Position;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants.TEMP_FOLDER_PATH;

public class IsolatedTryOutHandler {

    private static final Logger LOGGER = Logger.getLogger(IsolatedTryOutHandler.class.getName());
    private String projectRoot;
    private final TryOutHandler tryOutHandler;

    public IsolatedTryOutHandler(TryOutHandler tryOutHandler, String projectRoot) {

        this.tryOutHandler = tryOutHandler;
        this.projectRoot = projectRoot;
    }

    public MediatorTryoutInfo tryOut(MediatorTryoutRequest request) {

        String mediatorString = request.getEdits()[0].getText();
        DOMDocument dom = Utils.getDOMDocument(mediatorString);
        if (dom == null) {
            return new MediatorTryoutInfo("Invalid mediator content");
        }

        Mediator mediator = MediatorFactoryFinder.getInstance().getMediator(dom.getDocumentElement());
        if (mediator == null) {
            return new MediatorTryoutInfo("Invalid mediator content");
        }
        String tempProjectPath = TEMP_FOLDER_PATH.resolve(mediator.getTag() + "_" + UUID.randomUUID()).toString();
        String tryoutApi;
        try {
            tryoutApi = TryOutUtils.createAPI(mediator, tempProjectPath);
            copyDependencies(mediator, tempProjectPath);
            Position position = TryOutUtils.getMediatorPosition(tryoutApi, 0, 0);
            MediatorTryoutRequest mediatorTryoutRequest =
                    new MediatorTryoutRequest(tryoutApi, position.getLine(), position.getCharacter(),
                            request.getInputPayload(), null);
            mediatorTryoutRequest.setMediatorInfo(request.getMediatorInfo());
            return tryOutHandler.handleIsolatedTryOut(tempProjectPath, mediatorTryoutRequest, false, new Properties());
        } catch (InvalidConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error while creating the API for the mediator tryout", e);
            return new MediatorTryoutInfo("Error while creating the API for the mediator");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while copying the dependencies for tryout", e);
            return new MediatorTryoutInfo("Error while copying the dependencies");
        }
    }

    private void copyDependencies(Mediator mediator, String tempProjectPath) {

        MediatorDependencyVisitor visitor = new MediatorDependencyVisitor(projectRoot, new DependencyLookUp());
        visitor.visit(mediator);
        List<Dependency> dependencies = visitor.getDependencies();
        if (dependencies != null && !dependencies.isEmpty()) {
            for (Dependency dependency : dependencies) {
                String path = dependency.getPath();
                String targetPath = null;
                switch (dependency.getType()) {
                    case API:
                        targetPath = Path.of(tempProjectPath, "src", "main", "wso2mi", "artifacts", "apis").toString();
                        break;
                    case ENDPOINT:
                        targetPath =
                                Path.of(tempProjectPath, "src", "main", "wso2mi", "artifacts", "endpoints").toString();
                        break;
                    case SEQUENCE:
                        targetPath =
                                Path.of(tempProjectPath, "src", "main", "wso2mi", "artifacts", "sequences").toString();
                        break;
                    case TEMPLATE:
                        targetPath =
                                Path.of(tempProjectPath, "src", "main", "wso2mi", "artifacts", "templates").toString();
                        break;
                    case CONNECTION:
                    case LOCAL_ENTRY:
                        targetPath = Path.of(tempProjectPath, "src", "main", "wso2mi", "artifacts", "local-entries")
                                .toString();
                        break;
                    case MESSAGE_STORE:
                        targetPath = Path.of(tempProjectPath, "src", "main", "wso2mi", "artifacts", "message-stores")
                                .toString();
                        break;
                    case DATA_SERVICE:
                        targetPath = Path.of(tempProjectPath, "src", "main", "wso2mi", "artifacts", "data-services")
                                .toString();
                        break;
                    case DATASOURCE:
                        targetPath = Path.of(tempProjectPath, "src", "main", "wso2mi", "artifacts", "datasources")
                                .toString();
                        break;
                    case INBOUND_ENDPOINT:
                        targetPath = Path.of(tempProjectPath, "src", "main", "wso2mi", "artifacts", "inbound-endpoints")
                                .toString();
                        break;
                    case MESSAGE_PROCESSOR:
                        targetPath =
                                Path.of(tempProjectPath, "src", "main", "wso2mi", "artifacts", "message-processors")
                                        .toString();
                        break;
                }
                if (targetPath != null) {
                    try {
                        Utils.copyFile(path, targetPath);
                    } catch (IOException e) {
                        System.out.println("Error while copying the dependencies");
                    }
                }
            }
        }
    }
}
