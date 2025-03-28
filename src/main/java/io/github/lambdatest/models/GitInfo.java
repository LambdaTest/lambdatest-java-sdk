package io.github.lambdatest.models;

public class GitInfo {
    private final String branch;
    private final String commitId;
    private final String commitMessage;
    private final String commitAuthor;
    private final String githubURL;
    private final String baselineBranch;

    public GitInfo(String branch, String commitId, String commitMessage, String commitAuthor, String githubURL, String baselineBranch) {
        this.branch = branch;
        this.commitId = commitId;
        this.commitMessage = commitMessage;
        this.commitAuthor = commitAuthor;
        this.githubURL = githubURL;
        this.baselineBranch = baselineBranch;
    }

    public String getBranch() { return branch; }
    public String getCommitId() { return commitId; }
    public String getCommitMessage() { return commitMessage; }
    public String getCommitAuthor() { return commitAuthor; }
    public String getGithubURL() { return githubURL; }
    public String getBaselineBranch() { return baselineBranch; }

    @Override
    public String toString() {
        return "GitInfo{" +
                "branch='" + branch + '\'' +
                ", commitId='" + commitId + '\'' +
                ", commitMessage='" + commitMessage + '\'' +
                ", commitAuthor='" + commitAuthor + '\'' +
                ", githubURL='" + githubURL + '\'' +
                ", baselineBranch='" + baselineBranch + '\'' +
                '}';
    }
}

