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

package org.eclipse.lemminx.customservice.synapse.dependency.tree;

import org.eclipse.lemminx.customservice.synapse.AbstractMediatorVisitor;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.Dependency;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.visitor.EndpointVisitor;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.visitor.MediatorDependencyVisitor;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.visitor.MessageStoreVisitor;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.visitor.SequenceVisitor;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.visitor.TemplateVisitor;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.utils.ConfigFinder;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DependencyVisitorUtils {

    private static final Logger LOGGER = Logger.getLogger(DependencyVisitorUtils.class.getName());

    /**
     * Visits the sequence and returns the dependencies.
     *
     * @param projectPath      The project path.
     * @param sequenceName     The sequence name to visit.
     * @param dependencyLookUp The lookup table for already visited nodes.
     * @return The list of dependencies.
     */
    public static Dependency visitSequence(String projectPath, String sequenceName, DependencyLookUp dependencyLookUp) {

        String inSequencePath = DependencyVisitorUtils.getDependencyPath(sequenceName, "sequences", projectPath);
        if (dependencyLookUp.isVisited(inSequencePath)) {
           return null;
        }
        dependencyLookUp.addToVisitedPaths(inSequencePath);
        if (inSequencePath != null) {
            Dependency dependency = dependencyLookUp.getDependency(inSequencePath);
            if (dependency != null) {
                return dependency;
            }
            SequenceVisitor sequenceVisitor = new SequenceVisitor(projectPath, dependencyLookUp);
            sequenceVisitor.visit(inSequencePath);
            return new Dependency(sequenceName, ArtifactType.SEQUENCE, inSequencePath,
                    sequenceVisitor.getDependencyTree().getDependencyList());
        }
        return null;
    }

    /**
     * Visits the anonymous sequence and returns the dependencies.
     *
     * @param sequence         The anonymous sequence to visit.
     * @param projectPath      The project path.
     * @param dependencyLookUp The lookup table for already visited nodes.
     * @return The list of dependencies.
     */
    public static List<Dependency> visitAnonymousSequence(Sequence sequence, String projectPath,
                                                          DependencyLookUp dependencyLookUp) {

        if (sequence != null) {
            return visitMediators(sequence.getMediatorList(), projectPath, dependencyLookUp);
        }
        return Collections.emptyList();
    }

    /**
     * Visits the mediators in the list and returns the dependencies.
     *
     * @param mediators        The list of mediators to visit.
     * @param dependencyLookUp The lookup table for already visited nodes.
     * @return The list of dependencies.
     */
    public static List<Dependency> visitMediators(List<Mediator> mediators, String projectPath,
                                                  DependencyLookUp dependencyLookUp) {

        MediatorDependencyVisitor visitor = new MediatorDependencyVisitor(projectPath, dependencyLookUp);
        for (Mediator mediator : mediators) {
            visitMediator(mediator, visitor);
        }
        return visitor.getDependencies();
    }

    /**
     * Visits the mediator node.
     *
     * @param node    The mediator node to visit.
     * @param visitor The visitor to visit the mediator.
     */
    public static void visitMediator(Mediator node, MediatorDependencyVisitor visitor) {

        String tag = Utils.sanitizeTag(node.getTag());
        if (Constant.INVALID.equalsIgnoreCase(tag)) {
            return;
        }
        String visitFn = "visit" + tag.substring(0, 1).toUpperCase() + tag.substring(1);
        try {
            Method method = AbstractMediatorVisitor.class.getDeclaredMethod(visitFn, node.getClass());
            method.setAccessible(true);
            method.invoke(visitor, node);
        } catch (NoSuchMethodException e) {
            LOGGER.log(Level.SEVERE, "No visit method found for mediator: " + tag, e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "Error while invoking visit method for mediator: " + tag, e);
        }
    }

    /**
     * Visits the endpoint and returns the dependencies.
     *
     * @param endpoint         The endpoint to visit.
     * @param projectPath      The project path.
     * @param dependencyLookUp The lookup table for already visited nodes.
     * @return The list of dependencies.
     */
    public static Dependency visitEndpoint(NamedEndpoint endpoint, String projectPath,
                                           DependencyLookUp dependencyLookUp) {

        String endpointKey = endpoint.getKey();
        EndpointVisitor endpointVisitor = new EndpointVisitor(projectPath, dependencyLookUp);
        if (endpointKey != null) {
            String endpointPath = DependencyVisitorUtils.getDependencyPath(endpointKey, "endpoints", projectPath);
            if (endpointPath != null) {
                Dependency dependency = dependencyLookUp.getDependency(endpointPath);
                if (dependency != null) {
                    return dependency;
                }
                endpointVisitor.visit(endpointPath);
                return new Dependency(endpointKey, ArtifactType.ENDPOINT, endpointPath,
                        endpointVisitor.getDependencyTree().getDependencyList());
            }
        } else {
            UUID uuid = UUID.randomUUID();
            String anonymousEPName = "AnonymousEndpoint_" + uuid;
            endpointVisitor.visit(endpoint);
            return new Dependency(anonymousEPName, ArtifactType.ENDPOINT, null,
                    endpointVisitor.getDependencyTree().getDependencyList());
        }
        return null;
    }

    /**
     * Visits the endpoint and returns the dependencies.
     *
     * @param endpoint         The endpoint to visit.
     * @param projectPath      The project path.
     * @param dependencyLookUp The lookup table for already visited nodes.
     * @return The list of dependencies.
     */
    public static Dependency visitEndpoint(String endpoint, String projectPath, DependencyLookUp dependencyLookUp) {

        String endpointPath = DependencyVisitorUtils.getDependencyPath(endpoint, "endpoints", projectPath);
        if (endpointPath != null) {
            Dependency dependency = dependencyLookUp.getDependency(endpointPath);
            if (dependency != null) {
                return dependency;
            }
            EndpointVisitor endpointVisitor = new EndpointVisitor(projectPath, dependencyLookUp);
            endpointVisitor.visit(endpointPath);
            return new Dependency(endpoint, ArtifactType.ENDPOINT, endpointPath,
                    endpointVisitor.getDependencyTree().getDependencyList());
        }
        return null;
    }

    /**
     * Visits the template and returns the dependencies.
     *
     * @param template         The template to visit.
     * @param projectPath      The project path.
     * @param dependencyLookUp The lookup table for already visited nodes.
     * @return The list of dependencies.
     */
    public static Dependency visitTemplate(String template, String projectPath, DependencyLookUp dependencyLookUp) {

        String templatePath = DependencyVisitorUtils.getDependencyPath(template, "templates", projectPath);
        if (templatePath != null) {
            Dependency dependency = dependencyLookUp.getDependency(templatePath);
            if (dependency != null) {
                return dependency;
            }
            TemplateVisitor templateVisitor = new TemplateVisitor(projectPath, dependencyLookUp);
            templateVisitor.visit(templatePath);
            return new Dependency(template, ArtifactType.TEMPLATE, templatePath,
                    templateVisitor.getDependencyTree().getDependencyList());
        }
        return null;
    }

    /**
     * Returns the path of the given artifact.
     *
     * @param key         The key of the artifact.
     * @param type        The type of the artifact.
     * @param projectPath The project path.
     * @return The path of the artifact.
     */
    public static String getDependencyPath(String key, String type, String projectPath) {

        try {
            return ConfigFinder.findEsbComponentPath(key, type, projectPath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while finding the path for the artifact: " + key, e);
        }
        return null;
    }

    /**
     * Visits the message store and returns the dependencies.
     *
     * @param messageStore     The message store to visit.
     * @param projectPath      The project path.
     * @param dependencyLookUp The lookup table for already visited nodes.
     * @return The list of dependencies.
     */
    public static Dependency visitMessageStore(String messageStore, String projectPath,
                                               DependencyLookUp dependencyLookUp) {

        String path = getDependencyPath(messageStore, "message-stores", projectPath);
        if (path != null) {
            Dependency dependency = dependencyLookUp.getDependency(path);
            if (dependency != null) {
                return dependency;
            }
            MessageStoreVisitor messageStoreVisitor = new MessageStoreVisitor(projectPath, dependencyLookUp);
            messageStoreVisitor.visit(path);
            return new Dependency(messageStore, ArtifactType.MESSAGE_STORE, path,
                    messageStoreVisitor.getDependencyTree().getDependencyList());
        }
        return null;
    }
}
