package io.github.lambdatest.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BuildScreenshotsResponse {
    @SerializedName("screenshots")
    private List<Screenshot> screenshots;
    
    @SerializedName("build")
    private BuildInfo build;
    
    @SerializedName("project")
    private ProjectInfo project;

    public BuildScreenshotsResponse() {}

    public List<Screenshot> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(List<Screenshot> screenshots) {
        this.screenshots = screenshots;
    }

    public BuildInfo getBuild() {
        return build;
    }

    public void setBuild(BuildInfo build) {
        this.build = build;
    }

    public ProjectInfo getProject() {
        return project;
    }

    public void setProject(ProjectInfo project) {
        this.project = project;
    }
}
