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
    private String projectToken;
    private BuildData buildData;

    public SmartUIAppSnapshot() {
        this.log = Logger.getLogger("lt-smart-ui-java-sdk");
        this.smartUIUtil = new SmartUIUtil();
        this.gson = new Gson();
    }

    public void start(AppiumDriver appiumDriver, String screenshotName, Map<String, Object> options) {
        if (Objects.isNull(appiumDriver)) {
            throw new IllegalArgumentException(Constants.Errors.SELENIUM_DRIVER_NULL);
        }

        if (Objects.isNull(screenshotName) || screenshotName.isEmpty()) {
            throw new IllegalArgumentException(Constants.Errors.SNAPSHOT_NAME_NULL);
        }

        this.projectToken = smartUIUtil.getProjectToken(options);
        boolean isServerUp = smartUIUtil.isSmartUIRunning();
        if (!isServerUp) {
            log.severe("Error in authenticating user with projectToken: " + projectToken);
            throw new IllegalArgumentException(Constants.Errors.SMARTUI_NOT_RUNNING);
        }
        //Authenticate user for projectToken and create build for User's session
        try {
            this.buildData = smartUIUtil.build(projectToken, appiumDriver);
            log.info("Created build, received response :" + this.buildData);
        } catch (Exception e) {
            log.severe("Couldn't create build due to Exception: " + e.getMessage());
            throw new IllegalStateException("Couldn't create build due to error: " + e.getMessage());
        }
    }

    public void smartuiAppSnapshot(AppiumDriver appiumDriver, String screenshotName) {
        try {
            String deviceName = appiumDriver.getCapabilities().getCapability("deviceName").toString();
            String platformName = appiumDriver.getCapabilities().getCapability("platformName").toString();
            String platformVersion = appiumDriver.getCapabilities().getCapability("platformVersion").toString();
            String os = platformName + platformVersion;

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
            smartUIUtil.uploadScreenshot(uploadRequest);
            log.info("Uploaded screenshot for request: " + uploadRequest);
        } catch (Exception e) {
            log.severe("Error in uploading screenshot: " + e.getMessage());
            throw new RuntimeException("Error capturing or uploading screenshot: " + e.getMessage());
        }
    }

    public void stop() {
        try {
            if (this.buildData != null) {
                log.info("Stopping session for buildId: " + buildData.getBuildId());
                smartUIUtil.stop(buildData.getBuildId());
                log.info("Session ended for token: " + projectToken);
            }
        } catch (Exception e) {
            log.severe("Couldn't stop the build due to an exception: " + e.getMessage());
            throw new RuntimeException("Exception occurred in stopping build: " + e.getMessage());
        }
    }
}