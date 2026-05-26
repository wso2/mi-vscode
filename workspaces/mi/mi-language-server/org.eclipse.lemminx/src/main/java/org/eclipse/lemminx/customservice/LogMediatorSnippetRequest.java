package org.eclipse.lemminx.customservice;

public class LogMediatorSnippetRequest extends SnippetCompletionRequest {

    String logLevel;
    String logCategory;
    String logSeparator;
    String description;
    Object properties;

    public String getLogLevel() {

        return logLevel;
    }

    public void setLogLevel(String logLevel) {

        this.logLevel = logLevel;
    }

    public String getLogCategory() {

        return logCategory;
    }

    public void setLogCategory(String logCategory) {

        this.logCategory = logCategory;
    }

    public String getLogSeparator() {

        return logSeparator;
    }

    public void setLogSeparator(String logSeparator) {

        this.logSeparator = logSeparator;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public Object getProperties() {

        return properties;
    }

    public void setProperties(Object properties) {

        this.properties = properties;
    }

    public SnippetCompletionResponse getLogMediator() {
        return new SnippetCompletionResponse(getLogMediatorSnippet());
    }

    public void setLogMediator(String logMediator) {

        this.logMediator = logMediator;
    }

    String logMediator = "<log level=\"$logLevel\" description=\"$description\">\n\t$properties\n</log>";

    public String getLogMediatorSnippet() {
        return logMediator.replace("$logLevel", getLogLevel()).replace("$description", getDescription());
    }

}
