package io.github.lambdatest;

import io.appium.java_client.AppiumDriver;
import io.github.lambdatest.constants.Constants;
import io.github.lambdatest.models.BuildData;
import io.github.lambdatest.models.UploadSnapshotRequest;
import io.github.lambdatest.utils.SmartUIUtil;
import org.checkerframework.checker.units.qual.C;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import com.google.gson.Gson;

import java.io.File;
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

    public void start(AppiumDriver appiumDriver, Map<String, Object> options) {
        if (Objects.isNull(appiumDriver)) {
            throw new IllegalArgumentException(Constants.Errors.SELENIUM_DRIVER_NULL);
        }
        String deviceName = appiumDriver.getCapabilities().getCapability("deviceName").toString();
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

        boolean isServerUp = util.isSmartUIRunning();
        if (!isServerUp) {
            log.severe("Error in authenticating user with projectToken: " + projectToken);
            throw new IllegalArgumentException(Constants.Errors.SMARTUI_NOT_RUNNING);
        }

        //Authenticate user for projectToken and create build for User's session
        try {
            this.buildData = util.build(projectToken, appiumDriver, options);
            log.info("Created build, received response :" + this.buildData);
            Objects.requireNonNull(options).put("buildName", this.buildData.getName());
        } catch (Exception e) {
            log.severe("Couldn't create build due to Exception: " + e.getMessage());
            throw new IllegalStateException("Couldn't create build due to error: " + e.getMessage());
        }
    }

    public void smartUiAppSnapshot(AppiumDriver appiumDriver, String screenshotName, Map<String, Object> options) {
        try {
            String deviceName = appiumDriver.getCapabilities().getCapability("deviceName").toString();
            String platformName = appiumDriver.getCapabilities().getCapability("platformName").toString();
            String platformVersion = appiumDriver.getCapabilities().getCapability("platformVersion").toString();
            String os = platformName + platformVersion;

            if(Objects.isNull(screenshotName)){
                log.info(Constants.Errors.SNAPSHOT_NAME_NULL);
                return;
            }

            TakesScreenshot takesScreenshot = (TakesScreenshot) appiumDriver;
            File screenshot = takesScreenshot.getScreenshotAs(OutputType.FILE);
            log.info("Screenshot captured: " + screenshotName);

            UploadSnapshotRequest uploadSnapshotRequest = new UploadSnapshotRequest();
            uploadSnapshotRequest.setScreenshotName(screenshotName);
            uploadSnapshotRequest.setScreenshot(screenshot);
            uploadSnapshotRequest.setProjectToken(projectToken);
            uploadSnapshotRequest.setOs(os);
            uploadSnapshotRequest.setDeviceName(deviceName);

            if (Objects.nonNull(buildData)) {
                uploadSnapshotRequest.setBuildId(buildData.getBuildId());
                uploadSnapshotRequest.setBuildName(buildData.getName());
            }

            String uploadRequest = gson.toJson(uploadSnapshotRequest);
            log.info("Adding upload for request: " + uploadRequest);

            // Upload Screenshot
            util.uploadScreenshot(uploadRequest);
            log.info("Uploaded screenshot for request: " + uploadRequest);
        } catch (Exception e) {
            log.severe("Error in uploading screenshot: " + e.getMessage());
        }
    }

    public void stop(){
        try {
            if (this.buildData != null) {
                log.info("Stopping session for buildId: " + buildData.getBuildId());
                if(Objects.nonNull(buildData.getBuildId())){
                    util.stop(buildData.getBuildId());
                    log.info("Session ended for token: " + projectToken);}
                log.info("Build ID not found for: "+ projectToken + "with build :"+ buildData);
            }
        } catch (Exception e) {
            log.severe("Couldn't stop the build due to an exception: " + e.getMessage());
        }
    }
}