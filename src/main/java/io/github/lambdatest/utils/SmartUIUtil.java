package io.github.lambdatest.utils;


import java.util.*;
import java.util.logging.Logger;

import io.appium.java_client.AppiumDriver;
import io.github.lambdatest.models.*;
import com.google.gson.Gson;
import io.github.lambdatest.constants.Constants;
import org.openqa.selenium.Capabilities;

public class SmartUIUtil {
    private final HttpClientUtil httpClient;
    private final Logger log;
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

    public void uploadScreenshot(String uploadScreenshotRequest) throws Exception {
        try{
            String uploadScreenshotResponse = httpClient.uploadScreenshot(uploadScreenshotRequest);
            UploadSnapshotResponse uploadAPIResponse = gson.fromJson(uploadScreenshotResponse, UploadSnapshotResponse.class);
            if(Objects.isNull(uploadAPIResponse))
                throw new IllegalStateException("Failed to upload screenshot to SmartUI");
        }
        catch (Exception e){
            throw new Exception("Couldn't upload image to SmartUI because of error :"+ e.getMessage());
        }
    }

    public BuildData build(String projectToken, AppiumDriver driver, Map<String, Object> options) throws Exception {
        boolean isAuthenticatedUser = isUserAuthenticated(projectToken);
        if(!isAuthenticatedUser){
            throw new IllegalArgumentException(Constants.Errors.USER_AUTH_ERROR);
        }

//        Capabilities capabilities = driver.getCapabilities();
//        Map<String, Object> capabilitiesMap = capabilities.asMap();
//        String deviceName= null;
//        String os = null;
//        if (capabilitiesMap != null && capabilitiesMap.containsKey("deviceName") && capabilitiesMap.containsKey("platformName") && capabilitiesMap.containsKey("platformVersion")) {
//             deviceName = capabilitiesMap.get("deviceName").toString();
//            String platformName = capabilitiesMap.get("platformName").toString();
//            String platformVersion = capabilitiesMap.get("platformVersion").toString();
//             os = platformName+platformVersion;}
        //Create build request
        CreateBuildRequest createBuildRequest = new CreateBuildRequest();
        BuildConfig buildConfig = new BuildConfig();

        if (options != null && options.containsKey("buildName")) {
            Object buildNameValue = options.get("buildName");

            // Check if value is non-null and a valid String
            if (buildNameValue instanceof String && !((String) buildNameValue).trim().isEmpty()) {
                createBuildRequest.setBuildName((String) buildNameValue);
            } else {
                createBuildRequest.setBuildName("smartui-" + UUID.randomUUID().toString().substring(0, 8));
            }
        } else {
            createBuildRequest.setBuildName("smartui-" + UUID.randomUUID().toString().substring(0, 8)); //Setting buildName for case when user hasn't given
        }

        buildConfig.setScrollTime(8);
        buildConfig.setEnableJavaScript(false);
        buildConfig.setWaitForPageRender(3000);
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

    public void stop(String buildId) throws IllegalArgumentException{
        try{
            httpClient.stopBuild(buildId);
        }
        catch (Exception e){
            throw new IllegalArgumentException(Constants.Errors.STOP_BUILD_FAILED);
        }
    }
}