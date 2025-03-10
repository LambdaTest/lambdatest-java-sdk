package io.github.lambdatest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class CreateBuildRequest {
    private String buildName;
    private Config config;
    private GitInfo git;

    public CreateBuildRequest() {}

    public CreateBuildRequest(Config config, String buildName, GitInfo git) {
        this.config = config;
        this.buildName = buildName;
        this.git = git;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public Config getBuildConfig() {
        return config;
    }

    public void setBuildConfig(Config config) {
        this.config = config;
    }

    public GitInfo getGit() {
        return git;
    }

    public void setGit(GitInfo git) {
        this.git = git;
    }
}