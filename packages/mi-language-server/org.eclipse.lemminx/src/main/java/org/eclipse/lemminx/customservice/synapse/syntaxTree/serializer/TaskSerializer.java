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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.task.Task;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.task.TaskTrigger;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

public class TaskSerializer {

    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    public static String serializeTask(Task task) {

        OMElement taskElt = fac.createOMElement("task", Constant.SYNAPSE_OMNAMESPACE);

        serializeAttributes(task, taskElt);
        serializeChildren(task, taskElt);

        return taskElt.toString();
    }

    private static void serializeAttributes(Task task, OMElement taskElt) {

        if (task.getClazz() != null) {
            taskElt.addAttribute("class", task.getClazz(), null);
        }
        if (task.getName() != null) {
            taskElt.addAttribute("name", task.getName(), null);
        }
        if (task.getGroup() != null) {
            taskElt.addAttribute("group", task.getGroup(), null);
        }
        if (task.getPinnedServers() != null) {
            taskElt.addAttribute("pinnedServers", task.getPinnedServers(), null);
        }
    }

    private static void serializeChildren(Task task, OMElement taskElt) {

        if (task.getTrigger() != null) {
            serializeTrigger(task.getTrigger(), taskElt);
        }
        if (task.getProperty() != null) {
            SerializerUtils.serializeMediatorProperties(taskElt, task.getProperty());
        }
    }

    private static void serializeTrigger(TaskTrigger trigger, OMElement taskElt) {

        OMElement triggerElt = fac.createOMElement("trigger", Constant.SYNAPSE_OMNAMESPACE);

        if (trigger.getCron() != null) {
            triggerElt.addAttribute("cron", trigger.getCron(), null);
        } else {
            if (trigger.getOnce() || ("1".equals(trigger.getInterval()) && "1".equals(trigger.getCount()))) {
                triggerElt.addAttribute("once", "true", null);
            } else {
                if (trigger.getInterval() != null) {
                    triggerElt.addAttribute("interval", trigger.getInterval(), null);
                }
                if (trigger.getCount() != null) {
                    triggerElt.addAttribute("count", trigger.getCount(), null);
                }
            }
        }
        taskElt.addChild(triggerElt);
    }
}
