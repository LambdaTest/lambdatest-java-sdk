package io.github.lambdatest.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * Response model for SmartUI results containing screenshots grouped by name
 * and an aggregate summary.
 */
public class SmartUIResultsResponse {

    @SerializedName("screenshots")
    private Map<String, List<SmartUIResultScreenshot>> screenshots;

    @SerializedName("summary")
    private SmartUIResultsSummary summary;

    public SmartUIResultsResponse() {}

    public Map<String, List<SmartUIResultScreenshot>> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(Map<String, List<SmartUIResultScreenshot>> screenshots) {
        this.screenshots = screenshots;
    }

    public SmartUIResultsSummary getSummary() {
        return summary;
    }

    public void setSummary(SmartUIResultsSummary summary) {
        this.summary = summary;
    }
}
