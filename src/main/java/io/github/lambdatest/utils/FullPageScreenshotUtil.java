package io.github.lambdatest.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

public class FullPageScreenshotUtil {
    private static final int DEFAULT_MAX_COUNT = 10;
    private static final int SCROLL_DELAY_MS = 200;
    private static final int WEB_SCROLL_PAUSE_MS = 1000;
    private static final int IOS_SCROLL_DURATION_MS = 1500;
    private static final int ANDROID_SCROLL_SPEED = 300;
    private static final int PAGE_SOURCE_CHECK_DELAY_MS = 1000;

    // Scroll percentages
    private static final double ANDROID_SCROLL_END_PERCENT = 0.3;
    private static final double ANDROID_SCROLL_HEIGHT_PERCENT = 0.35;
    private static final double IOS_SCROLL_HEIGHT_PERCENT = 0.3;
    private static final double IOS_START_Y_PERCENT = 0.7;
    private static final double IOS_END_Y_PERCENT = 0.4;
    private static final double WEB_SCROLL_HEIGHT_PERCENT = 0.4;

    private final WebDriver driver;
    private final String saveDirectoryName;
    private final Logger log = LoggerUtil.createLogger("lambdatest-java-app-sdk");
    private final String platform;
    private final String testType;
    private final String deviceName;
    private String prevPageSource = "";
    private int maxCount = DEFAULT_MAX_COUNT;

    public FullPageScreenshotUtil(WebDriver driver, String saveDirectoryName, String testType) {
        this.driver = driver;
        this.saveDirectoryName = saveDirectoryName;
        this.testType = testType;
        this.platform = detectPlatform();
        this.deviceName = detectDeviceName();

        log.info("FullPageScreenshotUtil initialized for testType: " + testType + ", platform: " + platform + ", deviceName: " + deviceName);
        createDirectoryIfNeeded();
    }

    public Map<String, Object> captureFullPageScreenshot(int pageCount, Map<String, List<String>> ignoreSelectors, Map<String, List<String>> selectSelectors) {
        initializePageCount(pageCount);

        List<File> screenshotDir = new ArrayList<>();
        List<ElementBoundingBox> ignoreElements = new ArrayList<>();
        List<ElementBoundingBox> selectElements = new ArrayList<>();

        boolean hasSelectors = hasAnySelectors(ignoreSelectors, selectSelectors);
        ElementBoundingBoxUtil elementUtil = hasSelectors ? new ElementBoundingBoxUtil(driver, testType, deviceName) : null;

        boolean captureAllElementsAtTheStart = shouldCaptureAllElementsAtStart(hasSelectors);

        if (captureAllElementsAtTheStart) {
            captureElementsAtStart(ignoreSelectors, selectSelectors, ignoreElements, selectElements, elementUtil);
        }

        processScreenshotsAndElements(screenshotDir, ignoreElements, selectElements, hasSelectors, elementUtil, captureAllElementsAtTheStart, ignoreSelectors, selectSelectors);

        if (hasSelectors && hasAnyElements(ignoreElements, selectElements)) {
            adjustElementHeights(ignoreElements, selectElements, elementUtil);
        }

        return createResult(screenshotDir, ignoreElements, selectElements);
    }

    private void initializePageCount(int pageCount) {
        if (pageCount <= 0) {
            pageCount = DEFAULT_MAX_COUNT;
        }
        if (pageCount < maxCount) {
            maxCount = pageCount;
        }
    }

    private boolean hasAnySelectors(Map<String, List<String>> ignoreSelectors, Map<String, List<String>> selectSelectors) {
        return isValidSelectorMap(ignoreSelectors) || isValidSelectorMap(selectSelectors);
    }

    private boolean isValidSelectorMap(Map<String, List<String>> selectors) {
        return selectors != null && !selectors.isEmpty();
    }

    private boolean hasAnyElements(List<ElementBoundingBox> ignoreElements, List<ElementBoundingBox> selectElements) {
        return !ignoreElements.isEmpty() || !selectElements.isEmpty();
    }

    private boolean shouldCaptureAllElementsAtStart(boolean hasSelectors) {
        return hasSelectors && (platform.equals("ios") || testType.equalsIgnoreCase("web"));
    }

