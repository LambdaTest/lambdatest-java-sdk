package io.github.lambdatest;


import io.appium.java_client.AppiumDriver;
import io.github.lambdatest.constants.Constants;
import io.github.lambdatest.models.BuildConfig;
import io.github.lambdatest.models.BuildData;
import io.github.lambdatest.models.MobileConfig;
import io.github.lambdatest.models.UploadSnapshotRequest;
import io.github.lambdatest.utils.SmartUIUtil;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import com.google.gson.Gson;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class SmartUIAppSnapshot {

    private static final Logger log = Logger.getLogger("lt-smart-ui-java-sdk");
    private static final SmartUIUtil smartUIUtil = new SmartUIUtil();

    public static void smartUICapture(AppiumDriver appiumDriver, String screenshotName, Map<String, Object> options) throws Exception {

        Gson gson = new Gson();
        //Check 1 : If driver is missing
        if (Objects.isNull(appiumDriver)) {
            throw new IllegalArgumentException(Constants.Errors.SELENIUM_DRIVER_NULL);
        }

        // Fetch the device information
        String deviceName = appiumDriver.getCapabilities().getCapability("deviceName").toString();
        String platformName = appiumDriver.getCapabilities().getCapability("platformName").toString();
        String platformVersion = appiumDriver.getCapabilities().getCapability("platformVersion").toString();


        //Check 2 : if screenshot name is missing
        if (Objects.isNull(screenshotName) || screenshotName.isEmpty()) {
            throw new IllegalArgumentException(Constants.Errors.SNAPSHOT_NAME_NULL);
        }

        //Check 3 : fetch project token from options/env and validating it
        String projectToken = smartUIUtil.getProjectToken(options);

        //Check 4 : check for SmartUI Server is up or not
        if (!smartUIUtil.isSmartUIRunning()) {
            log.warning("Smart-UI server is not running ...");
            throw new IllegalStateException(Constants.Errors.SMARTUI_NOT_RUNNING);
        }
        log.info("Smart-UI server is running ...");

        //Check 5 : for users authentication and creating build
        BuildData createBuildResponse = null;
        try{
             createBuildResponse = smartUIUtil.build(projectToken);        //youre gonna need this response to get build Name and buildId for upload screenshot API
        }
        catch (Exception e){
            log.severe("Couldn't create build cos of Exception : "+ e.getMessage());
        }
        //Check 6: Capture screenshot using Appium and send to SMART-UI
        TakesScreenshot tks = (TakesScreenshot) appiumDriver;
        File screenshot = (tks.getScreenshotAs(OutputType.FILE));
        log.info("Screenshot captured: " + screenshotName);

        UploadSnapshotRequest uploadSnapshotRequest = new UploadSnapshotRequest();
        uploadSnapshotRequest.setScreenshotName(screenshotName);
        uploadSnapshotRequest.setScreenshot(screenshot);
        uploadSnapshotRequest.setProjectToken(projectToken);
        uploadSnapshotRequest.setOs(platformName+platformVersion);
        uploadSnapshotRequest.setDeviceName(deviceName);
        uploadSnapshotRequest.setResolution("all");
        if(Objects.nonNull(createBuildResponse)){
        uploadSnapshotRequest.setBuildId(createBuildResponse.getBuildId());
        uploadSnapshotRequest.setBuildName(createBuildResponse.getName());
        }
        String uploadRequest = gson.toJson(uploadSnapshotRequest);
        smartUIUtil.uploadScreenshot(uploadRequest);

        //Closure : Stop/Finalise build
        try{
        smartUIUtil.stop(Objects.requireNonNull(createBuildResponse).getBuildId());}
        catch (Exception e){
            log.severe("Couldn't stop the build due to an exception :"+ e.getMessage());
        }
    }
}