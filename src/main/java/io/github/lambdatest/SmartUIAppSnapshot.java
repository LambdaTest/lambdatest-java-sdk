package io.github.lambdatest;

import com.google.gson.Gson;
import io.github.lambdatest.constants.Constants;
import io.github.lambdatest.models.*;
import io.github.lambdatest.utils.FullPageScreenshotUtil;
import io.github.lambdatest.utils.GitUtils;
import io.github.lambdatest.utils.SmartUIUtil;
import org.openqa.selenium.*;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import io.github.lambdatest.utils.LoggerUtil;


public class SmartUIAppSnapshot {
    private final Logger log = LoggerUtil.createLogger("lambdatest-java-app-sdk");
    private final SmartUIUtil util;
    private final Gson gson = new Gson();
    private String projectToken;

    private BuildData buildData;

    public SmartUIAppSnapshot() {
        this.util = new SmartUIUtil();
    }

    public SmartUIAppSnapshot(String proxyHost, int proxyPort) throws Exception {
        this.util = new SmartUIUtil(proxyHost, proxyPort);
    }

    public SmartUIAppSnapshot(String proxyHost, int proxyPort, boolean allowInsecure) throws Exception {
        this.util = new SmartUIUtil(proxyHost, proxyPort, allowInsecure);
    }

    public SmartUIAppSnapshot(String proxyProtocol, String proxyHost, int proxyPort, boolean allowInsecure)
            throws Exception {
        this.util = new SmartUIUtil(proxyProtocol, proxyHost, proxyPort, allowInsecure);
    }

    public void start(Map<String, String> options) throws Exception {
        try {
            this.projectToken = getProjectToken(options);
            log.info("Project token set as: " + this.projectToken);
        } catch (Exception e) {
            log.severe(Constants.Errors.PROJECT_TOKEN_UNSET);
            throw new Exception("Project token is a mandatory field", e);
        }

        try {
            Map<String, String> envVars = new HashMap<>(System.getenv());
            GitInfo git = GitUtils.getGitInfo(envVars);
            BuildResponse buildRes = util.build(git, this.projectToken, options);
            this.buildData = buildRes.getData();
            log.info("Build ID set : " + this.buildData.getBuildId() + "for Build name : " + this.buildData.getName());
            options.put("buildName", this.buildData.getName());
        } catch (Exception e) {
            log.severe("Couldn't create smartui build: " + e.getMessage());
            throw new Exception("Couldn't create smartui build: " + e.getMessage());
        }
    }

