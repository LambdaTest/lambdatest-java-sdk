package io.github.lambdatest.models;

public class UploadSnapshotResponseData {
    private String message;
    private int screenshots;
    private String buildId;
    private String buildUrl;

    // Default constructor
    public UploadSnapshotResponseData() {
    }

    // All-args constructor
    public UploadSnapshotResponseData(String message, int screenshots, String buildId, String buildUrl) {
        this.message = message;
        this.screenshots = screenshots;
        this.buildId = buildId;
        this.buildUrl = buildUrl;
    }

    // Getter methods
    public String getMessage(){
        return message;
    }

    public int getScreenshots(){
        return screenshots;
    }

    public String getBuildUrl(){
        return buildUrl;
    }

    public String getBuildId(){
        return buildId;
    }

    // Setter methods
    public void setMessage(String message) {
        this.message = message;
    }

    public void setScreenshots(int screenshots) {
        this.screenshots = screenshots;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }
}
