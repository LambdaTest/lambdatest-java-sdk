package io.github.lambdatest.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lambdatest.models.GitInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Logger;


public class GitUtils {

    private static Logger log = LoggerUtil.createLogger("lambdatest-java-sdk");

    public static GitInfo getGitInfo(Map<String, String> envVars) {
        String gitInfoFilePath = envVars.get("SMARTUI_GIT_INFO_FILEPATH");
        if (gitInfoFilePath != null) {
            return readGitInfoFromFile(gitInfoFilePath, envVars);
        } else {
            GitInfo gitInfo = fetchGitInfoFromCommands(envVars);
            return  gitInfo;
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
                    envVars.getOrDefault("BASELINE_BRANCH", ""));
        } catch (IOException e) {
            log.info("Error reading Git info file: " + e.getMessage());
            return null;
        }
    }

    private static GitInfo fetchGitInfoFromCommands(Map<String, String> envVars) {
        String splitCharacter = "<##>";
        String[] prettyFormat = { "%h", "%H", "%s", "%f", "%b", "%at", "%ct", "%an", "%ae", "%cn", "%ce", "%N", "" };

        String command = String.format(
                "git log -1 --pretty=format:\"%s\" && git rev-parse --abbrev-ref HEAD && git tag --contains HEAD",
                String.join(splitCharacter, prettyFormat));
        List<String> outputLines = executeCommand(command);

        if (outputLines.isEmpty()) {
            return new GitInfo("", "", "", "", "", "");
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
                envVars.getOrDefault("BASELINE_BRANCH", ""));
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
        List<String> outputLines = new ArrayList<>();
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Process process;
            if (os.contains("win")) {
                // For Windows, use cmd.exe to execute the command
                process = Runtime.getRuntime().exec(new String[] { "cmd.exe", "/c", command });
            } else {
                // For Unix-like systems (Linux, macOS), use /bin/sh
                process = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", command });
            }
            // Read both the output and error streams
            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                // Capture standard output (stdout)
                while ((line = inputReader.readLine()) != null) {
                    outputLines.add(line);
                }
                // Capture error output (stderr)
                while ((line = errorReader.readLine()) != null) {
                    log.severe("Error: " + line);
                }
            }
            // Wait for the command to complete
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.severe("Command failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            log.severe("Error executing command: " + e.getMessage());
            return new ArrayList<String>();
        }
        return outputLines;
    }
}
