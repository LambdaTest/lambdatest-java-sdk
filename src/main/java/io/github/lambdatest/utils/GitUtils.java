package io.github.lambdatest.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lambdatest.models.GitInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GitUtils {

    public static GitInfo getGitInfo(Map<String, String> envVars) {
        //Check if git repo, exit graciously if not
        boolean isGit = isGitRepo();
        if(!isGit)
            throw new IllegalStateException("Git Repo is needed to run this test");
        String gitInfoFilePath = envVars.get("SMARTUI_GIT_INFO_FILEPATH");

        // If Git info file exists, read from it
        if (gitInfoFilePath != null) {
            return readGitInfoFromFile(gitInfoFilePath, envVars);
        }
        // Otherwise, fetch Git info from Git commands
        else{
        return fetchGitInfoFromCommands(envVars);
        }
    }
    private static boolean isGitRepo() {
        try {
            Process process = Runtime.getRuntime().exec("git status");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while(reader.readLine() != null) {}
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static GitInfo readGitInfoFromFile(String filePath, Map<String, String> envVars) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            File file = new File(filePath);
            Map<String, Object> gitInfo = objectMapper.readValue(file, Map.class);

            return new GitInfo(
                    envVars.getOrDefault("CURRENT_BRANCH", (String) gitInfo.get("branch")),
                    shortenCommitId((String) gitInfo.get("commit_id")),
                    (String) gitInfo.get("commit_body"),
                    (String) gitInfo.get("commit_author"),
                    getGitHubURL(envVars, (String) gitInfo.get("commit_id")),
                    envVars.getOrDefault("BASELINE_BRANCH", "")
            );
        } catch (IOException e) {
            throw new RuntimeException("Error reading Git info file: " + e.getMessage(), e);
        }
    }

    private static GitInfo fetchGitInfoFromCommands(Map<String, String> envVars) {
        String splitCharacter = "<##>";
        String[] prettyFormat = {"%h", "%H", "%s", "%f", "%b", "%at", "%ct", "%an", "%ae", "%cn", "%ce", "%N", ""};

        String command = String.format(
                "git log -1 --pretty=format:\"%s\" && git rev-parse --abbrev-ref HEAD && git tag --contains HEAD",
                String.join(splitCharacter, prettyFormat)
        );

        List<String> outputLines = executeCommand(command);

        if (outputLines.isEmpty()) {
            throw new RuntimeException("Git command returned empty output.");
        }

        String[] res = String.join("\n", outputLines).split(splitCharacter);

        // Extract branch and tags
        String[] branchAndTags = res[res.length - 1].split("\n");
        List<String> branchAndTagsList = Arrays.asList(branchAndTags);
        String branch = envVars.getOrDefault("CURRENT_BRANCH", branchAndTagsList.get(0));


        return new GitInfo(
                branch,
                res[0], // commitId
                res[2], // commitMessage
                res[7], // commitAuthor
                getGitHubURL(envVars, res[1]), // githubURL
                envVars.getOrDefault("BASELINE_BRANCH", "")
        );
    }

    private static String shortenCommitId(String commitId) {
        return (commitId != null && commitId.length() >= 6) ? commitId.substring(0, 6) : "";
    }

    private static String getGitHubURL(Map<String, String> envVars, String commitId) {
        if (envVars.containsKey("GITHUB_ACTIONS")) {
            String repo = System.getenv("GITHUB_REPOSITORY");
            return String.format("https://api.github.com/repos/%s/statuses/%s", repo, commitId);
        }
        return "";
    }

    private static List<String> executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            return new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error executing Git command: " + e.getMessage(), e);
        }
    }
}
