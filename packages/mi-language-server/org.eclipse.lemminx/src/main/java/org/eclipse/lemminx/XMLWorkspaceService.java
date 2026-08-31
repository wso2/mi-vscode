/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.WorkspaceFolders;
import org.eclipse.lemminx.customservice.synapse.ProjectContext;
import org.eclipse.lemminx.customservice.synapse.dataService.DynamicClassLoader;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.extensions.synapse.SynapseDiagnosticsParticipant;
import org.eclipse.lemminx.services.extensions.commands.IXMLCommandService;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * XML workspace service.
 *
 */
public class XMLWorkspaceService implements WorkspaceService, IXMLCommandService {

	private static final Logger log = Logger.getLogger(XMLWorkspaceService.class.getName());

	private final XMLLanguageServer xmlLanguageServer;
	private final WorkspaceFolders workspaceFolders;

	private final Map<String, IDelegateCommandHandler> commands;

	public XMLWorkspaceService(XMLLanguageServer xmlLanguageServer) {
		this.xmlLanguageServer = xmlLanguageServer;
		this.commands = new HashMap<>();
		this.workspaceFolders = WorkspaceFolders.getInstance();
	}

	@Override
	public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
		synchronized (commands) {
			IDelegateCommandHandler handler = commands.get(params.getCommand());
			if (handler == null) {
				throw new ResponseErrorException(new ResponseError(ResponseErrorCode.InternalError,
						"No command handler for the command: " + params.getCommand(), null));
			}
			return CompletableFutures.computeAsync(cancelChecker -> {
				try {
					return handler.executeCommand(params, xmlLanguageServer.getSharedSettings(), cancelChecker);
				} catch (Exception e) {
					if (e instanceof ResponseErrorException) {
						throw (ResponseErrorException) e;
					} else if (e instanceof CancellationException) {
						throw (CancellationException) e;
					}
					throw new ResponseErrorException(
							new ResponseError(ResponseErrorCode.UnknownErrorCode, e.getMessage(), e));
				}
			});
		}
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		xmlLanguageServer.updateSettings(params.getSettings());
		xmlLanguageServer.getCapabilityManager().syncDynamicCapabilitiesWithPreferences();
	}

	@Override
	public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
		xmlLanguageServer.getXMLLanguageService().getWorkspaceServiceParticipants()
				.forEach(participant -> participant.didChangeWorkspaceFolders(params));

		boolean hasSchemaChanges = false;
		if (params.getEvent().getRemoved() != null) {
			for (WorkspaceFolder folder : params.getEvent().getRemoved()) {
				if (log.isLoggable(Level.FINE)) {
					log.fine("Removing workspace folder: " + folder.getUri());
				}
				xmlLanguageServer.removeWorkspaceSchema(folder.getUri());
				xmlLanguageServer.removeWorkspaceProjectContext(folder.getUri());
				DynamicClassLoader.removeProject(folder.getUri());
				hasSchemaChanges = true;
			}
		}
		if (params.getEvent().getAdded() != null) {
			for (WorkspaceFolder folder : params.getEvent().getAdded()) {
				try {
					// copyXSDFiles() reads the project's pom.xml to pick the MI-version schema set, so it
					// needs the filesystem path — handing it the file:// URI makes the pom lookup fail and
					// silently falls back to DEFAULT_MI_VERSION's XSDs (the initialize path passes a path too).
					String projectPath = Utils.getAbsolutePath(folder.getUri());
					Path schemaDir = Utils.copyXSDFiles(projectPath);
					xmlLanguageServer.addWorkspaceSchema(folder.getUri(), schemaDir);
					hasSchemaChanges = true;
					xmlLanguageServer.addWorkspaceProjectContext(folder.getUri(), projectPath, schemaDir);
				} catch (Exception e) {
					log.log(Level.SEVERE, "Failed to copy XSD files for workspace folder: " + folder.getUri() + ". Error: " + e.getMessage());
				}
			}
		}
		if (hasSchemaChanges) {
			xmlLanguageServer.triggerSettingsRefresh();
		}
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		XMLTextDocumentService xmlTextDocumentService = (XMLTextDocumentService) xmlLanguageServer
				.getTextDocumentService();
		List<FileEvent> changes = params.getChanges();
		for (FileEvent change : changes) {
			if ((change.getUri().contains(Constant.INBOUND_ENDPOINTS)
					|| change.getUri().contains(Constant.INBOUND_CONNECTORS_DIR)) && change.getUri().contains(".zip")) {
				ProjectContext context = xmlLanguageServer
						.getWorkspaceManager().getProjectForDocument(change.getUri());
				if (context != null) {
					context.updateInboundConnectors();
				} else {
					// TODO(unrouted-request): a watched .zip that belongs to no registered MI project.
					// This previously reloaded the *default* project's inbound connectors, refreshing a
					// project with nothing to do with the changed file. Ignoring it is correct for the
					// known cause (a zip in a non-MI workspace folder); if a zip inside a real MI
					// project ever lands here, that project failed to register — which this log surfaces.
					log.warning("Watched inbound connector zip belongs to no registered project, ignoring: "
							+ change.getUri());
				}
			} else if (change.getUri().contains(Constant.CONNECTORS) && change.getUri().contains(".zip")) {
				ProjectContext context = xmlLanguageServer
						.getWorkspaceManager().getProjectForDocument(change.getUri());
				if (context != null) {
					context.updateConnectors();
				} else {
					// TODO(unrouted-request): see the inbound branch above — same reasoning.
					log.warning("Watched connector zip belongs to no registered project, ignoring: "
							+ change.getUri());
				}
			} else {
				// LSP URIs use '/', but normalize defensively so a backslash path also matches on Windows.
				if (change.getUri().replace('\\', '/').contains("src/main/wso2mi")) {
					// An artifact/resource file changed on disk — drop the cached cross-file index so
					// the next diagnostics run rebuilds it (otherwise a just-written sibling stays
					// "unresolved" for up to the cache TTL).
					SynapseDiagnosticsParticipant.invalidateArtifactIndexCache();
				}
				if (!xmlTextDocumentService.documentIsOpen(change.getUri())) {
					xmlTextDocumentService.doSave(change.getUri());
				}
			}
		}
	}

	@Override
	public void registerCommand(String commandId, IDelegateCommandHandler handler) {
		synchronized (commands) {
			if (commands.containsKey(commandId)) {
				throw new IllegalArgumentException("Command with id '" + commandId + "' is already registered");
			}
			commands.put(commandId, handler);
		}
	}

	@Override
	public void unregisterCommand(String commandId) {
		synchronized (commands) {
			commands.remove(commandId);
		}
	}

	@Override
	public CompletableFuture<Object> executeClientCommand(ExecuteCommandParams command) {
		return xmlLanguageServer.getLanguageClient().executeClientCommand(command);
	}

	@Override
	public void endCommandsRegistration() {
		if (!commands.isEmpty()) {
			xmlLanguageServer.getCapabilityManager().registerExecuteCommand(new ArrayList<>(commands.keySet()));
		}
	}
}
