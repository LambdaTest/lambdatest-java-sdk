package io.github.lambdatest.utils;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

import io.github.lambdatest.models.*;
import com.google.gson.Gson;
import io.github.lambdatest.constants.Constants;


public class SmartUIUtil {
    private final HttpClientUtil httpClient;
    private final Logger log = LoggerUtil.createLogger("lambdatest-java-sdk");
    private Gson gson = new Gson();

    public SmartUIUtil() {
        this.httpClient = new HttpClientUtil();
    }

    public SmartUIUtil(String proxyHost, int proxyPort) throws Exception {
        this.httpClient = new HttpClientUtil(proxyHost, proxyPort);
    }

    public SmartUIUtil(String proxyHost, int proxyPort, boolean allowInsecure) throws Exception {
        this.httpClient = new HttpClientUtil(proxyHost, proxyPort, allowInsecure);
    }

    public SmartUIUtil(String proxyProtocol, String proxyHost, int proxyPort, boolean allowInsecure) throws Exception {
        this.httpClient = new HttpClientUtil(proxyProtocol, proxyHost, proxyPort, allowInsecure);
    }

    public boolean isSmartUIRunning() {
        try {
            httpClient.isSmartUIRunning();
            return true;
        } catch (Exception e) {
            log.severe("Exception occurred " + e);
            return false;
        }
    }

    public boolean isUserAuthenticated(String projectToken) throws Exception {
        return httpClient.isUserAuthenticated(projectToken);
    }

    public String fetchDOMSerializer() throws Exception {
        try {
            return httpClient.fetchDOMSerializer();
        } catch (Exception e) {
            log.severe(e.getMessage());
            throw new Exception(Constants.Errors.FETCH_DOM_FAILED, e);
        }
    }

    public String postSnapshot(Object snapshotDOM, Map<String, Object> options, String url, String snapshotName,
            String testType) throws Exception {
        // Create Snapshot and SnapshotData objects
        Snapshot snapshot = new Snapshot();
        snapshot.setDom(snapshotDOM);
        snapshot.setName(snapshotName);
        snapshot.setOptions(options);
        snapshot.setURL(url);

        SnapshotData data = new SnapshotData();
        data.setSnapshot(snapshot);
        data.setTestType(testType);

        // Serialize to JSON using Gson
        Gson gson = new Gson();
        String jsonData = gson.toJson(data);

        try {
            return httpClient.postSnapshot(jsonData);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getSmartUIServerAddress() {
        String smartUiServerAddress = Constants.SmartUIRoutes.SMARTUI_CLIENT_API_URL;
        if (smartUiServerAddress != null && !smartUiServerAddress.isEmpty()) {
            return smartUiServerAddress;
        } else {
            return "http://localhost:49152";
        }
    }

    public void uploadScreenshot(File screenshotFile, UploadSnapshotRequest uploadScreenshotRequest,
                                 BuildData buildData) throws Exception {
        UploadSnapshotResponse uploadAPIResponse = new UploadSnapshotResponse();
        try {
            if(Objects.isNull(screenshotFile)){
                throw new RuntimeException(Constants.Errors.SNAPSHOT_NOT_FOUND);
            }
            String hostUrl = Constants.getHostUrlFromEnvOrDefault();
            String url = hostUrl + Constants.SmartUIRoutes.SMARTUI_UPLOAD_SCREENSHOT_ROUTE;
            String uploadScreenshotResponse = httpClient.uploadScreenshot(url, screenshotFile, uploadScreenshotRequest, buildData);
            uploadAPIResponse = gson.fromJson(uploadScreenshotResponse, UploadSnapshotResponse.class);
            if (Objects.isNull(uploadAPIResponse))
                throw new IllegalStateException("Failed to upload screenshot to SmartUI");
        } catch (Exception e) {
            throw new Exception("Couldn't upload image to SmartUI because of error : " + e.getMessage());
        }
    }

    public BuildResponse build(GitInfo git, String projectToken, Map<String, String> options) throws Exception {
        boolean isAuthenticatedUser = isUserAuthenticated(projectToken);
        if (!isAuthenticatedUser) {
            log.severe("Authentication failed for projectToken: " + projectToken);
            throw new Exception(Constants.Errors.USER_AUTH_ERROR);
        }
        CreateBuildRequest createBuildRequest = new CreateBuildRequest();
        if (options != null && options.containsKey("buildName")) {
            String buildNameStr = options.get("buildName");

            if (buildNameStr != null && !buildNameStr.trim().isEmpty()) {
                createBuildRequest.setBuildName(buildNameStr);
                log.info("Build name set from options: " + buildNameStr);
            } else {
                buildNameStr = "smartui-" + UUID.randomUUID().toString().substring(0, 10);
                createBuildRequest.setBuildName(buildNameStr);
                log.info("Build name set from system: " + buildNameStr);
            }
        } else {
            createBuildRequest.setBuildName("smartui-" + UUID.randomUUID().toString().substring(0, 10));
        }

        if (Objects.nonNull(git)) {
            createBuildRequest.setGit(git);
        }
        String createBuildJson = gson.toJson(createBuildRequest);
        Map<String, String> header = new HashMap<>();
        header.put(Constants.PROJECT_TOKEN, projectToken);
        String createBuildResponse = httpClient.createSmartUIBuild(createBuildJson, header);
        BuildResponse buildData = gson.fromJson(createBuildResponse, BuildResponse.class);
        if (Objects.isNull(buildData)) {
            throw new Exception("Build not created for projectToken: " + projectToken);
        }
        return buildData;
    }

    public void stopBuild(String buildId, String projectToken) throws Exception {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put(Constants.PROJECT_TOKEN, projectToken);
            httpClient.stopBuild(buildId, headers);
        } catch (Exception e) {
            throw new Exception(Constants.Errors.STOP_BUILD_FAILED + " due to: " + e.getMessage());
        }
    }
}