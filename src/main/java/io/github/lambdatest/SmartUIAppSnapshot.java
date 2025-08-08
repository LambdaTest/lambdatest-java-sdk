package io.github.lambdatest;

import com.google.gson.Gson;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lambdatest.constants.Constants;
import io.github.lambdatest.models.*;
import io.github.lambdatest.utils.FullPageScreenshotUtil;
import io.github.lambdatest.utils.GitUtils;
import io.github.lambdatest.utils.SmartUIUtil;
import io.github.lambdatest.utils.ElementBoundingBoxUtil;
import io.github.lambdatest.utils.ElementBoundingBox;
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
        
        log.info("Extracting XPaths from options map");
        
        if (options == null) {
            log.info("Options map is null, returning empty XPath list");
            return xpaths;
        }
        
        // Check for ignoreBoxes option with nested structure
        String ignoreBoxesValue = getOptionValue(options, "ignoreBoxes");
        log.info("ignoreBoxes value from options: " + (ignoreBoxesValue.isEmpty() ? "empty" : "present"));
        
        if (!ignoreBoxesValue.isEmpty()) {
            try {
                log.info("Parsing ignoreBoxes JSON structure");
                // Parse the ignoreBoxes JSON structure
                Map<String, Object> ignoreBoxesMap = gson.fromJson(ignoreBoxesValue, Map.class);
                
                if (ignoreBoxesMap != null) {
                    log.info("Successfully parsed ignoreBoxes map with " + ignoreBoxesMap.size() + " keys");
                    
                    if (ignoreBoxesMap.containsKey("xpaths")) {
                        log.info("Found xpaths key in ignoreBoxes map");
                        Object xpathsObj = ignoreBoxesMap.get("xpaths");
                        
                        if (xpathsObj instanceof List) {
                            List<?> xpathsList = (List<?>) xpathsObj;
                            log.info("XPaths list contains " + xpathsList.size() + " items");
                            
                            for (int i = 0; i < xpathsList.size(); i++) {
                                Object xpathObj = xpathsList.get(i);
                                log.info("Processing XPath item " + (i + 1) + "/" + xpathsList.size() + ": " + xpathObj);
                                
                                if (xpathObj instanceof String) {
                                    String xpath = ((String) xpathObj).trim();
                                    if (!xpath.isEmpty()) {
                                        xpaths.add(xpath);
                                        log.info("Added XPath: " + xpath);
                                    } else {
                                        log.warning("Empty XPath found at index " + i + ", skipping");
                                    }
                                } else {
                                    log.warning("Non-string XPath found at index " + i + ": " + xpathObj.getClass().getSimpleName() + ", skipping");
                                }
                            }
                        } else {
                            log.warning("xpaths value is not a List: " + (xpathsObj != null ? xpathsObj.getClass().getSimpleName() : "null"));
                        }
                    } else {
                        log.info("No xpaths key found in ignoreBoxes map. Available keys: " + ignoreBoxesMap.keySet());
                    }
                } else {
                    log.warning("Failed to parse ignoreBoxes map - result is null");
                }
            } catch (Exception e) {
                log.warning("Failed to parse ignoreBoxes option: " + e.getMessage());
                log.warning("Exception type: " + e.getClass().getSimpleName());
            }
        } else {
            log.info("No ignoreBoxes option found in options map");
        }
        
        log.info("Extracted " + xpaths.size() + " XPaths from options");
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

    private void logUploadRequest(UploadSnapshotRequest request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(request);
            log.info("Final UploadSnapshotRequest: " + json);
        } catch (Exception e) {
            log.warning("Failed to serialize UploadSnapshotRequest for logging: " + e.getMessage());
        }
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
                logUploadRequest(uploadSnapshotRequest);
                util.uploadScreenshot(screenshot, uploadSnapshotRequest, this.buildData);

            } else {
                uploadSnapshotRequest.setFullPage("true");
                
                FullPageScreenshotUtil fullPageCapture = new FullPageScreenshotUtil(driver, screenshotName);
                
                // Extract XPaths from options for element detection
                List<String> xpaths = extractXPathsFromOptions(options);
                
                List<File> ssDir;
                List<ElementBoundingBox> allDetectedElements = new ArrayList<>();
                
                if (xpaths != null && !xpaths.isEmpty()) {
                    log.info("Element detection enabled for " + xpaths.size() + " XPaths");
                    ssDir = fullPageCapture.captureFullPage(userInputtedPageCount, xpaths);
                    
                    // Collect all detected elements for bounding box data
                    ElementBoundingBoxUtil elementUtil = new ElementBoundingBoxUtil(driver);
                    for (int chunkIndex = 0; chunkIndex < ssDir.size(); chunkIndex++) {
                        List<ElementBoundingBox> chunkElements = elementUtil.detectElements(xpaths, chunkIndex);
                        allDetectedElements.addAll(chunkElements);
                    }
                    
                    // Create bounding box data in the format {left, top, right, bottom}
                    if (!allDetectedElements.isEmpty()) {
                        List<Map<String, Integer>> boundingBoxes = new ArrayList<>();
                        for (ElementBoundingBox element : allDetectedElements) {
                            Map<String, Integer> box = new HashMap<>();
                            box.put("left", element.getX());
                            box.put("top", element.getY());
                            box.put("right", element.getX() + element.getWidth());
                            box.put("bottom", element.getY() + element.getHeight());
                            boundingBoxes.add(box);
                        }
                        
                        // Create ignoreBoxes structure with bounding boxes
                        Map<String, Object> ignoreBoxesData = new HashMap<>();
                        ignoreBoxesData.put("boxes", boundingBoxes);
                        
                        String ignoreBoxesJson = gson.toJson(ignoreBoxesData);
                        uploadSnapshotRequest.setIgnoreBoxes(ignoreBoxesJson);
                        log.info("ignoreBoxes set with bounding boxes: " + ignoreBoxesJson);
                    }
                } else {
                    ssDir = fullPageCapture.captureFullPage(userInputtedPageCount);
                }
                
                if(ssDir.isEmpty()){
                    throw new RuntimeException(Constants.Errors.SMARTUI_SNAPSHOT_FAILED);
                }
                int pageCountInSsDir = ssDir.size(); int i;
                if(pageCountInSsDir == 1) {         //when page count is set to 1 as user for fullPage
                    uploadSnapshotRequest.setFullPage("false");
                    logUploadRequest(uploadSnapshotRequest);
                    util.uploadScreenshot(ssDir.get(0), uploadSnapshotRequest, this.buildData);
                    return;
                }
                for( i = 0; i < pageCountInSsDir -1; ++i){
                    uploadSnapshotRequest.setIsLastChunk("false");
                    uploadSnapshotRequest.setChunkCount(i);
                    logUploadRequest(uploadSnapshotRequest);
                    util.uploadScreenshot(ssDir.get(i), uploadSnapshotRequest, this.buildData);
                }
                uploadSnapshotRequest.setIsLastChunk("true");
                uploadSnapshotRequest.setChunkCount(i);
                logUploadRequest(uploadSnapshotRequest);
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