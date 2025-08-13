package io.github.lambdatest;

import com.google.gson.Gson;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lambdatest.constants.Constants;
import io.github.lambdatest.models.*;
import io.github.lambdatest.utils.FullPageScreenshotUtil;
import io.github.lambdatest.utils.GitUtils;
import io.github.lambdatest.utils.SmartUIUtil;
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
            log.info("Build ID set : " + this.buildData.getBuildId() + "for build name : " + this.buildData.getName());
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

    private Map<String, Object> extractSelectorsFromOptions(Map<String, String> options) {
        Map<String, Object> selectorData = new HashMap<>();
        
        if (options == null) {
            return selectorData;
        }

        String ignoreBoxesValue = getOptionValue(options, "ignoreBoxes");
        String selectBoxesValue = getOptionValue(options, "selectBoxes");

        if (!ignoreBoxesValue.isEmpty()) {
            try {
                Map<String, Object> ignoreBoxesMap = gson.fromJson(ignoreBoxesValue, Map.class);
                Map<String, List<String>> ignoreSelectors = parseSelectors(ignoreBoxesMap);
                selectorData.put("ignore", ignoreSelectors);
            } catch (Exception e) {
                log.warning("Failed to parse ignoreBoxes option: " + e.getMessage());
            }
        }

        if (!selectBoxesValue.isEmpty()) {
            try {
                Map<String, Object> selectBoxesMap = gson.fromJson(selectBoxesValue, Map.class);
                Map<String, List<String>> selectSelectors = parseSelectors(selectBoxesMap);
                selectorData.put("select", selectSelectors);
            } catch (Exception e) {
                log.warning("Failed to parse selectBoxes option: " + e.getMessage());
            }
        }

        return selectorData;
    }

    /**
     * Parse selectors from a selector map object
     */
    private Map<String, List<String>> parseSelectors(Object selectorsObj) {
        Map<String, List<String>> selectors = new HashMap<>();
        
        if (selectorsObj instanceof Map) {
            Map<?, ?> selectorsMap = (Map<?, ?>) selectorsObj;
            
            for (Map.Entry<?, ?> entry : selectorsMap.entrySet()) {
                String selectorType = entry.getKey().toString();
                Object selectorValuesObj = entry.getValue();
                
                if (selectorValuesObj instanceof List) {
                    List<?> selectorValuesList = (List<?>) selectorValuesObj;
                    List<String> selectorValues = new ArrayList<>();
                    
                    for (int i = 0; i < selectorValuesList.size(); i++) {
                        Object selectorValueObj = selectorValuesList.get(i);
                        
                        if (selectorValueObj instanceof String) {
                            String selectorValue = ((String) selectorValueObj).trim();
                            if (!selectorValue.isEmpty()) {
                                selectorValues.add(selectorValue);
                            }
                        } else {
                            log.warning("Non-string " + selectorType + " selector found at index " + i + ": " + selectorValueObj.getClass().getSimpleName() + ", skipping");
                        }
                    }
                    
                    if (!selectorValues.isEmpty()) {
                        selectors.put(selectorType, selectorValues);
                        log.info("Added " + selectorType + " selectors: " + selectorValues);
                    }
                } else {
                    log.warning(selectorType + " value is not a List: " + (selectorValuesObj != null ? selectorValuesObj.getClass().getSimpleName() : "null"));
                }
            }
        } else {
            log.warning("Selectors object is not a Map: " + (selectorsObj != null ? selectorsObj.getClass().getSimpleName() : "null"));
        }
        
        return selectors;
    }

    private UploadSnapshotRequest initializeUploadRequest(String screenshotName, String viewport) {
        UploadSnapshotRequest request = new UploadSnapshotRequest();
        request.setScreenshotName(screenshotName);
        request.setProjectToken(projectToken);
        request.setViewport(viewport);
        log.info("Viewport set to: " + viewport);
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

    /**
     * Create bounding box data for elements and set it on the upload request
     */
    private void setBoundingBoxes(UploadSnapshotRequest uploadSnapshotRequest, 
                                   List<ElementBoundingBox> ignoredElements, 
                                   List<ElementBoundingBox> selectedElements) {
        if (!ignoredElements.isEmpty()) {
            List<Map<String, Integer>> ignoreBoxes = new ArrayList<>();
            for (ElementBoundingBox element : ignoredElements) {
                Map<String, Integer> box = new HashMap<>();
                box.put("left", element.getX());
                box.put("top", element.getY());
                box.put("right", element.getX() + element.getWidth());
                box.put("bottom", element.getY() + element.getHeight());
                ignoreBoxes.add(box);
            }

            Map<String, Object> ignoreBoxesData = new HashMap<>();
            ignoreBoxesData.put("boxes", ignoreBoxes);
            
            String ignoreBoxesJson = gson.toJson(ignoreBoxesData);
            uploadSnapshotRequest.setIgnoreBoxes(ignoreBoxesJson);
        }

        if (!selectedElements.isEmpty()) {
            List<Map<String, Integer>> selectBoxes = new ArrayList<>();
            for (ElementBoundingBox element : selectedElements) {
                Map<String, Integer> box = new HashMap<>();
                box.put("left", element.getX());
                box.put("top", element.getY());
                box.put("right", element.getX() + element.getWidth());
                box.put("bottom", element.getY() + element.getHeight());
                selectBoxes.add(box);
            }

            Map<String, Object> selectBoxesData = new HashMap<>();
            selectBoxesData.put("boxes", selectBoxes);
            
            String selectBoxesJson = gson.toJson(selectBoxesData);
            uploadSnapshotRequest.setSelectBoxes(selectBoxesJson);
        }
    }

    /**
     * Process and set various options on the upload request
     */
    private int processUploadOptions(UploadSnapshotRequest uploadSnapshotRequest, Map<String, String> options) {
        String uploadChunk = getOptionValue(options, "uploadChunk");
        String pageCount = getOptionValue(options, "pageCount");
        int userInputtedPageCount = 0;
        if (!pageCount.isEmpty()) {
            userInputtedPageCount = Integer.parseInt(pageCount);
        }
        if (!uploadChunk.isEmpty() && uploadChunk.toLowerCase().contains("true")) {
            uploadSnapshotRequest.setUploadChunk("true");
        } else {
            uploadSnapshotRequest.setUploadChunk("false");
        }
        
        String navBarHeight = getOptionValue(options, "navigationBarHeight");
        String statusBarHeight = getOptionValue(options, "statusBarHeight");

        if (!navBarHeight.isEmpty()) {
            uploadSnapshotRequest.setNavigationBarHeight(navBarHeight);
        }
        if (!statusBarHeight.isEmpty()) {
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
        
        return userInputtedPageCount;
    }

    /**
     * Handle fullPage screenshot capture and upload
     */
    private void handleFullPageScreenshot(WebDriver driver, String screenshotName, String testType, 
                                         UploadSnapshotRequest uploadSnapshotRequest, 
                                         int userInputtedPageCount, Map<String, String> options) throws Exception {
        FullPageScreenshotUtil fullPageCapture = new FullPageScreenshotUtil(driver, screenshotName, testType);

        Map<String, Object> selectorData = extractSelectorsFromOptions(options);
        
        // Extract selectors from options
        Map<String, List<String>> ignoreSelectors = selectorData.containsKey("ignore") ? 
            (Map<String, List<String>>) selectorData.get("ignore") : null;
        Map<String, List<String>> selectSelectors = selectorData.containsKey("select") ? 
            (Map<String, List<String>>) selectorData.get("select") : null;

        // Capture full page screenshots with selectors (if any)
        Map<String, Object> result = fullPageCapture.captureFullPageScreenshot(
            userInputtedPageCount, ignoreSelectors, selectSelectors);
        
        List<File> ssDir = (List<File>) result.get("screenshots");
        
        // Process bounding boxes if selectors were provided
        if (ignoreSelectors != null || selectSelectors != null) {
            List<ElementBoundingBox> ignoredElements = (List<ElementBoundingBox>) result.get("ignoreElements");
            List<ElementBoundingBox> selectedElements = (List<ElementBoundingBox>) result.get("selectElements");
            setBoundingBoxes(uploadSnapshotRequest, ignoredElements, selectedElements);
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

    /**
     * Handle single screenshot capture and upload
     */
    private void handleSingleScreenshot(WebDriver driver, String screenshotName, 
                                       UploadSnapshotRequest uploadSnapshotRequest, 
                                       Map<String, String> options) throws Exception {
        String pageCount = getOptionValue(options, "pageCount");
        if(!pageCount.isEmpty()){
            throw new IllegalArgumentException(Constants.Errors.PAGE_COUNT_ERROR);
        }
        TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
        File screenshot = takesScreenshot.getScreenshotAs(OutputType.FILE);
        log.info("Screenshot captured: " + screenshotName);
        uploadSnapshotRequest.setFullPage("false");
        logUploadRequest(uploadSnapshotRequest);
        util.uploadScreenshot(screenshot, uploadSnapshotRequest, this.buildData);
    }

    public void smartuiAppSnapshot(WebDriver driver, String screenshotName, Map<String, String> options)
            throws Exception {
        try {
            String deviceName = getOptionValue(options, "deviceName");
            String platform = getOptionValue(options, "platform");
            String testType = getOptionValue(options, "testType");
            testType = testType.isEmpty() ? "app" : testType;
            validateMandatoryParams(driver, screenshotName, deviceName);
            Dimension d = driver.manage().window().getSize();
            int width = d.getWidth(), height = d.getHeight();
            UploadSnapshotRequest initReq = initializeUploadRequest(screenshotName, width + "x" + height);
            UploadSnapshotRequest uploadSnapshotRequest = configureDeviceNameAndPlatform(initReq, deviceName, platform);
            String screenshotHash = UUID.randomUUID().toString();
            uploadSnapshotRequest.setScreenshotHash(screenshotHash);
            
            int userInputtedPageCount = processUploadOptions(uploadSnapshotRequest, options);

            String fullPage = getOptionValue(options, "fullPage").toLowerCase();
            if(!Boolean.parseBoolean(fullPage)){
                handleSingleScreenshot(driver, screenshotName, uploadSnapshotRequest, options);
            } else {
                uploadSnapshotRequest.setFullPage("true");
                handleFullPageScreenshot(driver, screenshotName, testType, uploadSnapshotRequest, userInputtedPageCount, options);
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