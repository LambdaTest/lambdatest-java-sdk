package io.github.lambdatest.models;

import com.google.gson.annotations.SerializedName;

/**
 * Summary section of the SmartUI results response containing aggregate counts.
 */
public class SmartUIResultsSummary {

    @SerializedName("variants")
    private int variants;

    @SerializedName("screenshots")
    private int screenshots;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("buildId")
    private String buildId;

    @SerializedName("type")
    private String type;

    public SmartUIResultsSummary() {}

    public int getVariants() {
        return variants;
    }

    public void setVariants(int variants) {
        this.variants = variants;
    }

    public int getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(int screenshots) {
        this.screenshots = screenshots;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
