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

package org.eclipse.lemminx.customservice.synapse.debugger.visitor;

import org.eclipse.lemminx.customservice.synapse.AbstractMediatorVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.Breakpoint;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.IDebugInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.breakpoint.BreakpointMediatorVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.stepover.StepOverMediatorVisitor;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VisitorUtils {

    private static final Logger LOGGER = Logger.getLogger(VisitorUtils.class.getName());

    /**
     * Check whether the breakpoint is within the range of the node.
     *
     * @param node       The node to check.
     * @param breakpoint The breakpoint to check.
     * @return True if the breakpoint is within the range of the node, false otherwise.
     */
    public static boolean checkNodeInRange(STNode node, Breakpoint breakpoint) {

        if (node == null) {
            return false;
        }
        int startLine = node.getRange().getStartTagRange().getStart().getLine();
        int startColumn = node.getRange().getStartTagRange().getStart().getCharacter();
        int endLine;
        int endColumn;
        if (node.isSelfClosed()) {
            endLine = node.getRange().getStartTagRange().getEnd().getLine();
            endColumn = node.getRange().getStartTagRange().getEnd().getCharacter();
        } else {
            endLine = node.getRange().getEndTagRange().getEnd().getLine();
            endColumn = node.getRange().getEndTagRange().getEnd().getCharacter();
        }
        if (startLine < breakpoint.getLine() && breakpoint.getLine() < endLine) {
            return true;
        } else if (startLine == breakpoint.getLine() && endLine == breakpoint.getLine()) {
            return breakpoint.getColumn() == null ||
                    (startColumn <= breakpoint.getColumn() && breakpoint.getColumn() < endColumn);
        } else if (startLine == breakpoint.getLine()) {
            return breakpoint.getColumn() == null || startColumn <= breakpoint.getColumn();
        } else if (endLine == breakpoint.getLine()) {
            return breakpoint.getColumn() == null || breakpoint.getColumn() < endColumn;
        } else {
            return false;
        }
    }

    /**
     * Check whether the breakpoint is in the start tag range of the mediator node.
     *
     * @param node       The node(mediator) to check.
     * @param breakpoint The breakpoint to check.
     * @return True if the breakpoint is within the start tag range of the node, false otherwise.
     */
    public static boolean checkValidBreakpoint(STNode node, Breakpoint breakpoint) {

        if (node == null) {
            return false;
        }
        int startLine = node.getRange().getStartTagRange().getStart().getLine(); // Start line of the starting tag
        int startColumn = node.getRange().getStartTagRange().getStart().getCharacter(); // Start column of the
        // starting tag
        int endLine = node.getRange().getStartTagRange().getEnd().getLine(); // End line of the starting tag
        int endColumn = node.getRange().getStartTagRange().getEnd().getCharacter(); // End column of the starting tag
        if (startLine < breakpoint.getLine() && breakpoint.getLine() < endLine) {
            return true;
        } else if (startLine == breakpoint.getLine() || endLine == breakpoint.getLine()) {
            return breakpoint.getColumn() == null ||
                    (startColumn <= breakpoint.getColumn() && breakpoint.getColumn() < endColumn);
        } else {
            return false;
        }
    }

    /**
     * Visit the mediators in the list and get the debug info.
     *
     * @param mediators The list of mediators to visit.
     * @param visitor   The visitor to visit the mediators.
     */
    public static void visitMediators(List<Mediator> mediators, BreakpointMediatorVisitor visitor) {

        visitMediators(mediators, visitor, new HashMap<>());
    }

    /**
     * Visit the mediators in the list and generate debug info for the current breakpoint.
     * If the visitor is done, then the breakpoint will be removed from the list and the debug info will be stored in
     * the map. Then the visitor will continue from the next breakpoint.
     *
     * @param mediators  The list of mediators to visit.
     * @param visitor    The visitor to visit the mediators.
     * @param debugInfos The map to store the debug info of the mediators.
     */
    public static void visitMediators(List<Mediator> mediators, BreakpointMediatorVisitor visitor, HashMap<Breakpoint,
            IDebugInfo> debugInfos) {

        if (mediators != null && mediators.size() > 0) {
            for (int i = 0; i < mediators.size(); i++) {

                visitMediator(mediators.get(i), visitor);
                if (visitor.isDone()) {
                    IDebugInfo debugInfo = null;
                    try {
                        debugInfo = visitor.getDebugInfo().clone();
                    } catch (CloneNotSupportedException e) {
                        LOGGER.log(Level.SEVERE, "Error while cloning debug info", e);
                    }
                    debugInfos.put(visitor.getBreakpoint(), debugInfo);
                    if (visitor.getBreakpoints() != null && visitor.getBreakpoints().size() > 0) {
                        visitor.nextBreakpoint();
                        i--;
                    }
                }
            }
        }
    }

    /**
     * Visit the mediators in the list and get the next step over breakpoints.
     *
     * @param mediators The list of mediators to visit.
     * @param visitor   The visitor to visit the mediators.
     */
    public static void visitMediators(List<Mediator> mediators, StepOverMediatorVisitor visitor) {

        if (mediators != null && mediators.size() > 0) {
            for (int i = 0; i < mediators.size(); i++) {
                visitMediator(mediators.get(i), visitor);
                if (visitor.isDone()) {
                    break;
                }
            }
        }
    }

    /**
     * Visit the mediator node.
     *
     * @param node    The mediator node to visit.
     * @param visitor The visitor to visit the mediator.
     */
    public static void visitMediator(Mediator node, AbstractMediatorVisitor visitor) {

        String tag = node.getTag();
        tag = Utils.sanitizeTag(tag);

        String visitFn;
        visitFn = "visit" + tag.substring(0, 1).toUpperCase() + tag.substring(1);
        try {
            Method method = AbstractMediatorVisitor.class.getDeclaredMethod(visitFn, node.getClass());
            method.setAccessible(true);
            method.invoke(visitor, node);
        } catch (NoSuchMethodException e) {
            LOGGER.log(Level.SEVERE, "No visit method found for mediator: " + tag, e);
        } catch (InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "Error while invoking visit method for mediator: " + tag, e);
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "Error while accessing visit method for mediator: " + tag, e);
        }
    }

    /**
     * Mark the breakpoint as invalid and store the error message in the debug info.
     *
     * @param breakpoint        The breakpoint to mark as invalid.
     * @param error             The error message to store in the debug info.
     * @param debugInfo         The debug info to store the error message.
     * @param breakpointInfoMap The map to store the debug info of the breakpoints.
     * @param breakpoints       The list of breakpoints.
     */
    public static void markAsInvalid(Breakpoint breakpoint, String error, IDebugInfo debugInfo, HashMap<Breakpoint,
            IDebugInfo> breakpointInfoMap, List<Breakpoint> breakpoints) {

        breakpoints.remove(breakpoint);
        debugInfo.setValid(false);
        debugInfo.setError(error);
        breakpointInfoMap.put(breakpoint, debugInfo);
    }
}
