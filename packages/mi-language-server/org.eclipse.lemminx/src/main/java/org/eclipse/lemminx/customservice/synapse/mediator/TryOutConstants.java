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

package org.eclipse.lemminx.customservice.synapse.mediator;

import java.nio.file.Path;
import java.util.List;

public class TryOutConstants {

    //Default Ports
    public static final int DEFAULT_DEBUGGER_COMMAND_PORT = 9005;
    public static final int DEFAULT_DEBUGGER_EVENT_PORT = 9006;
    public static final int DEFAULT_SERVER_PORT = 8290;
    public static final int DEFAULT_SERVER_INBOUND_PORT = 9201;

    // One subfolder per language server process (PID), so windows don't overwrite each other.
    public static final Path TEMP_FOLDER_ROOT = Path.of(System.getProperty("user.home"), ".wso2-mi", "tryout");
    public static final Path TEMP_FOLDER_PATH = TEMP_FOLDER_ROOT.resolve(String.valueOf(ProcessHandle.current().pid()));
    public static final Path CAPP_BUILD_TEMP_ROOT =
            Path.of(System.getProperty("user.home"), ".wso2-mi", "tryout_capp_build_temp");
    public static final Path CAPP_CACHE_LOCATION =
            Path.of(System.getProperty("user.home") + "/.wso2-mi/tryout_capp_cache");
    public static final Path API_RELATIVE_PATH = Path.of("src", "main", "wso2mi", "artifacts", "apis");
    public static final String SYNAPSE_PROPERTIES = "synapse-properties";
    public static final String AXIS2_PROPERTIES = "axis2-properties";
    public static final String AXIS2_CLIENT_PROPERTIES = "axis2Client-properties";
    public static final String AXIS2_TRANSPORT_PROPERTIES = "axis2Transport-properties";
    public static final String AXIS2_OPERATION_PROPERTIES = "axis2Operation-properties";
    public static final String ENVELOPE = "Envelope";
    public static final String API_KEY = "api-key";
    public static final String HTTP_PREFIX = "http://";
    public static final String SLASH = "/";
    public static final String POST = "POST";
    public static final String RESUME_COMMAND = "{\"command\":\"resume\"}";
    public static final String EVENT = "event";
    public static final String BREAKPOINT = "breakpoint";
    public static final String LOCALHOST = "localhost";
    public static final Path CARBON_XML_J2_PATH = Path.of("repository", "resources", "conf", "templates",
            "conf", "carbon.xml.j2");
    public static final Path CARBON_XML_PATH = Path.of("conf", "carbon.xml");
    public static final Path MI_REPOSITORY_PATH = Path.of("repository", "deployment", "server",
            "synapse-configs", "default");
    public static final Path PROJECT_ARTIFACT_PATH = Path.of("src", "main", "wso2mi", "artifacts");
    public static final Path PROJECT_CONNECTOR_PATH = Path.of("src", "main", "wso2mi", "resources", "connectors");
    public static final Path MI_CONNECTOR_PATH = Path.of("repository", "deployment", "server", "synapse-libs");
    public static final Path PROJECT_REGSTRY_PATH = Path.of("src", "main", "wso2mi", "resources", "registry");
    public static final Path DEPLOYMENT_TOML_PATH = Path.of("conf", "deployment.toml");
    public static final String GOV = "gov";
    public static final String CONF = "conf";
    public static final Path MI_GOV_PATH = Path.of("registry", "governance");
    public static final Path MI_CONF_PATH = Path.of("registry", "config");
    public static final String CORRELATION_ID = "correlation_id";
    public static final String MESSAGE_VARIABLES = "message-variables";
    public static final String SERVER_ALREADY_IN_USE_ERROR =
            "The server is already in use or running. Please stop it and try again.";
    // The Try-Out panel matches this text to show "Remove and Continue".
    public static final String CONFLICTING_CAPPS_ERROR = "Other Carbon Applications deployed in the try-out MI " +
            "server define artifacts this try-out also deploys: %s. Remove them to continue (they are moved to %s, " +
            "not deleted).";
    public static final Path REMOVED_CAPPS_FOLDER =
            Path.of(System.getProperty("user.home"), ".wso2-mi", "tryout_removed_capps");
    public static final String ARTIFACT_DEPLOYMENT_FAILED = "The %s could not be deployed: %s";
    public static final String ARTIFACT_NOT_DEPLOYED = "The %s was not deployed within %d seconds.";
    public static final String SKIPPED_ARTIFACTS_WARNING = "Some project artifacts could not be deployed and were " +
            "skipped: %s. The try-out still runs, but fails if the flow uses them.";
    // The Try-Out panel matches this text to show "Load Input Again".
    public static final String SESSION_ENDED_ERROR = "This try-out session is no longer active (the try-out was " +
            "reset or another Try-Out panel took over). Load the mediator input again to continue.";
    // Starts like SERVER_STOPPED_ERROR, so the Try-Out panel offers "Load Input Again".
    public static final String SERVER_STOPPED_WHILE_WAITING_ERROR = "The try-out server was stopped while this " +
            "request was waiting (for example, to start debugging). Load the input again to continue.";
    public static final String SERVER_STOPPED_ERROR = "The try-out server was stopped, possibly from another " +
            "window. This try-out has ended; reopen the Try-Out to start again.";
    // The Try-Out panel matches this text to show "Retry".
    public static final String TRYOUT_IN_PROGRESS_ERROR = "Another try-out is in progress for project '%s'. Finish " +
            "it or close its Try-Out panel, then retry.";
    public static final String NO_PROJECT_ERROR = "This request does not identify an open MI project, so no try-out " +
            "server could be started. Reopen the file from its project folder and try again.";
    // Record of the running try-out server, shared by all windows. Kept outside TEMP_FOLDER_PATH, which is wiped.
    public static final Path TRYOUT_SERVER_RECORD_FILE =
            Path.of(System.getProperty("user.home"), ".wso2-mi", "tryout-server.json");
    public static final String TRYOUT_NOT_ACTIVATED_ERROR = "Try-Out feature not activated.";
    public static final String SERVER_NOT_READY_ERROR = "The try-out server did not start in time, so the project " +
            "dependencies could not be deployed. Try again.";
    public static final String TRYOUT_FAILURE_MESSAGE =
            "An error occurred while handling the tryout. Please try again.";
    public static final String AXIS2 = "axis2";
    public static final String VARIABLE = "variable";
    public static final String DEFAULT = "default";
    public static final String AXIS2_CLIENT = "axis2-client";
    public static final String AXIS2_TRANSPORT = "axis2-transport";
    public static final String AXIS2_OPERATION = "axis2-operation";
    public static final String TRANSPORT = "transport";
    public static final String COMMAND = "command";
    public static final String SET = "set";
    public static final String COMMAND_ARGUMENT = "command-argument";
    public static final String PROPERTY = "property";
    public static final String CONTEXT = "context";
    public static final String VARIABLE_NAME = "variable-name";
    public static final String PROPERTY_NAME = "property-name";
    public static final String VARIABLE_VALUE = "variable-value";
    public static final String PROPERTY_VALUE = "property-value";
    public static final List<String> LAST_MEDIATOR_LIST = List.of("send", "respond", "drop", "loopback");
    public static final String SUCCESSFUL = "successful";
    public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
    public static final String CLEAR = "clear";
    public static final String GET = "get";
    public static final String VARIABLES = "variables";
    public static final String PROPERTIES = "properties";
    public static final String IMPORTS = "imports";
    public static final Path MI_DEPLOYMENT_PATH = Path.of("repository", "deployment",
            "server", "carbonapps");
    public static final String INVALID_ARTIFACT_ERROR = "Unable to try out the mediator due to an invalid " +
            "configuration in the API. Please review the API and try again.";
    public static final String URI_TEMPLATE = "uri-template";
    public static final String BUILD_FAILURE_MESSAGE = "Unable to try-out the mediator." +
            " Please build the project manually and resolve any issues before trying again.";
    public static final String URI_PARAM_PREFIX = "uri.var.";
    public static final String QUERY_PARAM_PREFIX = "query.param.";
    public static final String MISSING_PATH_PARAMS_ERROR = "The selected request doesn't provide a value for the " +
            "path parameter(s) %s of this resource. Add them to the request (select the start node to edit requests) " +
            "and try again.";
    public static final String MEDIATION_ERROR = "The mediation failed while running: %s";
    public static final String OUTPUT_TIMEOUT_ERROR = "The mediator did not finish within %d seconds, so its output " +
            "could not be captured. It may be waiting on a slow or unreachable backend, or the flow may have ended " +
            "before reaching the next mediator. Running it again with the same request will likely give the same " +
            "result; check the Try-Out server log for details.";
    public static final String PAYLOAD_NOT_HIT_ERROR =
            "This mediator cannot be tried out with the current request payload. Please select a different payload.";
    public static final CharSequence BREAKPOINT_ALREADY_REGISTERED = "already breakpoint enabled at mediator position";
    public static final Path PROJECT_RESOURCES_RELATIVE_PATH = Path.of("src", "main", "wso2mi", "resources");
    public static final String POST_CLEANUP = "POST_CLEANUP";
    public static final String IS_CONNECTOR_TEST = "IS_CONNECTOR_TEST";
    // PID file MI writes on startup; used to kill whatever server holds the try-out ports.
    public static final Path WSO2CARBON_PID_FILE = Path.of("wso2carbon.pid");

    private TryOutConstants() {

    }
}
