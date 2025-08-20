package io.github.lambdatest.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.interactions.PointerInput.Origin;

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
    private static final int ANDROID_SCROLL_SPEED = 1500;
    private static final int PAGE_SOURCE_CHECK_DELAY_MS = 100;

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
    private final boolean preciseScroll;

    public FullPageScreenshotUtil(WebDriver driver, String saveDirectoryName, String testType, boolean preciseScroll) {
        this.driver = driver;
        this.saveDirectoryName = saveDirectoryName;
        this.testType = testType;
        this.platform = detectPlatform();
        this.deviceName = detectDeviceName();
        this.preciseScroll = preciseScroll;

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
            int scrollDistance = scrollDown(hasSelectors);

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

    private int scrollDown(boolean hasSelectors) {
        try {
            Thread.sleep(SCROLL_DELAY_MS);
            if (testType.equalsIgnoreCase("app")) {
                return platform.equals("ios") ? scrollIOS() : scrollAndroid(hasSelectors);
            }
            return scrollWeb();
        } catch (Exception e) {
            log.severe("Error in scrollDown: " + e.getMessage());
            return 0;
        }
    }

    private int scrollAndroid(boolean hasSelectors) {
        Dimension size = driver.manage().window().getSize();
        double screenHeight = size.getHeight();
        double screenWidth = size.getWidth();

        double scrollHeight = screenHeight * ANDROID_SCROLL_HEIGHT_PERCENT;
        double startX = screenWidth / 2.0;
        double startY = screenHeight * ANDROID_SCROLL_END_PERCENT;
        double endY = startY - scrollHeight;

        int preciseStartX = (int) Math.round(startX);
        int preciseStartY = (int) Math.round(startY);
        int preciseEndY = (int) Math.round(endY);
        int expectedScrollHeight = preciseStartY - preciseEndY;

        WebElement trackingElement = null;
        Point beforePosition = null;

        if (hasSelectors && this.preciseScroll) {
            trackingElement = findAutoTrackingElement(size);
            if (trackingElement != null) {
                beforePosition = trackingElement.getLocation();
            }
        }

        try {
            long optimalDuration = Math.max(ANDROID_SCROLL_SPEED, expectedScrollHeight * 3);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence scroll = new Sequence(finger, 1);

            scroll.addAction(new Pause(finger, Duration.ofMillis(50)));
            scroll.addAction(finger.createPointerMove(Duration.ZERO, Origin.viewport(), preciseStartX, preciseStartY));
            scroll.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            scroll.addAction(new Pause(finger, Duration.ofMillis(50)));
            scroll.addAction(finger.createPointerMove(Duration.ofMillis(optimalDuration), Origin.viewport(), preciseStartX, preciseEndY));
            scroll.addAction(new Pause(finger, Duration.ofMillis(50)));
            scroll.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            scroll.addAction(new Pause(finger, Duration.ofMillis(50)));

            ((RemoteWebDriver) driver).perform(Arrays.asList(scroll));
            Thread.sleep(200);
            return calculateActualScrollDistance(trackingElement, beforePosition, expectedScrollHeight);

        } catch (Exception e) {
            log.warning("Primary Android scroll failed: " + e.getMessage());
        }

        if (tryTouchSwipeAndroid()) {
            return finishScrollAndCalculateDistance(trackingElement, beforePosition, expectedScrollHeight);
        }

        if (tryDragFromToAndroid()) {
            return finishScrollAndCalculateDistance(trackingElement, beforePosition, expectedScrollHeight);
        }

        if (tryJavaScriptScrollAndroid()) {
            return finishScrollAndCalculateDistance(trackingElement, beforePosition, expectedScrollHeight);
        }

        log.severe("All Android scroll methods failed");
        return 0;
    }

    private WebElement findAutoTrackingElement(Dimension screenSize) {
        try {
            int screenHeight = screenSize.height;
            int lowerPortionStart = (int) (screenHeight * 0.6);
            int lowerPortionEnd = (int) (screenHeight * 0.9);

            // Get all elements once
            List<WebElement> allElements = driver.findElements(By.xpath("//*[@displayed='true']"));

            for (WebElement element : allElements) {
                try {
                    Rectangle rect = element.getRect();

                    if (rect.y >= lowerPortionStart &&
                        rect.y <= lowerPortionEnd &&
                        rect.width > 50 &&
                        rect.height > 20) {

                        return element;
                    }

                } catch (Exception e) {
                    // Skip problematic elements, continue search
                }
            }

            return null;

        } catch (Exception e) {
            log.warning("Auto-tracking element search failed: " + e.getMessage());
            return null;
        }
    }

    private int scrollIOS() {
        Dimension size = driver.manage().window().getSize();
        int scrollHeight = (int) (size.getHeight() * IOS_SCROLL_HEIGHT_PERCENT);
        int centerX = size.getWidth() / 2;
        int startY = (int) (size.getHeight() * IOS_START_Y_PERCENT);
        int endY = (int) (size.getHeight() * IOS_SCROLL_HEIGHT_PERCENT);

        try {
            Sequence dragSequence = createIOSScrollSequence(size.getHeight(), size.getWidth());
            ((RemoteWebDriver) driver).perform(Arrays.asList(dragSequence));
            return scrollHeight;
        } catch (Exception e) {
            log.warning("Primary iOS scroll failed: " + e.getMessage());
        }

        if (tryTouchSwipe()) {
            return finishScroll(scrollHeight, "mobile:touch:swipe");
        }

        if (tryDragFromTo(centerX, startY, endY)) {
            return finishScroll(scrollHeight, "mobile:dragFromToForDuration");
        }

        if (tryJavaScriptScroll(scrollHeight)) {
            return finishScroll(scrollHeight, "JavaScript scroll");
        }

        log.severe("All iOS scroll methods failed");
        return 0;
    }

    private boolean tryTouchSwipe() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("start", "50%,70%");
            params.put("end", "50%,40%");
            params.put("duration", String.valueOf(2));
            ((JavascriptExecutor) driver).executeScript("mobile:touch:swipe", params);
            return true;
        } catch (Exception e) {
            log.info("touch:swipe failed: " + e.getMessage());
            return false;
        }
    }

    private boolean tryDragFromTo(int centerX, int startY, int endY) {
        try {
            Map<String, Object> swipeObj = new HashMap<>();
            swipeObj.put("fromX", centerX);
            swipeObj.put("fromY", startY);
            swipeObj.put("toX", centerX);
            swipeObj.put("toY", endY);
            swipeObj.put("duration", (double) 2);
            ((JavascriptExecutor) driver).executeScript("mobile:dragFromToForDuration", swipeObj);
            return true;
        } catch (Exception e) {
            log.info("dragFromToForDuration failed: " + e.getMessage());
            return false;
        }
    }

    private boolean tryJavaScriptScroll(int scrollHeight) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "window.scrollTo({top: window.pageYOffset + arguments[0], behavior: 'smooth'});",
                    scrollHeight
            );
            return true;
        } catch (Exception e) {
            log.info("JavaScript scroll failed: " + e.getMessage());
            return false;
        }
    }

    private boolean tryTouchSwipeAndroid() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("start", "50%,70%");
            params.put("end", "50%,30%");
            params.put("duration", String.valueOf(2));
            ((JavascriptExecutor) driver).executeScript("mobile:touch:swipe", params);
            return true;
        } catch (Exception e) {
            log.info("Android touch:swipe failed: " + e.getMessage());
            return false;
        }
    }

    private boolean tryDragFromToAndroid() {
        try {
            Dimension size = driver.manage().window().getSize();
            int centerX = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * ANDROID_SCROLL_END_PERCENT);
            int endY = (int) (size.getHeight() * ANDROID_SCROLL_HEIGHT_PERCENT);
            
            Map<String, Object> swipeObj = new HashMap<>();
            swipeObj.put("fromX", centerX);
            swipeObj.put("fromY", startY);
            swipeObj.put("toX", centerX);
            swipeObj.put("toY", endY);
            swipeObj.put("duration", (double) 2);
            ((JavascriptExecutor) driver).executeScript("mobile:dragFromToForDuration", swipeObj);
            return true;
        } catch (Exception e) {
            log.info("Android dragFromToForDuration failed: " + e.getMessage());
            return false;
        }
    }

    private boolean tryJavaScriptScrollAndroid() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "window.scrollTo({top: window.pageYOffset + arguments[0], behavior: 'smooth'});",
                    (int) (driver.manage().window().getSize().getHeight() * ANDROID_SCROLL_HEIGHT_PERCENT)
            );
            return true;
        } catch (Exception e) {
            log.info("Android JavaScript scroll failed: " + e.getMessage());
            return false;
        }
    }

    private int calculateActualScrollDistance(WebElement trackingElement, Point beforePosition, int expectedScrollHeight) {
        if (trackingElement != null && beforePosition != null) {
            try {
                Point afterPosition = trackingElement.getLocation();
                int actualScrollDistance = beforePosition.y - afterPosition.y;
                if (actualScrollDistance > 0) {
                    return actualScrollDistance;
                }
            } catch (Exception e) {
                log.warning("Could not re-locate tracking element: " + e.getMessage());
            }
        }
        return expectedScrollHeight;
    }

    private int finishScrollAndCalculateDistance(WebElement trackingElement, Point beforePosition, int expectedScrollHeight) {
        try {
            Thread.sleep(200);
            return calculateActualScrollDistance(trackingElement, beforePosition, expectedScrollHeight);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return expectedScrollHeight;
        }
    }

    private int finishScroll(int scrollHeight, String method) {
        try {
            log.info("Successfully used: " + method);
            Thread.sleep(500);
            return scrollHeight;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return scrollHeight;
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
            log.info("Available capabilities: "+ caps.toString());
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
            Number currentScrollY = (Number) ((JavascriptExecutor) driver).executeScript(
                "return window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0;"
            );
            
            Number pageHeight = (Number) ((JavascriptExecutor) driver).executeScript(
                "return Math.max(" +
                "document.body.scrollHeight, " +
                "document.body.offsetHeight, " +
                "document.documentElement.clientHeight, " +
                "document.documentElement.scrollHeight, " +
                "document.documentElement.offsetHeight);"
            );
            
            Number viewportHeight = (Number) ((JavascriptExecutor) driver).executeScript(
                "return window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;"
            );
            
            long currentScrollYLong = currentScrollY.longValue();
            long pageHeightLong = pageHeight.longValue();
            long viewportHeightLong = viewportHeight.longValue();
            
            boolean isAtBottom = (currentScrollYLong + viewportHeightLong) >= pageHeightLong;
            
            if (isAtBottom) {
                log.info("Reached bottom of web page - scroll position: " + currentScrollYLong + ", page height: " + pageHeightLong);
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