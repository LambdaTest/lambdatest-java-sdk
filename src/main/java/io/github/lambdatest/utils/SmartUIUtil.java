package io.github.lambdatest.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import io.github.lambdatest.models.*;
import com.google.gson.Gson;
import io.github.lambdatest.constants.Constants;

import java.util.Map;

public class SmartUIUtil {
    private final HttpClientUtil httpClient;
    private Logger log;
    Gson gson = new Gson();

    public SmartUIUtil() {
        this.httpClient = new HttpClientUtil();
        this.log = LoggerUtil.createLogger("lambdatest-java-sdk");
    }

    public boolean isSmartUIRunning() {
        try {
            httpClient.isSmartUIRunning();
            return true;
        } catch (Exception e) {
            log.severe(e.getMessage());
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

    public String postSnapshot(Object snapshotDOM, Map<String, Object> options, String url, String snapshotName, String testType) throws Exception {
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
        String smartUiServerAddress = System.getenv(Constants.SMARTUI_SERVER_ADDRESS);
        if (smartUiServerAddress != null && !smartUiServerAddress.isEmpty()) {
            return smartUiServerAddress;
        } else {
            throw new RuntimeException("SmartUI server address not found");
        }
    }

    public void uploadScreenshot(String uploadScreenshotRequest) {
        try{
            String uploadScreenshotResponse = httpClient.uploadScreenshot(uploadScreenshotRequest);
            UploadSnapshotResponse uploadAPIResponse = gson.fromJson(uploadScreenshotResponse, UploadSnapshotResponse.class);
            if(Objects.isNull(uploadAPIResponse))
                throw new RuntimeException("Failed to upload screenshot to SmartUI");
        }
        catch (Exception e){
            log.severe("Exception occurred during uploading screenshot :" + e.toString());
            throw new RuntimeException("Couldn't upload image to SmartUI because of error :"+ e.getMessage());
        }
    }

    public BuildData build(String projectToken) throws IOException {
        boolean isAuthenticatedUser = httpClient.isUserAuthenticated(projectToken);
        if(!isAuthenticatedUser){
            throw new IllegalArgumentException(Constants.Errors.USER_AUTH_ERROR);
        }
        //Create build request
        CreateBuildRequest createBuildRequest = new CreateBuildRequest();
        BuildConfig buildConfig = new BuildConfig();
        MobileConfig mobile = new MobileConfig(
                Arrays.asList("iPhone 14",
                        "Galaxy S24"),
                true,
                "portrait"
        );
        buildConfig.setMobile(mobile);
        buildConfig.setScrollTime(8);
        buildConfig.setEnableJavaScript(false);
        buildConfig.setWaitForPageRender(1000);
        buildConfig.setWaitForTimeout(1000);
        createBuildRequest.setBuildConfig(buildConfig);
        createBuildRequest.setProjectToken(projectToken);

        String createBuildJson = gson.toJson(createBuildRequest);

        String createBuildResponse = httpClient.createSmartUIBuild(createBuildJson);
        BuildData buildData = gson.fromJson(createBuildResponse, BuildData.class);
        if (Objects.isNull(buildData)) {
            throw new RuntimeException("Build not created for projectToken: "+ projectToken);
        }
        return buildData;
    }

    public String getProjectToken(Map<String, Object> options) {
        String envProjectToken = System.getenv("PROJECT_TOKEN");
        if(envProjectToken != null && !envProjectToken.trim().isEmpty())
            return envProjectToken.trim();
        return Optional.ofNullable(options)
                .map(opts -> options.get("projectToken"))
                .map(Object::toString)
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException(Constants.Errors.PROJECT_TOKEN_UNSET));
    }

    public void stop(String buildId) {
        try{
            httpClient.stopBuild(buildId);
        }
        catch (Exception e){
            log.warning("Failed to stop the build for buildId :"+ buildId);
            throw new RuntimeException(Constants.Errors.STOP_BUILD_FAILED);
        }
    }
}