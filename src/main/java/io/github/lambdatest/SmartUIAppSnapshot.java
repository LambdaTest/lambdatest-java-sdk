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
import io.github.lambdatest.utils.ElementBoundingBoxUtil;

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
    private void validateMandatoryParams(WebDriver driver, String screenshotName, String deviceName) {
        if (driver == null) {
            log.severe(Constants.Errors.SELENIUM_DRIVER_NULL + " during take snapshot");
            throw new IllegalArgumentException(Constants.Errors.SELENIUM_DRIVER_NULL);
        }
        if (screenshotName == null || screenshotName.isEmpty()) {
            log.info(Constants.Errors.SNAPSHOT_NAME_NULL);
            throw new IllegalArgumentException(Constants.Errors.SNAPSHOT_NAME_NULL);
        }
        if (deviceName == null || deviceName.isEmpty()) {
            throw new IllegalArgumentException(Constants.Errors.DEVICE_NAME_NULL);
        }
    }

    private String getOptionValue(Map<String, String> options, String key) {
        if (options != null && options.containsKey(key)) {
            String value = options.get(key);
            return value != null ? value.trim() : "";
        }
        return "";
    }

    private List<String> extractXPathsFromOptions(Map<String, String> options) {
        List<String> xpaths = new ArrayList<>();
        
        if (options == null) {
            return xpaths;
        }
        
        // Check for ignoreBoxes option with nested structure
        String ignoreBoxesValue = getOptionValue(options, "ignoreBoxes");
        if (!ignoreBoxesValue.isEmpty()) {
            try {
                // Parse the ignoreBoxes JSON structure
                Map<String, Object> ignoreBoxesMap = gson.fromJson(ignoreBoxesValue, Map.class);
                if (ignoreBoxesMap != null && ignoreBoxesMap.containsKey("xpaths")) {
                    Object xpathsObj = ignoreBoxesMap.get("xpaths");
                    if (xpathsObj instanceof List) {
                        List<?> xpathsList = (List<?>) xpathsObj;
                        for (Object xpathObj : xpathsList) {
                            if (xpathObj instanceof String) {
                                String xpath = ((String) xpathObj).trim();
                                if (!xpath.isEmpty()) {
                                    xpaths.add(xpath);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warning("Failed to parse ignoreBoxes option: " + e.getMessage());
            }
        }
        
        return xpaths;
    }

    private UploadSnapshotRequest initializeUploadRequest(String screenshotName, String viewport) {
        UploadSnapshotRequest request = new UploadSnapshotRequest();
        request.setScreenshotName(screenshotName);
        request.setProjectToken(projectToken);
        request.setViewport(viewport);
        log.info("Viewport set to :" + viewport);
        if (Objects.nonNull(buildData)) {
            request.setBuildId(buildData.getBuildId());
            request.setBuildName(buildData.getName());
        }
        return request;
    }

    private UploadSnapshotRequest configureDeviceNameAndPlatform(UploadSnapshotRequest request, String deviceName, String platform) {
        String browserName = deviceName.toLowerCase().startsWith("i") ? "iOS" : "Android";
        String platformName = (platform == null || platform.isEmpty()) ? browserName : platform;
        request.setOs(platformName);
        request.setDeviceName(deviceName + " " + platformName);
        assert platform != null;
        request.setBrowserName(platform.toLowerCase().contains("ios") ? "safari" : "chrome");
        return request;
    }

    public void smartuiAppSnapshot(WebDriver driver, String screenshotName, Map<String, String> options)
            throws Exception {
        try {
            String deviceName = getOptionValue(options, "deviceName");
            String platform = getOptionValue(options, "platform");
            validateMandatoryParams(driver, screenshotName, deviceName);
            Dimension d = driver.manage().window().getSize();
            int width = d.getWidth(), height = d.getHeight();
            UploadSnapshotRequest initReq = initializeUploadRequest(screenshotName, width + "x" + height);
            UploadSnapshotRequest uploadSnapshotRequest = configureDeviceNameAndPlatform(initReq, deviceName, platform);
            String screenshotHash = UUID.randomUUID().toString();
            uploadSnapshotRequest.setScreenshotHash(screenshotHash);
            String uploadChunk = getOptionValue(options, "uploadChunk");
            String pageCount = getOptionValue(options, "pageCount"); int userInputtedPageCount=0;
            if(!pageCount.isEmpty()) {
                userInputtedPageCount = Integer.parseInt(pageCount);
            }
            if(!uploadChunk.isEmpty() && uploadChunk.toLowerCase().contains("true")) {
                uploadSnapshotRequest.setUploadChunk("true");
            } else {
                uploadSnapshotRequest.setUploadChunk("false");
            }
            String navBarHeight = getOptionValue(options, "navigationBarHeight");
            String statusBarHeight = getOptionValue(options, "statusBarHeight");

            if(!navBarHeight.isEmpty()) {
                uploadSnapshotRequest.setNavigationBarHeight(navBarHeight);
            }
            if(!statusBarHeight.isEmpty()) {
                uploadSnapshotRequest.setStatusBarHeight(statusBarHeight);
            }
            String cropFooter = getOptionValue(options, "cropFooter");
            if (!cropFooter.isEmpty()) {
                uploadSnapshotRequest.setCropFooter(cropFooter.toLowerCase());
            }
            String cropStatusBar = getOptionValue(options, "cropStatusBar");
            if (!cropStatusBar.isEmpty()) {
                uploadSnapshotRequest.setCropStatusBar(cropStatusBar.toLowerCase());
            }

            String fullPage = getOptionValue(options, "fullPage").toLowerCase();
            if(!Boolean.parseBoolean(fullPage)){
                if(!pageCount.isEmpty()){
                    throw new IllegalArgumentException(Constants.Errors.PAGE_COUNT_ERROR);
                }
                TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
                File screenshot = takesScreenshot.getScreenshotAs(OutputType.FILE);
                log.info("Screenshot captured: " + screenshotName);
                uploadSnapshotRequest.setFullPage("false");
                util.uploadScreenshot(screenshot, uploadSnapshotRequest, this.buildData);

            } else {
                uploadSnapshotRequest.setFullPage("true");
                FullPageScreenshotUtil fullPageCapture = new FullPageScreenshotUtil(driver, screenshotName);
                
                // Extract XPaths from options for element detection
                List<String> xpaths = extractXPathsFromOptions(options);
                
                List<File> ssDir;
                if (xpaths != null && !xpaths.isEmpty()) {
                    log.info("Element detection enabled for " + xpaths.size() + " XPaths");
                    ssDir = fullPageCapture.captureFullPage(userInputtedPageCount, xpaths);
                } else {
                    ssDir = fullPageCapture.captureFullPage(userInputtedPageCount);
                }
                
                if(ssDir.isEmpty()){
                    throw new RuntimeException(Constants.Errors.SMARTUI_SNAPSHOT_FAILED);
                }
                int pageCountInSsDir = ssDir.size(); int i;
                if(pageCountInSsDir == 1) {         //when page count is set to 1 as user for fullPage
                    uploadSnapshotRequest.setFullPage("false");
                    util.uploadScreenshot(ssDir.get(0), uploadSnapshotRequest, this.buildData);
                    return;
                }
                for( i = 0; i < pageCountInSsDir -1; ++i){
                    uploadSnapshotRequest.setIsLastChunk("false");
                    uploadSnapshotRequest.setChunkCount(i);
                    util.uploadScreenshot(ssDir.get(i), uploadSnapshotRequest, this.buildData);
                }
                uploadSnapshotRequest.setIsLastChunk("true");
                uploadSnapshotRequest.setChunkCount(i);
                util.uploadScreenshot(ssDir.get(pageCountInSsDir-1), uploadSnapshotRequest, this.buildData);
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