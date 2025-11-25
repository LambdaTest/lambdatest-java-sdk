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

    public UploadPDFResponse postPDFToSmartUI(List<File> pdfFiles, String projectToken, String buildName) throws Exception {
        UploadPDFResponse uploadResponse;
        try {
            if (pdfFiles == null || pdfFiles.isEmpty()) {
                throw new IllegalArgumentException("PDF files list cannot be null or empty");
            }
            
            if (projectToken == null || projectToken.trim().isEmpty()) {
                throw new IllegalArgumentException("Project token cannot be null or empty");
            }

            String hostUrl = Constants.getUploadHostUrlFromEnvOrDefault();
            String url = hostUrl + Constants.SmartUIRoutes.SMARTUI_UPLOAD_PDF_ROUTE;
            
            log.info("Uploading PDFs to SmartUI. Count: " + pdfFiles.size());

            String responseString = httpClient.uploadPDFs(url, pdfFiles, projectToken, buildName);
            uploadResponse = gson.fromJson(responseString, UploadPDFResponse.class);
            
            if (uploadResponse == null) {
                throw new IllegalStateException("Failed to parse PDF upload response");
            }
            
            if (!uploadResponse.isSuccessful()) {
                String errorMessage = uploadResponse.getMessage() != null ? 
                    uploadResponse.getMessage() : "Unknown error occurred";
                throw new IllegalStateException("PDF upload failed: " + errorMessage);
            }
            
            log.info(uploadResponse.getMessage());
            log.info("Build ID: " + uploadResponse.getBuildId());
            log.info("Project: " + uploadResponse.getProjectName());
            log.info("Build URL: " + uploadResponse.getBuildURL());
            
            return uploadResponse;
        } catch (Exception e) {
            log.severe("Failed to upload PDFs to SmartUI: " + e.getMessage());
            throw new Exception("Couldn't upload PDFs to SmartUI because of error: " + e.getMessage());
        }
    }


    public static String getSmartUIServerAddress() {
        String smartUiServerAddress = System.getenv(Constants.SMARTUI_SERVER_ADDRESS);
        if (smartUiServerAddress != null && !smartUiServerAddress.isEmpty()) {
            return smartUiServerAddress;
        }
        smartUiServerAddress = System.getProperty(Constants.SMARTUI_SERVER_ADDRESS);
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

    public BuildScreenshotsResponse getBuildScreenshots(String projectId, String buildId, String projectToken) throws Exception {
        return getBuildScreenshots(projectId, buildId, projectToken, false, 60);
    }

    public BuildScreenshotsResponse getBuildScreenshots(String projectId, String buildId, String projectToken, boolean baseline, int maxRetries) throws Exception {
        try {
            if (projectId == null || projectId.trim().isEmpty()) {
                throw new IllegalArgumentException("Project ID cannot be null or empty");
            }
            if (buildId == null || buildId.trim().isEmpty()) {
                throw new IllegalArgumentException("Build ID cannot be null or empty");
            }
            if (projectToken == null || projectToken.trim().isEmpty()) {
                throw new IllegalArgumentException("Project token cannot be null or empty");
            }

            String hostUrl = Constants.getUploadHostUrlFromEnvOrDefault();
            String url = hostUrl + Constants.SmartUIRoutes.SMARTUI_BUILD_SCREENSHOTS_ROUTE + 
                        "?project_id=" + projectId + 
                        "&baseline=" + baseline +
                        "&build_id=" + buildId;
            
            String username = System.getenv("LT_USERNAME");
            String accessKey = System.getenv("LT_ACCESS_KEY");
            
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("LT_USERNAME environment variable is required");
            }
            if (accessKey == null || accessKey.trim().isEmpty()) {
                throw new IllegalArgumentException("LT_ACCESS_KEY environment variable is required");
            }
            
            String credentials = username + ":" + accessKey;
            String basicAuth = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");
            headers.put("Authorization", "Basic " + basicAuth);
            
            log.info("Fetching build status for build: " + buildId);

            String responseString = httpClient.getBuildScreenshotsWithPolling(url, headers, maxRetries);
            BuildScreenshotsResponse response = gson.fromJson(responseString, BuildScreenshotsResponse.class);
            
            if (response == null) {
                throw new IllegalStateException("Failed to parse build screenshots response");
            }
            
            log.info("Number of screenshots: " + (response.getScreenshots() != null ? response.getScreenshots().size() : 0));
            if (response.getBuild() != null) {
                log.info("Build status: " + response.getBuild().getBuildStatus());
            }
            
            return response;
        } catch (Exception e) {
            log.severe("Failed to fetch build screenshots: " + e.getMessage());
            throw e;
        }
    }

    public String getSnapshotStatus(String contextId, String snapshotName, int timeout) throws Exception {
        try {
            String snapshotStatus = httpClient.getSnapshotStatus(contextId, snapshotName, timeout);
            log.info("Got snapshot status for snapshotName: " + snapshotName);
            return snapshotStatus;
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error occurred";
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", errorMessage);
            log.severe("Failed to get snapshot status due to: " + errorResponse.toString());
            return gson.toJson(errorResponse);
        }
    }
}