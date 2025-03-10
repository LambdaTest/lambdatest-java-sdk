package io.github.lambdatest;


import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.Dimension;
import io.github.lambdatest.constants.Constants;
import io.github.lambdatest.models.*;
import io.github.lambdatest.utils.GitUtils;
import io.github.lambdatest.utils.SmartUIUtil;
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
    private String deviceName;
    private String platform;
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

        this.projectToken = getProjectToken(options);
        log.info("Project token set as: " + this.projectToken);
        try{
        this.deviceName = getDeviceName(options);
        } catch (Exception e){
            log.severe("Couldnt set device name due to error :"+ e.getMessage());
        }
        try{
            this.platform = getPlatform(options, appiumDriver, this.deviceName);
        } catch (Exception e){
            log.severe("Couldnt set platform due to error :"+ e.getMessage());
        }
        log.info("Device name retrieved: " + this.deviceName);
        log.info("Platform retrieved: " + this.platform);
        Map<String, String> envVars = System.getenv();
        GitInfo git = GitUtils.getGitInfo(envVars);

        // Authenticate user and create a build
        try {
            BuildResponse buildRes = util.build(git, this.projectToken, options);
            this.buildData = buildRes.getData();
            log.info("Build data set: " + this.buildData);
            options.put("buildName", this.buildData.getName());
        } catch (Exception e) {
            log.severe("Couldn't create build: " + e.getMessage());
            throw new IllegalStateException("Couldn't create build: " + e.getMessage());
        }
    }

    private String getProjectToken(Map<String, String> options) {
        if (options != null && options.containsKey("projectToken")) {
            String token = options.get("projectToken").trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        String envToken = System.getenv("PROJECT_TOKEN");
        if (envToken != null && !envToken.trim().isEmpty()) {
            return envToken.trim();
        }
        throw new IllegalArgumentException(Constants.Errors.PROJECT_TOKEN_UNSET);
    }

    private String getDeviceName(Map<String, String> options) {
        if (options != null && options.containsKey("deviceName")) {
            String name = options.get("deviceName").trim();
            if (!name.isEmpty()) {
                return name;
            }
            else{
            log.info("Device name provided in options is empty, falling back to driver capabilities.");}
        }
        return deviceName;
    }

    private String getPlatform(Map<String, String> options,  AppiumDriver appiumDriver, String deviceName) {
        String platForm = "";
        if (deviceName != null && !deviceName.isEmpty()) {
            if (options != null && options.containsKey("platform")) {
                platForm =  options.get("platform").trim();
                log.info("Platform retrieved from options: " + options.get("platform").trim());
            }
        }
        else {
            Object deviceNameObj = appiumDriver.getCapabilities().getCapability("deviceName");
            if(Objects.isNull(deviceNameObj))
                throw new NullPointerException("Device Name is a mandatory parameter.");
            String deviceNameFromCap = (String)  deviceNameObj;
            log.info("Device name retrieved from driver capabilities: " + deviceName);
            this.deviceName = deviceNameFromCap;

            Object platformNameObj = appiumDriver.getCapabilities().getCapability("platformName");
            Object platformVersionObj = appiumDriver.getCapabilities().getCapability("platformVersion");
            String platformName = "" , platformVersion = "";
            if (platformNameObj != null && platformVersionObj != null) {
                platformName = (platformNameObj instanceof String) ? (String) platformNameObj : "default";
                platformVersion = (platformVersionObj instanceof String) ? (String)  platformVersionObj : "0";
            } else {
                log.info("Missing mandatory parameter " + "platformName and platformVersion");
                throw new NullPointerException("platformName Name and platformVersion is a mandatory parameter.");
            }
            platForm = platformName + platformVersion;
            log.info("Platform retrieved from driver capabilities: " + platForm);
        }
        return platForm;
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

            Dimension viewport = appiumDriver.manage().window().getSize();
            int width = viewport.getWidth();
            int height = viewport.getHeight();

            TakesScreenshot takesScreenshot = (TakesScreenshot) appiumDriver;
            File screenshot = takesScreenshot.getScreenshotAs(OutputType.FILE);
            log.info("Screenshot captured: " + screenshotName);
            UploadSnapshotRequest uploadSnapshotRequest = new UploadSnapshotRequest();
            uploadSnapshotRequest.setScreenshotName(screenshotName);
            uploadSnapshotRequest.setProjectToken(projectToken);
            uploadSnapshotRequest.setOs(this.platform);
            uploadSnapshotRequest.setDeviceName(this.deviceName);
            String w = String.valueOf(width);
            String h = String.valueOf(height);
            uploadSnapshotRequest.setViewport(w + "x" + h);
            uploadSnapshotRequest.setBrowserName("chrome");
            if (Objects.nonNull(buildData)) {
                uploadSnapshotRequest.setBuildId(buildData.getBuildId());
                uploadSnapshotRequest.setBuildName(buildData.getName());
            }
            //Upload Screenshot API call
            UploadSnapshotResponse uploadSnapshotResponse = util.uploadScreenshot(screenshot,uploadSnapshotRequest, this.buildData);
            log.info("For uploading: " + uploadSnapshotRequest.getScreenshotName() + "received response: "+ uploadSnapshotResponse.getData());
        } catch (Exception e) {
            log.severe(Constants.Errors.UPLOAD_SNAPSHOT_FAILED +" due to: " +e.getMessage());
        }
    }

    public void stop() throws Exception{
        try {
            if (this.buildData != null) {
                log.info("Stopping session for buildId: " + this.buildData.getBuildId());
                if(Objects.nonNull(this.buildData.getBuildId())){
                    util.stopBuild(this.buildData.getBuildId(),  projectToken);
                    log.info("Session ended for token: " + projectToken);}
                else {
                    log.info("Build ID not found for stopBuild: "+ projectToken);
                }
            }
        } catch (Exception e) {
            log.severe("Couldn't stop the build due to an exception: " + e.getMessage());
            throw new Exception(Constants.Errors.STOP_BUILD_FAILED +" due to : "+ e.getMessage());
        }
    }
}