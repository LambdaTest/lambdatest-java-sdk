package io.github.lambdatest.models;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a single screenshot variant in the SmartUI results response.
 */
public class SmartUIResultScreenshot {

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

    @SerializedName("viewport")
    private String viewport;

    @SerializedName("mismatch_percentage")
    private double mismatchPercentage;

    @SerializedName("status")
    private String status;

    @SerializedName("approved_by")
    private String approvedBy;

    public SmartUIResultScreenshot() {}

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

    public String getViewport() {
        return viewport;
    }

    public void setViewport(String viewport) {
        this.viewport = viewport;
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

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
}
