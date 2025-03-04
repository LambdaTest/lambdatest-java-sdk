package io.github.lambdatest;


import io.appium.java_client.AppiumDriver;
import io.github.lambdatest.constants.Constants;

import io.github.lambdatest.models.BuildData;
import io.github.lambdatest.models.UploadSnapshotRequest;
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
    private final SmartUIUtil smartUIUtil;
    private final Gson gson;

    public SmartUIAppSnapshot() {
        this.log = Logger.getLogger("lt-smart-ui-java-sdk");
        this.smartUIUtil = new SmartUIUtil();
        this.gson = new Gson();
    }

    public void smartUIAppSnapshot(AppiumDriver appiumDriver, String screenshotName, Map<String, Object> options) throws Exception {
        if (Objects.isNull(appiumDriver)) {
            throw new IllegalArgumentException(Constants.Errors.SELENIUM_DRIVER_NULL);
        }

        String deviceName = appiumDriver.getCapabilities().getCapability("deviceName").toString();
        String platformName = appiumDriver.getCapabilities().getCapability("platformName").toString();
        String platformVersion = appiumDriver.getCapabilities().getCapability("platformVersion").toString();

        if (Objects.isNull(screenshotName) || screenshotName.isEmpty()) {
            throw new IllegalArgumentException(Constants.Errors.SNAPSHOT_NAME_NULL);
        }

        String projectToken = smartUIUtil.getProjectToken(options);

        if (!smartUIUtil.isSmartUIRunning()) {
            log.warning("Smart-UI server is not running ...");
            throw new IllegalStateException(Constants.Errors.SMARTUI_NOT_RUNNING);
        }
        log.info("Smart-UI server is running ...");

        BuildData createBuildResponse = null;
        try {
             createBuildResponse = smartUIUtil.build(projectToken);
        } catch (Exception e) {
            log.severe("Couldn't create build due to Exception: " + e.getMessage());
        }

        TakesScreenshot takesScreenshot = (TakesScreenshot) appiumDriver;
        File screenshot = takesScreenshot.getScreenshotAs(OutputType.FILE);
        log.info("Screenshot captured: " + screenshotName);

        UploadSnapshotRequest uploadSnapshotRequest = new UploadSnapshotRequest();
        uploadSnapshotRequest.setScreenshotName(screenshotName);
        uploadSnapshotRequest.setScreenshot(screenshot);
        uploadSnapshotRequest.setProjectToken(projectToken);
        uploadSnapshotRequest.setOs(platformName + platformVersion);
        uploadSnapshotRequest.setDeviceName(deviceName);
//      Need to check :    uploadSnapshotRequest.setResolution("all");

        if (Objects.nonNull(createBuildResponse)) {
            uploadSnapshotRequest.setBuildId(createBuildResponse.getBuildId());
            uploadSnapshotRequest.setBuildName(createBuildResponse.getName());
        }

        String uploadRequest = gson.toJson(uploadSnapshotRequest);
        log.info("Adding upload for request :"+ uploadRequest);

        try{
        smartUIUtil.uploadScreenshot(uploadRequest);
        } catch (Exception e){
            log.severe("Error in uploading screenshot :"+ e.getMessage());
        }

        try {
            smartUIUtil.stop(Objects.requireNonNull(createBuildResponse).getBuildId());
            log.info("Session ended for token: "+projectToken);
        } catch (Exception e) {
            log.severe("Couldn't stop the build due to an exception: " + e.getMessage());
        }
    }
}