    private void captureElementsAtStart(Map<String, List<String>> ignoreSelectors, Map<String, List<String>> selectSelectors,
                                        List<ElementBoundingBox> ignoreElements, List<ElementBoundingBox> selectElements,
                                        ElementBoundingBoxUtil elementUtil) {
        if (isValidSelectorMap(ignoreSelectors)) {
            List<ElementBoundingBox> captured = elementUtil.detectAllElementsAtTheStart(ignoreSelectors, "ignore");
            if (captured != null) {
                ignoreElements.addAll(captured);
            }
        }

        if (isValidSelectorMap(selectSelectors)) {
            List<ElementBoundingBox> captured = elementUtil.detectAllElementsAtTheStart(selectSelectors, "select");
            if (captured != null) {
                selectElements.addAll(captured);
            }
        }
    }

    private void processScreenshotsAndElements(List<File> screenshotDir, List<ElementBoundingBox> ignoreElements,
                                               List<ElementBoundingBox> selectElements, boolean hasSelectors,
                                               ElementBoundingBoxUtil elementUtil, boolean captureAllElementsAtTheStart,
                                               Map<String, List<String>> ignoreSelectors, Map<String, List<String>> selectSelectors) {
        int chunkCount = 0;
        boolean isLastScroll = false;

        while (!isLastScroll && chunkCount < maxCount) {
            File screenshotFile = captureAndSaveScreenshot(chunkCount);
            screenshotDir.add(screenshotFile);

            if (hasSelectors && !captureAllElementsAtTheStart) {
                detectElementsInChunk(ignoreSelectors, selectSelectors, ignoreElements, selectElements, elementUtil, chunkCount);
            }

            chunkCount++;
            int scrollDistance = scrollDown();

            if (hasSelectors && !captureAllElementsAtTheStart) {
                elementUtil.updateScrollPosition(scrollDistance);
            }

            isLastScroll = hasReachedBottom();
        }
    }

    private void detectElementsInChunk(Map<String, List<String>> ignoreSelectors, Map<String, List<String>> selectSelectors,
                                       List<ElementBoundingBox> ignoreElements, List<ElementBoundingBox> selectElements,
                                       ElementBoundingBoxUtil elementUtil, int chunkCount) {
        if (isValidSelectorMap(ignoreSelectors)) {
            List<ElementBoundingBox> chunkIgnoreElements = elementUtil.detectElements(ignoreSelectors, chunkCount, "ignore");
            ignoreElements.addAll(chunkIgnoreElements);
        }

        if (isValidSelectorMap(selectSelectors)) {
            List<ElementBoundingBox> chunkSelectElements = elementUtil.detectElements(selectSelectors, chunkCount, "select");
            selectElements.addAll(chunkSelectElements);
        }
    }

    private Map<String, Object> createResult(List<File> screenshotDir, List<ElementBoundingBox> ignoreElements, List<ElementBoundingBox> selectElements) {
        Map<String, Object> result = new HashMap<>();
        result.put("screenshots", screenshotDir);
        result.put("ignoreElements", ignoreElements);
        result.put("selectElements", selectElements);
        return result;
    }

