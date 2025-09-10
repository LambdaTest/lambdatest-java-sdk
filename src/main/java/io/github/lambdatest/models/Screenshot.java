package io.github.lambdatest.models;

import com.google.gson.annotations.SerializedName;

public class Screenshot {
    @SerializedName("captured_image_id")
    private String capturedImageId;
    
    @SerializedName("screenshot_name")
    private String screenshotName;
    
    @SerializedName("captured_image")
    private String capturedImage;
    
    @SerializedName("baseline_image")
    private String baselineImage;
    
    @SerializedName("compared_image")
    private String comparedImage;
    
    @SerializedName("browser_name")
    private String browserName;
    
    @SerializedName("browser_version")
    private String browserVersion;
    
    @SerializedName("viewport")
    private String viewport;
    
    @SerializedName("os")
    private String os;
    
    @SerializedName("mismatch_percentage")
    private double mismatchPercentage;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("captured_image_timestamp")
    private String capturedImageTimestamp;
    
    @SerializedName("compared_image_timestamp")
    private String comparedImageTimestamp;
    
    @SerializedName("compared_image_id")
    private String comparedImageId;
    
    @SerializedName("shareable_link")
    private String shareableLink;

    public Screenshot() {}

    public String getCapturedImageId() {
        return capturedImageId;
    }

    public void setCapturedImageId(String capturedImageId) {
        this.capturedImageId = capturedImageId;
    }

    public String getScreenshotName() {
        return screenshotName;
    }

    public void setScreenshotName(String screenshotName) {
        this.screenshotName = screenshotName;
    }

    public String getCapturedImage() {
        return capturedImage;
    }

    public void setCapturedImage(String capturedImage) {
        this.capturedImage = capturedImage;
    }

    public String getBaselineImage() {
        return baselineImage;
    }

    public void setBaselineImage(String baselineImage) {
        this.baselineImage = baselineImage;
    }

    public String getComparedImage() {
        return comparedImage;
    }

    public void setComparedImage(String comparedImage) {
        this.comparedImage = comparedImage;
    }

    public String getBrowserName() {
        return browserName;
    }

    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }

    public String getViewport() {
        return viewport;
    }

    public void setViewport(String viewport) {
        this.viewport = viewport;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public double getMismatchPercentage() {
        return mismatchPercentage;
    }

    public void setMismatchPercentage(double mismatchPercentage) {
        this.mismatchPercentage = mismatchPercentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCapturedImageTimestamp() {
        return capturedImageTimestamp;
    }

    public void setCapturedImageTimestamp(String capturedImageTimestamp) {
        this.capturedImageTimestamp = capturedImageTimestamp;
    }

    public String getComparedImageTimestamp() {
        return comparedImageTimestamp;
    }

    public void setComparedImageTimestamp(String comparedImageTimestamp) {
        this.comparedImageTimestamp = comparedImageTimestamp;
    }

    public String getComparedImageId() {
        return comparedImageId;
    }

    public void setComparedImageId(String comparedImageId) {
        this.comparedImageId = comparedImageId;
    }

    public String getShareableLink() {
        return shareableLink;
    }

    public void setShareableLink(String shareableLink) {
        this.shareableLink = shareableLink;
    }
}