    public void start() throws Exception {
        this.start(new HashMap<>());
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

    private String getOptionValue(Map<String, String> options, String key) {
        if (options != null && options.containsKey(key)) {
            String value = options.get(key);
            return value != null ? value.trim() : "";
        }
        return "";
    }

    public void smartuiAppSnapshot(WebDriver driver, String screenshotName, Map<String, String> options)
            throws Exception {
        try {
            if (driver == null) {
                log.severe(Constants.Errors.SELENIUM_DRIVER_NULL + " during take snapshot");
                throw new IllegalArgumentException(Constants.Errors.SELENIUM_DRIVER_NULL);
            }
            if (screenshotName == null || screenshotName.isEmpty()) {
                log.info(Constants.Errors.SNAPSHOT_NAME_NULL);
                throw new IllegalArgumentException(Constants.Errors.SNAPSHOT_NAME_NULL);
            }
            UUID screenshotHash = UUID.randomUUID();
            String ssHash = screenshotHash.toString();

            UploadSnapshotRequest uploadSnapshotRequest = new UploadSnapshotRequest();
            uploadSnapshotRequest.setScreenshotName(screenshotName);
            uploadSnapshotRequest.setProjectToken(projectToken);
            if (Objects.nonNull(buildData)) {
                uploadSnapshotRequest.setBuildId(buildData.getBuildId());
                uploadSnapshotRequest.setBuildName(buildData.getName());
            }

            Dimension d = driver.manage().window().getSize();
            int width = d.getWidth(), height = d.getHeight();
            uploadSnapshotRequest.setViewport(width + "x" + height);
            log.info("Device viewport set to: " + uploadSnapshotRequest.getViewport());

            String platform = "", browserName = "";
            String deviceName = getOptionValue(options, "deviceName");
            String fullpage = options.get("fullPage").trim().toLowerCase(); //ToDo : parseBoolean + null check
            if(deviceName.isEmpty()) {
                throw new IllegalArgumentException(Constants.Errors.DEVICE_NAME_NULL);
            }
            platform = getOptionValue(options, "platform");
            if(platform.isEmpty()){
                browserName = deviceName.toLowerCase().startsWith("i") ? "iOS" : "Android";
            }
            uploadSnapshotRequest.setOs(platform != null && !platform.isEmpty() ? platform : browserName);
            if (platform != null && !platform.isEmpty()) {
                uploadSnapshotRequest.setDeviceName(deviceName + " " + platform);
            } else {
                uploadSnapshotRequest.setDeviceName(deviceName + " " + browserName);
            }
            if (uploadSnapshotRequest.getOs().toLowerCase().contains("ios")) {
                uploadSnapshotRequest.setBrowserName("safari");
            } else {
                uploadSnapshotRequest.setBrowserName("chrome");
            }
            if(options != null && options.containsKey("cropFooter")) {
                uploadSnapshotRequest.setCropFooter(options.get("cropFooter").trim().toLowerCase());
            }
            if(options != null && options.containsKey("cropStatusBar")) {
                uploadSnapshotRequest.setCropStatusBar(options.get("cropStatusBar").trim().toLowerCase());
            }

            if(options != null && (!options.containsKey("fullPage") || !Boolean.parseBoolean(fullpage))){
                TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
                File screenshot = takesScreenshot.getScreenshotAs(OutputType.FILE);
                log.info("Screenshot captured: " + screenshotName);
                util.uploadScreenshot(screenshot, uploadSnapshotRequest, this.buildData);
            }
            if(options != null && Boolean.parseBoolean(fullpage)) {
                uploadSnapshotRequest.setFullPage(fullpage);
                FullPageScreenshotUtil fullPageScreenshotUtil = new FullPageScreenshotUtil(driver, ssHash);
                List<File> ssDir = fullPageScreenshotUtil.captureFullPage();
                int chunkCount = ssDir.size();
                for( int i = 0; i < chunkCount; ++i){
                    uploadSnapshotRequest.setIsLastChunk("false");
                    uploadSnapshotRequest.setScreenshotHash(ssHash);
                    uploadSnapshotRequest.setChunkCount(i);
                    util.uploadScreenshot(ssDir.get(i), uploadSnapshotRequest, this.buildData);
                    if(i == chunkCount-1){
                        log.info("Last chunk received!");
                        uploadSnapshotRequest.setIsLastChunk("true");
                        uploadSnapshotRequest.setScreenshotHash(ssHash);
                        uploadSnapshotRequest.setChunkCount(i);
                        util.uploadScreenshot(ssDir.get(i), uploadSnapshotRequest, this.buildData);
                    }
                }
            }
        } catch (Exception e) {
            log.severe(Constants.Errors.UPLOAD_SNAPSHOT_FAILED + " due to: " + e.getMessage());
            throw new Exception("Couldnt upload image to Smart UI due to: " + e.getMessage());
        }
    }

    public void stop() throws Exception {
        try {
            if (this.buildData != null) {
                log.info("Stopping session for buildId: " + this.buildData.getBuildId());
                if (Objects.nonNull(this.buildData.getBuildId())) {
                    util.stopBuild(this.buildData.getBuildId(), projectToken);
                    log.info("Session ended for token: " + projectToken);
                } else {
                    log.info("Build ID not found to stop build for " + projectToken);
                }
            }
        } catch (Exception e) {
            log.severe("Couldn't stop the build due to an exception: " + e.getMessage());
            throw new Exception(Constants.Errors.STOP_BUILD_FAILED + " due to : " + e.getMessage());
        }
    }
}