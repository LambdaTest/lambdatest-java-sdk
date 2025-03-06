package io.github.lambdatest;

import io.appium.java_client.AppiumDriver;
import io.github.lambdatest.constants.Constants;
import io.github.lambdatest.models.BuildData;
import io.github.lambdatest.models.UploadSnapshotRequest;
import io.github.lambdatest.models.UploadSnapshotResponse;
import io.github.lambdatest.utils.SmartUIUtil;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import com.google.gson.Gson;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class SmartUIAppSnapshot {
    private final Logger log;
    private final SmartUIUtil util;
    private final Gson gson;
    private String projectToken;
    private BuildData buildData;

    public SmartUIAppSnapshot() {
        this.log = Logger.getLogger("lt-smart-ui-java-sdk");
        this.util = new SmartUIUtil();
        this.gson = new Gson();
    }

    public void start(AppiumDriver appiumDriver, Map<String, String> options) {
        if (Objects.isNull(appiumDriver)) {
            throw new IllegalArgumentException(Constants.Errors.SELENIUM_DRIVER_NULL);
        }
        String projectToken = null;
        if(options != null && options.containsKey("projectToken")) {
            projectToken = String.valueOf(options.get("projectToken")).trim();
        }

        if (projectToken == null || projectToken.isEmpty()) {
            String envToken = System.getenv("PROJECT_TOKEN");
            if (envToken != null && !envToken.trim().isEmpty()) {
                projectToken = envToken.trim();
            } else {
                throw new IllegalArgumentException(Constants.Errors.PROJECT_TOKEN_UNSET);
            }
        }
        log.info("Project token set as :"+ projectToken);
        this.projectToken = projectToken;
        Objects.requireNonNull(options).put("projectToken", projectToken);

//        boolean isServerUp = util.isSmartUIRunning();
//        if (!isServerUp) {
//            log.severe("Error in authenticating user with projectToken: " + projectToken);
//            throw new IllegalArgumentException(Constants.Errors.SMARTUI_NOT_RUNNING);
//        }

        //Authenticate user for projectToken and create build for User
        try {
            this.buildData = util.build(projectToken, appiumDriver, options);
            log.info("Created build, received response :" + this.buildData);
            Objects.requireNonNull(options).put("buildName", this.buildData.getName());
        } catch (Exception e) {
            log.severe("Couldn't create build due to Exception: " + e.getMessage());
            throw new IllegalStateException("Couldn't create build due to error: " + e.getMessage());
        }
    }

    public void smartUiAppSnapshot(AppiumDriver appiumDriver, String screenshotName, Map<String, String> options) {
        try {
            if (Objects.isNull(appiumDriver)) {
                log.severe(Constants.Errors.SELENIUM_DRIVER_NULL +" during take snapshot");
                return;
            }
            if(Objects.isNull(screenshotName)){
                log.info(Constants.Errors.SNAPSHOT_NAME_NULL);
                return;
            }
            String deviceName = (String) (appiumDriver.getCapabilities().getCapability("deviceName"));
            String platformName = (String) appiumDriver.getCapabilities().getCapability("platformName");
            String platformVersion = (String) appiumDriver.getCapabilities().getCapability("platformVersion");
            //ToDo: Clarify with Sushobhit sir
            Map<String, Object> deviceOptions = new HashMap<>();
            deviceOptions.put("platformName", platformName);
            deviceOptions.put("platformVersion", platformVersion);

            TakesScreenshot takesScreenshot = (TakesScreenshot) appiumDriver;
            File screenshot = takesScreenshot.getScreenshotAs(OutputType.FILE);
            log.info("Screenshot captured: " + screenshotName);

            UploadSnapshotRequest uploadSnapshotRequest = new UploadSnapshotRequest();
            uploadSnapshotRequest.setScreenshotName(screenshotName);
            uploadSnapshotRequest.setScreenshot(screenshot);
            uploadSnapshotRequest.setProjectToken(projectToken);
            uploadSnapshotRequest.setOs(deviceOptions);
            uploadSnapshotRequest.setDeviceName(deviceName);
            if (Objects.nonNull(buildData)) {
                uploadSnapshotRequest.setBuildId(buildData.getBuildId());
                uploadSnapshotRequest.setBuildName(buildData.getName());
            }
            String uploadRequest = gson.toJson(uploadSnapshotRequest);
            log.info("Adding upload request : " + uploadRequest);

            //Upload Screenshot API call
            UploadSnapshotResponse uploadSnapshotResponse = util.uploadScreenshot(uploadRequest);
            log.info("For: " + uploadRequest + "received response: "+ uploadSnapshotResponse);
        } catch (Exception e) {
            log.severe("Error in uploading screenshot: " + e.getMessage());
        }
    }

    public void stop() throws Exception{
        try {
            if (this.buildData != null) {
                log.info("Stopping session for buildId: " + buildData.getBuildId());
                if(Objects.nonNull(buildData.getBuildId())){
                    util.stop(buildData.getBuildId());
                    log.info("Session ended for token: " + projectToken);}
                else {
                    log.info("Build ID not found for: "+ projectToken + "with build :"+ buildData);
                }
            }
        } catch (Exception e) {
            log.severe("Couldn't stop the build due to an exception: " + e.getMessage());
            throw new Exception(Constants.Errors.STOP_BUILD_FAILED +" due to : "+ e.getMessage());
        }
    }
}