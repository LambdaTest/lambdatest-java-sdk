package io.github.lambdatest.models;

public class CreateBuildRequest {
    private String buildName;
    private String projectToken;
    private BuildConfig buildConfig;
    //private Boolean isStartExec;
    //private Git git;

    public CreateBuildRequest() {}

    public CreateBuildRequest(String projectToken, BuildConfig buildConfig, Boolean isStartExec, String buildName) {
        this.projectToken = projectToken;
        this.buildConfig = buildConfig;
        this.buildName = buildName;
        //this.isStartExec = isStartExec;
        //this.git = git;
    }

    public String getProjectToken() {
        return projectToken;
    }
    public String getBuildName() { return  buildName; }

    public void setProjectToken(String projectToken) {
        this.projectToken = projectToken;
    }
    public  void setBuildName(String buildName) { this.buildName = buildName; }

    public BuildConfig getBuildConfig() {
        return buildConfig;
    }
    public void setBuildConfig(BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }
}