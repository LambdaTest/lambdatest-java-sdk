package io.github.lambdatest.models;

import com.google.gson.annotations.SerializedName;

public class BuildInfo {
    @SerializedName("build_id")
    private String buildId;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("baseline")
    private boolean baseline;
    
    @SerializedName("build_type")
    private String buildType;
    
    @SerializedName("build_status")
    private String buildStatus;
    
    @SerializedName("commitId")
    private String commitId;
    
    @SerializedName("branch")
    private String branch;
    
    @SerializedName("commitAuthor")
    private String commitAuthor;
    
    @SerializedName("commitMessage")
    private String commitMessage;
    
    @SerializedName("comparisonStrategy")
    private String comparisonStrategy;

    public BuildInfo() {}

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isBaseline() {
        return baseline;
    }

    public void setBaseline(boolean baseline) {
        this.baseline = baseline;
    }

    public String getBuildType() {
        return buildType;
    }

    public void setBuildType(String buildType) {
        this.buildType = buildType;
    }

    public String getBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(String buildStatus) {
        this.buildStatus = buildStatus;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCommitAuthor() {
        return commitAuthor;
    }

    public void setCommitAuthor(String commitAuthor) {
        this.commitAuthor = commitAuthor;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getComparisonStrategy() {
        return comparisonStrategy;
    }

    public void setComparisonStrategy(String comparisonStrategy) {
        this.comparisonStrategy = comparisonStrategy;
    }
}
