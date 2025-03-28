package io.github.lambdatest.models;


import com.google.gson.annotations.SerializedName;

public class BuildData {
    @SerializedName("buildId")
    private String buildId;

    @SerializedName("buildURL")
    private String buildUrl;

    @SerializedName("baseline")
    private Boolean baseline;

    @SerializedName("buildName")
    private String name;

    // Getters & Setters
    public String getBuildId() { return buildId; }
    public String getBuildUrl() { return buildUrl; }
    public Boolean getBaseline() { return baseline; }
    public String getName() { return name; }

    public void setBuildId(String buildId) { this.buildId = buildId; }
    public void setBuildUrl(String buildUrl) { this.buildUrl = buildUrl; }
    public void setBaseline(Boolean baseline) { this.baseline = baseline; }
    public void setName(String buildName) { this.name = buildName; }
}