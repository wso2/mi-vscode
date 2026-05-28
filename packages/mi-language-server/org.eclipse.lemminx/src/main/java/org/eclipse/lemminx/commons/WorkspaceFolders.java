package org.eclipse.lemminx.commons;

import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WorkspaceFolders {

    private static final WorkspaceFolders INSTANCE = new WorkspaceFolders();

    private Map<String, WorkspaceFolder> xmlWorkspaceFoldersNew;

    private WorkspaceFolders() {
        xmlWorkspaceFoldersNew = new HashMap<>();
    }

    public static WorkspaceFolders getInstance() {
        return INSTANCE;
    }

    public void addWorkspaceFolder(WorkspaceFolder workspaceFolder) {
        this.xmlWorkspaceFoldersNew.put(workspaceFolder.getUri(), workspaceFolder);
    }

    public void removeWorkspaceFolder(WorkspaceFolder workspaceFolder) {
        this.xmlWorkspaceFoldersNew.remove(workspaceFolder.getUri());
    }

    public Collection<WorkspaceFolder> getWorkspaceFolders() {
        return this.xmlWorkspaceFoldersNew.values();
    }

    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
        WorkspaceFoldersChangeEvent changeEvent = params.getEvent();

        if (changeEvent.getAdded().size() > 0) {
            for (int i = 0; i < changeEvent.getAdded().size(); i++) {
                addWorkspaceFolder(changeEvent.getAdded().get(i));
            }
        } else if (changeEvent.getRemoved().size() > 0) {
            for (int i = 0; i < changeEvent.getRemoved().size(); i++) {
                removeWorkspaceFolder(changeEvent.getRemoved().get(i));
            }
        }
    }
}