    private void createDirectoryIfNeeded() {
        File dir = new File(saveDirectoryName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void adjustElementYCoordinates(List<ElementBoundingBox> elements, int heightToAdd) {
        if (elements.isEmpty() || heightToAdd <= 0) {
            return;
        }

        List<ElementBoundingBox> adjustedElements = new ArrayList<>();
        for (ElementBoundingBox element : elements) {
            ElementBoundingBox adjustedElement = new ElementBoundingBox(
                    element.getSelectorKey(),
                    element.getX(),
                    element.getY() + heightToAdd,
                    element.getWidth(),
                    element.getHeight(),
                    element.getChunkIndex(),
                    element.getPlatform(),
                    element.getPurpose()
            );
            adjustedElements.add(adjustedElement);
        }

        elements.clear();
        elements.addAll(adjustedElements);
    }

    private int calculateHeightAdjustment(ElementBoundingBoxUtil elementUtil) {
        if (isWebTestOnIOS()) {
            return elementUtil.getStatusBarHeightInDevicePixels();
        } else if (isWebTestOnAndroid()) {
            return elementUtil.getAndroidChromeBarHeightInDevicePixels();
        }
        return 0;
    }

    private boolean isWebTestOnIOS() {
        return testType.toLowerCase().contains("web") &&
                (deviceName.toLowerCase().contains("iphone") ||
                        deviceName.toLowerCase().contains("ipad") ||
                        deviceName.toLowerCase().contains("ipod"));
    }

    private boolean isWebTestOnAndroid() {
        return testType.toLowerCase().contains("web") &&
                (deviceName.toLowerCase().contains("android") ||
                        platform.toLowerCase().contains("android"));
    }

    private void adjustElementHeights(List<ElementBoundingBox> ignoreElements, List<ElementBoundingBox> selectElements, ElementBoundingBoxUtil elementUtil) {
        int heightToAdd = calculateHeightAdjustment(elementUtil);
        if (heightToAdd > 0) {
            adjustElementYCoordinates(ignoreElements, heightToAdd);
            adjustElementYCoordinates(selectElements, heightToAdd);
        }
    }

    private File captureAndSaveScreenshot(int index) {
        File destinationFile = new File(saveDirectoryName + "/" + saveDirectoryName + "_" + index + ".png");
        try {
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshotFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved screenshot: " + destinationFile.getAbsolutePath());
        } catch (IOException e) {
            log.warning("Error saving screenshot: " + e.getMessage());
        }
        return destinationFile;
    }

    private int scrollDown() {
        try {
            Thread.sleep(SCROLL_DELAY_MS);
            return scrollOnDevice();
        } catch (Exception e) {
            log.warning("Error during scroll operation: " + e.getMessage());
            return 0;
        }
    }

    private int scrollOnDevice() {
        try {
            if (testType.equalsIgnoreCase("app")) {
                return platform.equals("ios") ? scrollIOS() : scrollAndroid();
            }
            return scrollWeb();
        } catch (Exception e) {
            log.severe("Error in scrollOnDevice: " + e.getMessage());
            return 0;
        }
    }

    private int scrollAndroid() {
        try {
            Dimension size = driver.manage().window().getSize();
            int screenHeight = size.getHeight();
            int screenWidth = size.getWidth();

            int scrollEndY = (int) (screenHeight * ANDROID_SCROLL_END_PERCENT);
            int scrollHeight = (int) (screenHeight * ANDROID_SCROLL_HEIGHT_PERCENT);
            int scrollLeft = screenWidth / 2;

            Map<String, Object> scrollParams = createAndroidScrollParams(scrollLeft, scrollEndY, scrollHeight);
            ((JavascriptExecutor) driver).executeScript("mobile:scrollGesture", scrollParams);

            return scrollHeight;
        } catch (Exception e) {
            log.severe("Android scroll failed: " + e.getMessage());
            return 0;
        }
    }

    private Map<String, Object> createAndroidScrollParams(int scrollLeft, int scrollEndY, int scrollHeight) {
        Map<String, Object> scrollParams = new HashMap<>();
        scrollParams.put("left", scrollLeft);
        scrollParams.put("top", scrollEndY);
        scrollParams.put("width", 3);
        scrollParams.put("height", scrollHeight);
        scrollParams.put("direction", "down");
        scrollParams.put("percent", 1.0);
        scrollParams.put("speed", ANDROID_SCROLL_SPEED);
        return scrollParams;
    }

    private int scrollIOS() {
        try {
            Dimension size = driver.manage().window().getSize();
            int screenHeight = size.getHeight();
            int screenWidth = size.getWidth();
            int scrollHeight = (int) (screenHeight * IOS_SCROLL_HEIGHT_PERCENT);

            Sequence dragSequence = createIOSScrollSequence(screenHeight, screenWidth);
            ((RemoteWebDriver) driver).perform(Arrays.asList(dragSequence));

            return scrollHeight;
        } catch (Exception e) {
            log.severe("iOS scroll failed: " + e.getMessage());
            return 0;
        }
    }

    private Sequence createIOSScrollSequence(int screenHeight, int screenWidth) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence dragSequence = new Sequence(finger, 1);

        int startY = (int) (screenHeight * IOS_START_Y_PERCENT);
        int endY = (int) (screenHeight * IOS_END_Y_PERCENT);
        int x = screenWidth / 2;

        dragSequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
        dragSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        dragSequence.addAction(finger.createPointerMove(Duration.ofMillis(IOS_SCROLL_DURATION_MS), PointerInput.Origin.viewport(), x, endY));
        dragSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        return dragSequence;
    }

    private int scrollWeb() {
        try {
            Dimension size = driver.manage().window().getSize();
            int scrollHeight = (int) (size.getHeight() * WEB_SCROLL_HEIGHT_PERCENT);

            ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, arguments[0]);", scrollHeight);

            pauseForWebScroll();
            return scrollHeight;
        } catch (Exception e) {
            log.severe("Web JavaScript scroll failed: " + e.getMessage());
            return 0;
        }
    }

