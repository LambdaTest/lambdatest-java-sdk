package io.github.lambdatest.models;

import com.google.gson.annotations.SerializedName;

public class ProjectInfo {
    @SerializedName("project_id")
    private String projectId;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("project_type")
    private String projectType;
    
    @SerializedName("projectCategory")
    private String projectCategory;
    
    @SerializedName("platform")
    private String platform;

    public ProjectInfo() {}

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getProjectCategory() {
        return projectCategory;
    }

    public void setProjectCategory(String projectCategory) {
        this.projectCategory = projectCategory;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
