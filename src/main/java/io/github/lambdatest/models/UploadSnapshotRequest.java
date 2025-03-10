package io.github.lambdatest.models;

import java.util.Objects;

public class UploadSnapshotRequest {
    private String browserName;
    private String os;
    private String viewport;
    private String projectToken;
    private String buildId;
    private String buildName;
    private String screenshotName;
    private String deviceName;

    // Default constructor
    public UploadSnapshotRequest() {
    }

    // All Args constructor
    public UploadSnapshotRequest(String screenshot, String browserName, String os, String viewport,
                                 String projectToken, String buildId, String buildName,
                                 String screenshotName, String deviceName) {
        this.browserName = browserName;
        this.os = os;
        this.viewport = viewport;
        this.projectToken = projectToken;
        this.buildId = buildId;
        this.buildName = buildName;
        this.screenshotName = screenshotName;
        this.deviceName = deviceName;
    }

    // Getters and setters
    public String getBrowserName() {
        return browserName;
    }

    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = Objects.requireNonNull(os, "");
    }

    public String getViewport() {
        return viewport;
    }

    public void setViewport(String viewport) {
        this.viewport = viewport;
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

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
