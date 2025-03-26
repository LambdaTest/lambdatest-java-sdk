package io.github.lambdatest;


import com.google.gson.Gson;
import io.github.lambdatest.constants.Constants;
import io.github.lambdatest.models.*;
import io.github.lambdatest.utils.GitUtils;
import io.github.lambdatest.utils.SmartUIUtil;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import io.github.lambdatest.utils.LoggerUtil;

public class SmartUIAppSnapshot {
    private final Logger log;
    private final SmartUIUtil util;
    private final Gson gson;
    private String projectToken;

    private BuildData buildData;

    public SmartUIAppSnapshot() {
        this.log = LoggerUtil.createLogger("lambdatest-java-app-sdk");
        this.util = new SmartUIUtil();
        this.gson = new Gson();
    }

    public void start(Map<String, String> options) throws Exception{
        try{
            this.projectToken = getProjectToken(options);
            log.info("Project token set as: " + this.projectToken);
        } catch (Exception e){
            log.severe(Constants.Errors.PROJECT_TOKEN_UNSET);
            throw new Exception("Project token is a mandatory field", e);
        }
        Map<String, String> envVars = new HashMap<>(System.getenv());
        GitInfo git = GitUtils.getGitInfo(envVars);
        // Authenticate user and create a build
        try {
            BuildResponse buildRes = util.build(git, this.projectToken, options);
            this.buildData = buildRes.getData();
            log.info("Build ID set : " + this.buildData.getBuildId() + "for Build name : "+ this.buildData.getName());
            options.put("buildName", this.buildData.getName());
        } catch(Exception e) {
            log.severe("Couldn't create build: " + e.getMessage());
            throw new Exception("Couldn't create build: " + e.getMessage());
        }
    }
    public void start() throws Exception{
        String envToken = System.getenv("PROJECT_TOKEN");
        this.projectToken = envToken;
        log.info("Project token set as: " + this.projectToken);
        if(Objects.isNull(this.projectToken)){
            log.severe(Constants.Errors.PROJECT_TOKEN_UNSET);
            throw new Exception("Project token is a mandatory field");
        }
        Map<String, String> envVars = System.getenv();
        GitInfo git = GitUtils.getGitInfo(envVars);
        // Authenticate user and create a build
        try {
            BuildResponse buildRes = util.build(git, this.projectToken, null);
            this.buildData = buildRes.getData();
            log.info("Build ID set : " + this.buildData.getBuildId() + "for Build name : "+ this.buildData.getName());
        } catch(Exception e) {
            log.severe("Couldn't create build: " + e.getMessage());
            throw new Exception("Couldn't create build due to: ", e);
        }
    }

    private String getProjectToken(Map<String, String> options) {
        if (options != null && options.containsKey(Constants.PROJECT_TOKEN)) {
            String token = options.get(Constants.PROJECT_TOKEN).trim();
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


    public void smartuiAppSnapshot(WebDriver appiumDriver, String screenshotName, Map<String, String> options) throws Exception {
        try {
            if (appiumDriver == null) {
                log.severe(Constants.Errors.SELENIUM_DRIVER_NULL +" during take snapshot");
                throw new IllegalArgumentException(Constants.Errors.SELENIUM_DRIVER_NULL);
            }
            if (screenshotName == null || screenshotName.isEmpty()) {
                log.info(Constants.Errors.SNAPSHOT_NAME_NULL);
                throw new IllegalArgumentException(Constants.Errors.SNAPSHOT_NAME_NULL);
            }

            TakesScreenshot takesScreenshot = (TakesScreenshot) appiumDriver;
            File screenshot = takesScreenshot.getScreenshotAs(OutputType.FILE);
            log.info("Screenshot captured: " + screenshotName);

            UploadSnapshotRequest uploadSnapshotRequest = new UploadSnapshotRequest();
            uploadSnapshotRequest.setScreenshotName(screenshotName);
            uploadSnapshotRequest.setProjectToken(projectToken);
            Dimension d = appiumDriver.manage().window().getSize();
            int w = d.getWidth(), h = d.getHeight();
            uploadSnapshotRequest.setViewport(w+"x"+h);
            log.info("Device viewport set to: "+ uploadSnapshotRequest.getViewport());
            String platform = "", deviceName="", browserName ="";
            if(options != null && options.containsKey("platform")){
                platform = options.get("platform").trim();
            }
            if(options != null && options.containsKey("deviceName")){
                deviceName = options.get("deviceName").trim();
            }
            if(deviceName == null || deviceName.isEmpty()){
                throw new IllegalArgumentException(Constants.Errors.DEVICE_NAME_NULL);
            }
            if(platform == null || platform.isEmpty()){
                if(deviceName.toLowerCase().startsWith("i")){
                    browserName =  "iOS";
                }
                else {
                    browserName = "Android";
                }
            }
            uploadSnapshotRequest.setOs(platform != null && !platform.isEmpty() ? platform : browserName);
            if(platform != null && !platform.isEmpty()){
                uploadSnapshotRequest.setDeviceName(deviceName+" "+platform);
            }
            else {
                uploadSnapshotRequest.setDeviceName(deviceName + " "+browserName);
            }

            if (platform.toLowerCase().contains("ios")) {
                uploadSnapshotRequest.setBrowserName("safari");
            } else {
                uploadSnapshotRequest.setBrowserName("chrome");
            }
            if (Objects.nonNull(buildData)) {
                uploadSnapshotRequest.setBuildId(buildData.getBuildId());
                uploadSnapshotRequest.setBuildName(buildData.getName());
            }
            UploadSnapshotResponse uploadSnapshotResponse = util.uploadScreenshot(screenshot,uploadSnapshotRequest, this.buildData);
            log.info("For uploading: " + uploadSnapshotRequest.toString() + " received response: "+ uploadSnapshotResponse.getData());
        } catch (Exception e) {
            log.severe(Constants.Errors.UPLOAD_SNAPSHOT_FAILED + " due to: " +e.getMessage());
            throw new  Exception("Couldnt upload image to Smart UI due to: " + e.getMessage());
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