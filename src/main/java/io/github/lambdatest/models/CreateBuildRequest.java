package io.github.lambdatest.models;

public class CreateBuildRequest {
    private String projectToken;
    private BuildConfig buildConfig;
    //private Boolean isStartExec;
    //private Git git;

    public CreateBuildRequest() {}

    public CreateBuildRequest(String projectToken, BuildConfig buildConfig, Boolean isStartExec) {
        this.projectToken = projectToken;
        this.buildConfig = buildConfig;
        //this.isStartExec = isStartExec;
        //this.git = git;
    }

    public String getProjectToken() {
        return projectToken;
    }

    public void setProjectToken(String projectToken) {
        this.projectToken = projectToken;
    }

    public BuildConfig getBuildConfig() {
        return buildConfig;
    }

    public void setBuildConfig(BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }
}