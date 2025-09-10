package io.github.lambdatest.models;

public class UploadPDFResponse {
    private String status;
    private String message;
    private String buildURL;
    private String buildId;
    private String projectId;
    private String projectName;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBuildURL() {
        return buildURL;
    }

    public void setBuildURL(String buildURL) {
        this.buildURL = buildURL;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public boolean isSuccessful() {
        return "Success".equalsIgnoreCase(status);
    }
}
