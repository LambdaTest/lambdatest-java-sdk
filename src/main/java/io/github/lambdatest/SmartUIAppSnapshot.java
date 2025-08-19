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
    // Constants
    private static final String DEFAULT_TEST_TYPE = "app";
    private static final String OPTION_DEVICE_NAME = "deviceName";
    private static final String OPTION_PLATFORM = "platform";
    private static final String OPTION_TEST_TYPE = "testType";
    private static final String OPTION_FULL_PAGE = "fullPage";
    private static final String OPTION_PRECISE_SCROLL = "preciseScroll";
    private static final String OPTION_UPLOAD_CHUNK = "uploadChunk";
    private static final String OPTION_PAGE_COUNT = "pageCount";
    private static final String OPTION_NAVIGATION_BAR_HEIGHT = "navigationBarHeight";
    private static final String OPTION_STATUS_BAR_HEIGHT = "statusBarHeight";
    private static final String OPTION_CROP_FOOTER = "cropFooter";
    private static final String OPTION_CROP_STATUS_BAR = "cropStatusBar";
    private static final String OPTION_IGNORE_BOXES = "ignoreBoxes";
    private static final String OPTION_SELECT_BOXES = "selectBoxes";

    private static final String BROWSER_IOS = "safari";
    private static final String BROWSER_ANDROID = "chrome";
    private static final String PLATFORM_IOS = "iOS";
    private static final String PLATFORM_ANDROID = "Android";

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

    public SmartUIAppSnapshot(String proxyProtocol, String proxyHost, int proxyPort, boolean allowInsecure) throws Exception {
        this.util = new SmartUIUtil(proxyProtocol, proxyHost, proxyPort, allowInsecure);
    }

    public void start(Map<String, String> options) throws Exception {
        initializeProjectToken(options);
        createBuild(options);
    }

    public void start() throws Exception {
        start(new HashMap<>());
    }

    private void initializeProjectToken(Map<String, String> options) throws Exception {
        try {
            this.projectToken = getProjectToken(options);
            log.info("Project token set as: " + this.projectToken);
        } catch (Exception e) {
            log.severe(Constants.Errors.PROJECT_TOKEN_UNSET);
            throw new Exception("Project token is a mandatory field", e);
        }
    }

    private void createBuild(Map<String, String> options) throws Exception {
        try {
            Map<String, String> envVars = new HashMap<>(System.getenv());
            GitInfo git = GitUtils.getGitInfo(envVars);
            BuildResponse buildRes = util.build(git, this.projectToken, options);
            this.buildData = buildRes.getData();
            log.info("Build ID set: " + this.buildData.getBuildId() + " for build name: " + this.buildData.getName());
            options.put("buildName", this.buildData.getName());
        } catch (Exception e) {
            log.severe("Couldn't create smartui build: " + e.getMessage());
            throw new Exception("Couldn't create smartui build: " + e.getMessage());
        }
    }

    private String getProjectToken(Map<String, String> options) {
        // Check options first
        if (isValidOptionValue(options, Constants.PROJECT_TOKEN)) {
            return options.get(Constants.PROJECT_TOKEN).trim();
        }

        // Check environment variable
        String envToken = System.getenv("PROJECT_TOKEN");
        if (envToken != null && !envToken.trim().isEmpty()) {
            return envToken.trim();
        }

        throw new IllegalArgumentException(Constants.Errors.PROJECT_TOKEN_UNSET);
    }

    private boolean isValidOptionValue(Map<String, String> options, String key) {
        return options != null && options.containsKey(key) &&
                options.get(key) != null && !options.get(key).trim().isEmpty();
    }

    public void smartuiAppSnapshot(WebDriver driver, String screenshotName, Map<String, String> options) throws Exception {
        try {
            SnapshotConfig config = parseSnapshotConfig(options);
            validateMandatoryParams(driver, screenshotName, config.deviceName);

            UploadSnapshotRequest uploadRequest = createUploadRequest(driver, screenshotName, config);
            processScreenshotCapture(driver, screenshotName, config, uploadRequest, options);

        } catch (Exception e) {
            log.severe(Constants.Errors.UPLOAD_SNAPSHOT_FAILED + " due to: " + e.getMessage());
            throw new Exception("Couldn't upload image to Smart UI due to: " + e.getMessage());
        }
    }

    private SnapshotConfig parseSnapshotConfig(Map<String, String> options) {
        String testType = getOptionValue(options, OPTION_TEST_TYPE, DEFAULT_TEST_TYPE);
        if(!(testType.equals("app") || testType.equals("web"))) {
            testType = DEFAULT_TEST_TYPE;
        }

        return new SnapshotConfig(
                getOptionValue(options, OPTION_DEVICE_NAME),
                getOptionValue(options, OPTION_PLATFORM),
                testType,
                parseIntOption(options, OPTION_PAGE_COUNT, 0),
                parseBooleanOption(options, OPTION_FULL_PAGE, false),
                parseBooleanOption(options, OPTION_PRECISE_SCROLL, false)
        );
    }

    private UploadSnapshotRequest createUploadRequest(WebDriver driver, String screenshotName, SnapshotConfig config) {
        Dimension viewport = driver.manage().window().getSize();
        String viewportString = viewport.getWidth() + "x" + viewport.getHeight();

        UploadSnapshotRequest request = initializeUploadRequest(screenshotName, viewportString);
        configureDeviceAndPlatform(request, config.deviceName, config.platform);
        request.setScreenshotHash(UUID.randomUUID().toString());

        return request;
    }

    private void processScreenshotCapture(WebDriver driver, String screenshotName, SnapshotConfig config,
                                          UploadSnapshotRequest uploadRequest, Map<String, String> options) throws Exception {
        processUploadOptions(uploadRequest, options);

        int pageCount = config.fullPage ? config.pageCount : 1;
        if (config.fullPage) {
            uploadRequest.setFullPage("true");
        }

        handleFullPageScreenshot(driver, screenshotName, config, uploadRequest, pageCount, options);
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
        return getOptionValue(options, key, "");
    }

    private String getOptionValue(Map<String, String> options, String key, String defaultValue) {
        if (options != null && options.containsKey(key)) {
            String value = options.get(key);
            return value != null ? value.trim() : defaultValue;
        }
        return defaultValue;
    }

    private int parseIntOption(Map<String, String> options, String key, int defaultValue) {
        String value = getOptionValue(options, key);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warning("Invalid integer value for " + key + ": " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    private boolean parseBooleanOption(Map<String, String> options, String key, boolean defaultValue) {
        String value = getOptionValue(options, key);
        return value.isEmpty() ? defaultValue : Boolean.parseBoolean(value.toLowerCase());
    }

    private SelectorData extractSelectorsFromOptions(Map<String, String> options) {
        if (options == null) {
            return new SelectorData(null, null);
        }

        Map<String, List<String>> ignoreSelectors = parseSelectorsFromOption(options, OPTION_IGNORE_BOXES);
        Map<String, List<String>> selectSelectors = parseSelectorsFromOption(options, OPTION_SELECT_BOXES);

        if (ignoreSelectors != null && selectSelectors != null) {
            log.warning("Cannot use both ignore and select boxes in one screenshot, proceeding with just ignore selectors");
        }

        return new SelectorData(ignoreSelectors, selectSelectors);
    }

    private Map<String, List<String>> parseSelectorsFromOption(Map<String, String> options, String optionKey) {
        String selectorValue = getOptionValue(options, optionKey);
        if (selectorValue.isEmpty()) {
            return null;
        }

        try {
            Map<String, Object> selectorMap = gson.fromJson(selectorValue, Map.class);
            return parseSelectors(selectorMap);
        } catch (Exception e) {
            log.warning("Failed to parse " + optionKey + " option: " + e.getMessage());
            return null;
        }
    }

    private Map<String, List<String>> parseSelectors(Object selectorsObj) {
        Map<String, List<String>> selectors = new HashMap<>();

        if (!(selectorsObj instanceof Map)) {
            log.warning("Selectors object is not a Map: " + getClassName(selectorsObj));
            return selectors;
        }

        Map<?, ?> selectorsMap = (Map<?, ?>) selectorsObj;

        for (Map.Entry<?, ?> entry : selectorsMap.entrySet()) {
            String selectorType = entry.getKey().toString();
            List<String> selectorValues = parseSelectorValues(selectorType, entry.getValue());

            if (!selectorValues.isEmpty()) {
                selectors.put(selectorType, selectorValues);
                log.info("Added " + selectorType + " selectors: " + selectorValues);
            }
        }

        return selectors;
    }

    private List<String> parseSelectorValues(String selectorType, Object selectorValuesObj) {
        List<String> selectorValues = new ArrayList<>();

        if (!(selectorValuesObj instanceof List)) {
            log.warning(selectorType + " value is not a List: " + getClassName(selectorValuesObj));
            return selectorValues;
        }

        List<?> selectorValuesList = (List<?>) selectorValuesObj;

        for (int i = 0; i < selectorValuesList.size(); i++) {
            Object selectorValueObj = selectorValuesList.get(i);

            if (selectorValueObj instanceof String) {
                String selectorValue = ((String) selectorValueObj).trim();
                if (!selectorValue.isEmpty()) {
                    selectorValues.add(selectorValue);
                }
            } else {
                log.warning("Non-string " + selectorType + " selector found at index " + i + ": " +
                        getClassName(selectorValueObj) + ", skipping");
            }
        }

        return selectorValues;
    }

    private String getClassName(Object obj) {
        return obj != null ? obj.getClass().getSimpleName() : "null";
    }

    private UploadSnapshotRequest initializeUploadRequest(String screenshotName, String viewport) {
        UploadSnapshotRequest request = new UploadSnapshotRequest();
        request.setScreenshotName(screenshotName);
        request.setProjectToken(projectToken);
        request.setViewport(viewport);
        log.info("Viewport set to: " + viewport);

        if (buildData != null) {
            request.setBuildId(buildData.getBuildId());
            request.setBuildName(buildData.getName());
        }

        return request;
    }

    private void configureDeviceAndPlatform(UploadSnapshotRequest request, String deviceName, String platform) {
        String browserName = determineDefaultBrowser(deviceName);
        String platformName = determinePlatformName(platform, browserName);

        request.setOs(platformName);
        request.setDeviceName(deviceName + " " + platformName);
        request.setBrowserName(determineActualBrowser(platformName));
    }

    private String determineDefaultBrowser(String deviceName) {
        return deviceName.toLowerCase().startsWith("i") ? PLATFORM_IOS : PLATFORM_ANDROID;
    }

    private String determinePlatformName(String platform, String defaultBrowser) {
        return (platform == null || platform.isEmpty()) ? defaultBrowser : platform;
    }

    private String determineActualBrowser(String platformName) {
        return platformName.toLowerCase().contains("ios") ? BROWSER_IOS : BROWSER_ANDROID;
    }

    private void processUploadOptions(UploadSnapshotRequest uploadRequest, Map<String, String> options) {
        setUploadChunkOption(uploadRequest, options);
        setNavigationOptions(uploadRequest, options);
        setCropOptions(uploadRequest, options);
    }

    private void setUploadChunkOption(UploadSnapshotRequest uploadRequest, Map<String, String> options) {
        boolean uploadChunk = parseBooleanOption(options, OPTION_UPLOAD_CHUNK, false);
        uploadRequest.setUploadChunk(String.valueOf(uploadChunk));
    }

    private void setNavigationOptions(UploadSnapshotRequest uploadRequest, Map<String, String> options) {
        String navBarHeight = getOptionValue(options, OPTION_NAVIGATION_BAR_HEIGHT);
        String statusBarHeight = getOptionValue(options, OPTION_STATUS_BAR_HEIGHT);

        if (!navBarHeight.isEmpty()) {
            uploadRequest.setNavigationBarHeight(navBarHeight);
        }
        if (!statusBarHeight.isEmpty()) {
            uploadRequest.setStatusBarHeight(statusBarHeight);
        }
    }

    // TODO: Enable status bar and footer cropping, disabling it because the current version of selenium
    // doesn't have the required APIs to get status bar height
    private void setCropOptions(UploadSnapshotRequest uploadRequest, Map<String, String> options) {

        //        String cropFooter = getOptionValue(options, OPTION_CROP_FOOTER);
//        String cropStatusBar = getOptionValue(options, OPTION_CROP_STATUS_BAR);
//
//        if (!cropFooter.isEmpty()) {
//            uploadRequest.setCropFooter(cropFooter.toLowerCase());
//        }
//        if (!cropStatusBar.isEmpty()) {
//            uploadRequest.setCropStatusBar(cropStatusBar.toLowerCase());
//        }

        uploadRequest.setCropFooter("false");
        uploadRequest.setCropStatusBar("false");
    }

    private void handleFullPageScreenshot(WebDriver driver, String screenshotName, SnapshotConfig config,
                                          UploadSnapshotRequest uploadRequest, int pageCount,
                                          Map<String, String> options) throws Exception {
        FullPageScreenshotUtil fullPageCapture = new FullPageScreenshotUtil(driver, screenshotName, config.testType, config.preciseScroll);
        SelectorData selectorData = extractSelectorsFromOptions(options);

        Map<String, Object> result = fullPageCapture.captureFullPageScreenshot(
                pageCount, selectorData.ignoreSelectors, selectorData.selectSelectors);

        List<File> screenshots = getScreenshotsFromResult(result);
        validateScreenshots(screenshots);

        if (hasSelectors(selectorData)) {
            setBoundingBoxesFromResult(uploadRequest, result);
        }

        uploadScreenshots(screenshots, uploadRequest);
    }

    @SuppressWarnings("unchecked")
    private List<File> getScreenshotsFromResult(Map<String, Object> result) {
        return (List<File>) result.get("screenshots");
    }

    private void validateScreenshots(List<File> screenshots) {
        if (screenshots.isEmpty()) {
            throw new RuntimeException(Constants.Errors.SMARTUI_SNAPSHOT_FAILED);
        }
    }

    private boolean hasSelectors(SelectorData selectorData) {
        return selectorData.ignoreSelectors != null || selectorData.selectSelectors != null;
    }

    @SuppressWarnings("unchecked")
    private void setBoundingBoxesFromResult(UploadSnapshotRequest uploadRequest, Map<String, Object> result) {
        List<ElementBoundingBox> ignoredElements = (List<ElementBoundingBox>) result.get("ignoreElements");
        List<ElementBoundingBox> selectedElements = (List<ElementBoundingBox>) result.get("selectElements");
        setBoundingBoxes(uploadRequest, ignoredElements, selectedElements);
    }

    private void uploadScreenshots(List<File> screenshots, UploadSnapshotRequest uploadRequest) throws Exception {
        if (screenshots.size() == 1) {
            uploadSingleScreenshot(screenshots.get(0), uploadRequest);
        } else {
            uploadMultipleScreenshots(screenshots, uploadRequest);
        }
    }

    private void uploadSingleScreenshot(File screenshot, UploadSnapshotRequest uploadRequest) throws Exception {
        uploadRequest.setFullPage("false");
        util.uploadScreenshot(screenshot, uploadRequest, buildData);
    }

    private void uploadMultipleScreenshots(List<File> screenshots, UploadSnapshotRequest uploadRequest) throws Exception {
        int totalScreenshots = screenshots.size();

        // Upload all but last screenshot
        for (int i = 0; i < totalScreenshots - 1; i++) {
            uploadRequest.setIsLastChunk("false");
            uploadRequest.setChunkCount(i);
            util.uploadScreenshot(screenshots.get(i), uploadRequest, buildData);
        }

        // Upload last screenshot
        uploadRequest.setIsLastChunk("true");
        uploadRequest.setChunkCount(totalScreenshots - 1);
        util.uploadScreenshot(screenshots.get(totalScreenshots - 1), uploadRequest, buildData);
    }

    private void setBoundingBoxes(UploadSnapshotRequest uploadRequest,
                                  List<ElementBoundingBox> ignoredElements,
                                  List<ElementBoundingBox> selectedElements) {
        setBoundingBoxes(uploadRequest, ignoredElements, true);
        setBoundingBoxes(uploadRequest, selectedElements, false);
    }

    private void setBoundingBoxes(UploadSnapshotRequest uploadRequest,
                                  List<ElementBoundingBox> elements,
                                  boolean isIgnoreBoxes) {
        if (elements == null || elements.isEmpty()) {
            return;
        }

        List<Map<String, Integer>> boxes = createBoundingBoxMaps(elements);
        Map<String, Object> boxesData = Map.of("boxes", boxes);
        String boxesJson = gson.toJson(boxesData);

        if (isIgnoreBoxes) {
            uploadRequest.setIgnoreBoxes(boxesJson);
        } else {
            uploadRequest.setSelectBoxes(boxesJson);
        }
    }

    private List<Map<String, Integer>> createBoundingBoxMaps(List<ElementBoundingBox> elements) {
        List<Map<String, Integer>> boxes = new ArrayList<>();

        for (ElementBoundingBox element : elements) {
            Map<String, Integer> box = new HashMap<>();
            box.put("left", element.getX());
            box.put("top", element.getY());
            box.put("right", element.getX() + element.getWidth());
            box.put("bottom", element.getY() + element.getHeight());
            boxes.add(box);
        }

        return boxes;
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

    public void stop() throws Exception {
        try {
            if (buildData != null && buildData.getBuildId() != null) {
                log.info("Stopping session for buildId: " + buildData.getBuildId());
                util.stopBuild(buildData.getBuildId(), projectToken);
                log.info("Session ended for token: " + projectToken);
            } else {
                log.info("Build ID not found to stop build for " + projectToken);
            }
        } catch (Exception e) {
            log.severe("Couldn't stop the build due to an exception: " + e.getMessage());
            throw new Exception(Constants.Errors.STOP_BUILD_FAILED + " due to : " + e.getMessage());
        }
    }

    private static class SnapshotConfig {
        final String deviceName;
        final String platform;
        final String testType;
        final int pageCount;
        final boolean fullPage;
        final boolean preciseScroll;

        SnapshotConfig(String deviceName, String platform, String testType, int pageCount, boolean fullPage, boolean preciseScroll) {
            this.deviceName = deviceName;
            this.platform = platform;
            this.testType = testType;
            this.pageCount = pageCount;
            this.fullPage = fullPage;
            this.preciseScroll = preciseScroll;
        }
    }

    private static class SelectorData {
        final Map<String, List<String>> ignoreSelectors;
        final Map<String, List<String>> selectSelectors;

        SelectorData(Map<String, List<String>> ignoreSelectors, Map<String, List<String>> selectSelectors) {
            this.ignoreSelectors = ignoreSelectors;
            this.selectSelectors = selectSelectors;
        }
    }
}