    private void pauseForWebScroll() {
        try {
            Thread.sleep(WEB_SCROLL_PAUSE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warning("Web scroll pause interrupted: " + e.getMessage());
        }
    }

    private String detectPlatform() {
        try {
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            String platformName = getPlatformName(caps);

            if (platformName.contains("ios")) {
                return "ios";
            } else if (platformName.contains("android")) {
                return "android";
            } else {
                return "web";
            }
        } catch (Exception e) {
            log.warning("Failed to detect platform: " + e.getMessage());
            return "web";
        }
    }

    private String getPlatformName(Capabilities caps) {
        if (caps.getCapability("platformName") != null) {
            return caps.getCapability("platformName").toString().toLowerCase();
        } else if (caps.getCapability("platform") != null) {
            return caps.getCapability("platform").toString().toLowerCase();
        } else {
            log.warning("No platform capability found");
            return "";
        }
    }

    private String detectDeviceName() {
        try {
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            return getDeviceNameFromCapabilities(caps);
        } catch (Exception e) {
            log.warning("Failed to detect device name: " + e.getMessage());
            return platform;
        }
    }

    private String getDeviceNameFromCapabilities(Capabilities caps) {
        String[] deviceCapabilityKeys = {"deviceName", "device", "deviceModel", "deviceType"};

        for (String key : deviceCapabilityKeys) {
            if (caps.getCapability(key) != null) {
                return caps.getCapability(key).toString();
            }
        }

        log.info("No device name capability found, using platform as device identifier");
        return platform;
    }

    private boolean hasReachedBottom() {
        try {
            Thread.sleep(PAGE_SOURCE_CHECK_DELAY_MS);
            
            if (testType.equalsIgnoreCase("web")) {
                return hasReachedBottomWeb();
            } else {
                return hasReachedBottomMobile();
            }
        } catch (Exception e) {
            log.warning("Error checking if reached bottom: " + e.getMessage());
            return true;
        }
    }

    private boolean hasReachedBottomWeb() {
        try {
            Long currentScrollY = (Long) ((JavascriptExecutor) driver).executeScript(
                "return window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0;"
            );
            
            Long pageHeight = (Long) ((JavascriptExecutor) driver).executeScript(
                "return Math.max(" +
                "document.body.scrollHeight, " +
                "document.body.offsetHeight, " +
                "document.documentElement.clientHeight, " +
                "document.documentElement.scrollHeight, " +
                "document.documentElement.offsetHeight);"
            );
            
            Long viewportHeight = (Long) ((JavascriptExecutor) driver).executeScript(
                "return window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;"
            );
            
            boolean isAtBottom = (currentScrollY + viewportHeight) >= pageHeight;
            
            if (isAtBottom) {
                log.info("Reached bottom of web page - scroll position: " + currentScrollY + ", page height: " + pageHeight);
            }
            
            return isAtBottom;
        } catch (Exception e) {
            log.warning("Error checking web page bottom: " + e.getMessage());
            return true;
        }
    }

    private boolean hasReachedBottomMobile() {
        try {
            String currentPageSource = driver.getPageSource();

            if (currentPageSource == null) {
                log.warning("Page source is null");
                return false;
            }

            if (currentPageSource.equals(prevPageSource)) {
                log.info("Same page content detected — reached the bottom of the page.");
                return true;
            } else {
                prevPageSource = currentPageSource;
                return false;
            }
        } catch (Exception e) {
            log.warning("Error checking mobile page bottom: " + e.getMessage());
            return true;
        }
    }
}