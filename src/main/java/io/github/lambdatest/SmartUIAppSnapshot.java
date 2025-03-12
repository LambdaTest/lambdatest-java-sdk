package io.github.lambdatest;


import com.google.gson.Gson;
import io.appium.java_client.AppiumDriver;
import io.github.lambdatest.constants.Constants;
import io.github.lambdatest.models.*;
import io.github.lambdatest.utils.GitUtils;
import io.github.lambdatest.utils.SmartUIUtil;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

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


    public void start(Map<String, String> options) {
        if (options == null) {
            throw new IllegalArgumentException(Constants.Errors.NULL_OPTIONS_OBJECT);
        }
        try{
            this.projectToken = getProjectToken(options);
            log.info("Project token set as: " + this.projectToken);
        } catch (Exception e){
            log.severe(Constants.Errors.PROJECT_TOKEN_UNSET);
        }
        Map<String, String> envVars = System.getenv();
        GitInfo git = GitUtils.getGitInfo(envVars);
        // Authenticate user and create a build
        try {
            BuildResponse buildRes = util.build(git, this.projectToken, options);
            this.buildData = buildRes.getData();
            log.info("Build ID set : " + this.buildData.getBuildId() + "for Build name : "+ this.buildData.getName());
            options.put("buildName", this.buildData.getName());
        } catch(Exception e) {
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


    public void smartUiAppSnapshot(AppiumDriver appiumDriver, String screenshotName, Map<String, String> options) {
        try {
            if (Objects.isNull(appiumDriver)) {
                log.severe(Constants.Errors.SELENIUM_DRIVER_NULL +" during take snapshot");
                throw new IllegalArgumentException(Constants.Errors.SELENIUM_DRIVER_NULL);
            }
            if(Objects.isNull(screenshotName)){
                log.info(Constants.Errors.SNAPSHOT_NAME_NULL);
                throw new IllegalArgumentException(Constants.Errors.SNAPSHOT_NAME_NULL);
            }

            TakesScreenshot takesScreenshot = (TakesScreenshot) appiumDriver;
            File screenshot = takesScreenshot.getScreenshotAs(OutputType.FILE);
            log.info("Screenshot captured: " + screenshotName);
            UploadSnapshotRequest uploadSnapshotRequest = new UploadSnapshotRequest();
            uploadSnapshotRequest.setScreenshotName(screenshotName);
            uploadSnapshotRequest.setProjectToken(projectToken);
            String platform = "", deviceName="";
            if(options != null && options.containsKey("platform")){
                platform = options.get("platform").trim();
            }
            if(options != null && options.containsKey("deviceName")){
                deviceName = options.get("deviceName").trim();
            }
            if(Objects.isNull(deviceName) && deviceName.isEmpty()){
                throw new IllegalStateException(Constants.Errors.DEVICE_NAME_NULL);
            }
            if(Objects.isNull(platform) && platform.isEmpty()){
                if(deviceName.startsWith("i")){
                    platform =  "iOS";
                }
                else {
                    platform = "Android";
                }
            }
            uploadSnapshotRequest.setOs(platform);
            uploadSnapshotRequest.setDeviceName(deviceName +"-" +platform);
            log.info("In Upload Req - Device name is set to :"+ deviceName);
            log.info("In Upload Req - Platform is set to :"+ platform);
            if(platform.equalsIgnoreCase("ios")){
                uploadSnapshotRequest.setBrowserName("safari");
            }
            else {
                uploadSnapshotRequest.setBrowserName("chrome");}
            log.info("In Upload Req - Browser name is set to :"+ uploadSnapshotRequest.getBrowserName());
            if (Objects.nonNull(buildData)) {
                uploadSnapshotRequest.setBuildId(buildData.getBuildId());
                log.info("In Upload Req - Build id set to :"+ buildData.getBuildId());
                uploadSnapshotRequest.setBuildName(buildData.getName());
                log.info("In Upload Req - Build name set to :"+ buildData.getName());
            }
            //Upload Screenshot API call
            UploadSnapshotResponse uploadSnapshotResponse = util.uploadScreenshot(screenshot,uploadSnapshotRequest, this.buildData);
            log.info("For uploading: " + uploadSnapshotRequest.getScreenshotName() + " received response: "+ uploadSnapshotResponse.getData());
        } catch (Exception e) {
            log.severe(Constants.Errors.UPLOAD_SNAPSHOT_FAILED + " due to: " +e.getMessage());
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
                    log.info("Build ID not found to stop build for "+ projectToken);
                }
            }
        } catch (Exception e) {
            log.severe("Couldn't stop the build due to an exception: " + e.getMessage());
            throw new Exception(Constants.Errors.STOP_BUILD_FAILED +" due to : "+ e.getMessage());
        }
    }
}