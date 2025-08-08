package io.github.lambdatest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class UploadSnapshotRequest {
    private String browserName;
    private String os;
    private String viewport;
    @JsonIgnore
    private String projectToken;
    private String buildId;
    private String buildName;
    private String screenshotName;
    private String screenshotHash;
    private String deviceName;
    private String cropFooter;
    private String cropStatusBar;
    private  String fullPage;
    private String isLastChunk;
    private Integer chunkCount;
    private String uploadChunk;
    private String navigationBarHeight;
    private String statusBarHeight;
    private String ignoreBoxes;

    // Default constructor
    public UploadSnapshotRequest() {
    }

    // All Args constructor
    public UploadSnapshotRequest(String screenshot, String browserName, String os, String viewport,
                                 String projectToken, String buildId, String buildName,
                                 String screenshotName, String screenshotHash ,String deviceName,String fullPage, String  cropFooter,
                                 String cropStatusBar, String isLastChunk, Integer chunkCount, String uploadChunk,
                                 String navigationBarHeight, String statusBarHeight, String ignoreBoxes) {
        this.browserName = browserName;
        this.os = os;
        this.viewport = viewport;
        this.projectToken = projectToken;
        this.buildId = buildId;
        this.buildName = buildName;
        this.screenshotName = screenshotName;
        this.screenshotHash = screenshotHash;
        this.deviceName = deviceName;
        this.cropFooter = cropFooter;
        this.cropStatusBar = cropStatusBar;
        this.fullPage = fullPage;
        this.isLastChunk = isLastChunk;
        this.chunkCount = chunkCount;
        this.uploadChunk = uploadChunk;
        this.navigationBarHeight = navigationBarHeight;
        this.statusBarHeight = statusBarHeight;
        this.ignoreBoxes = ignoreBoxes;
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

    public String getCropFooter() { return cropFooter; }

    public void setCropFooter(String cropFooter) {
        this.cropFooter = cropFooter;
    }

    public String getCropStatusBar() { return cropStatusBar; }

    public String getFullPage() { return fullPage; }

    public void setFullPage(String fullPage) {
        this.fullPage = fullPage;
    }

    public String getIsLastChunk() { return isLastChunk; }

    public void setIsLastChunk(String isLastChunk) {
        this.isLastChunk = isLastChunk;
    }

    public String getUploadChunk() { return uploadChunk; }

    public void setUploadChunk(String uploadChunk) {
        this.uploadChunk = uploadChunk;
    }

    public void setCropStatusBar(String cropStatusBar) {
        this.cropStatusBar = cropStatusBar;
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

    public String getScreenshotHash() {
        return screenshotHash;
    }

    public void setScreenshotHash(String screenshotHash) {
        this.screenshotHash = screenshotHash;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public String getNavigationBarHeight() {
        return navigationBarHeight;
    }

    public void setNavigationBarHeight(String navigationBarHeight) {
        this.navigationBarHeight = navigationBarHeight;
    }

    public String getStatusBarHeight() {
        return statusBarHeight;
    }

    public void setStatusBarHeight(String statusBarHeight) {
        this.statusBarHeight = statusBarHeight;
    }

    public String getIgnoreBoxes() {
        return ignoreBoxes;
    }

    public void setIgnoreBoxes(String ignoreBoxes) {
        this.ignoreBoxes = ignoreBoxes;
    }
}
