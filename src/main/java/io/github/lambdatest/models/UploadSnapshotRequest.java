package io.github.lambdatest.models;

import java.io.File;

public class UploadSnapshotRequest {
    private File screenshot;
    private String deviceName;
    private String os;  // os field added
    private String resolution;
    private String projectToken;
    private String buildId;
    private String buildName;
    private String screenshotName;

    // Default constructor
    public UploadSnapshotRequest() {
    }

    // All Args constructor
    public UploadSnapshotRequest(File screenshot, String deviceName, String os, String resolution,
                                 String projectToken, String buildId, String buildName,
                                 String screenshotName) {
        this.screenshot = screenshot;
        this.deviceName = deviceName;
        this.os = os;  // initialize os
        this.resolution = resolution;
        this.projectToken = projectToken;
        this.buildId = buildId;
        this.buildName = buildName;
        this.screenshotName = screenshotName;
    }

    // Getters and setters
    public File getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(File screenshot) {
        this.screenshot = screenshot;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getProjectToken() {
        return projectToken;
    }

    public void setProjectToken(String projectToken) {
        this.projectToken = projectToken;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getScreenshotName() {
        return screenshotName;
    }

    public void setScreenshotName(String screenshotName) {
        this.screenshotName = screenshotName;
    }
}
