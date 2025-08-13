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
    private final WebDriver driver;
    private final String saveDirectoryName;
    private final Logger log = LoggerUtil.createLogger("lambdatest-java-app-sdk");
    private final String platform;
    private final String testType;
    private final String deviceName;

    public FullPageScreenshotUtil(WebDriver driver, String saveDirectoryName, String testType) {
        this.driver = driver;
        this.saveDirectoryName = saveDirectoryName;
        this.testType = testType;
        this.platform = detectPlatform();
        this.deviceName = detectDeviceName();
        log.info("FullPageScreenshotUtil initialized for testType: " + testType + ", platform: " + platform + ", deviceName: " + deviceName);

        File dir = new File(saveDirectoryName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private String prevPageSource = "";
    private int maxCount = 10;

    /**
     * Capture full page screenshots and detect elements for both ignore and select purposes
     * This method captures screenshots once and processes both selector types efficiently
     * Can handle cases with no selectors, only ignore selectors, only select selectors, or both
     */
    public Map<String, Object> captureFullPageScreenshot(int pageCount, Map<String, List<String>> ignoreSelectors, Map<String, List<String>> selectSelectors) {
        if (pageCount <= 0) {
            pageCount = maxCount;
        }
        if (pageCount < maxCount) {
            maxCount = pageCount;
        }
        int chunkCount = 0;
        boolean isLastScroll = false;
        List<File> screenshotDir = new ArrayList<>();
        List<ElementBoundingBox> ignoreElements = new ArrayList<>();
        List<ElementBoundingBox> selectElements = new ArrayList<>();
        ElementBoundingBoxUtil elementUtil = null;

        boolean hasSelectors = (ignoreSelectors != null && !ignoreSelectors.isEmpty()) || 
                              (selectSelectors != null && !selectSelectors.isEmpty());
        
        if (hasSelectors) {
            elementUtil = new ElementBoundingBoxUtil(driver, testType, deviceName);
        }

        List<ElementBoundingBox> ignoreElementsCapturedAtStart;
        List<ElementBoundingBox> selectElementsCapturedAtStart;
        boolean captureAllElementsAtTheStart = false;


        if (hasSelectors && (platform.equals("ios") || testType.equalsIgnoreCase("web"))) {
            captureAllElementsAtTheStart = true;
            
            if (ignoreSelectors != null && !ignoreSelectors.isEmpty()) {
                ignoreElementsCapturedAtStart = elementUtil.detectAllElementsAtTheStart(ignoreSelectors, "ignore");
                if (ignoreElementsCapturedAtStart != null) {
                    ignoreElements.addAll(ignoreElementsCapturedAtStart);
                }
            }
            
            if (selectSelectors != null && !selectSelectors.isEmpty()) {
                selectElementsCapturedAtStart = elementUtil.detectAllElementsAtTheStart(selectSelectors, "select");
                if (selectElementsCapturedAtStart != null) {
                    selectElements.addAll(selectElementsCapturedAtStart);
                }
            }
        }
        
        while (!isLastScroll && chunkCount < maxCount) {
            File screenshotFile = captureAndSaveScreenshot(this.saveDirectoryName, chunkCount);
            screenshotDir.add(screenshotFile);

            if (hasSelectors && !captureAllElementsAtTheStart) {
                if (ignoreSelectors != null && !ignoreSelectors.isEmpty()) {
                    List<ElementBoundingBox> chunkIgnoreElements = elementUtil.detectElements(ignoreSelectors, chunkCount, "ignore");
                    ignoreElements.addAll(chunkIgnoreElements);
                }

                if (selectSelectors != null && !selectSelectors.isEmpty()) {
                    List<ElementBoundingBox> chunkSelectElements = elementUtil.detectElements(selectSelectors, chunkCount, "select");
                    selectElements.addAll(chunkSelectElements);
                }
            }

            chunkCount++;
            int scrollDistance = scrollDown();

            if (hasSelectors && !captureAllElementsAtTheStart) {
                elementUtil.updateScrollPosition(scrollDistance);
            }

            isLastScroll = hasReachedBottom();
        }

        if (hasSelectors && (!ignoreElements.isEmpty() || !selectElements.isEmpty())) {
            adjustElementHeights(ignoreElements, selectElements, elementUtil);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("screenshots", screenshotDir);
        result.put("ignoreElements", ignoreElements);
        result.put("selectElements", selectElements);
        return result;
    }

    private void adjustElementYCoordinates(List<ElementBoundingBox> elements, int heightToAdd) {
        if (elements.isEmpty() || heightToAdd <= 0) {
            return;
        }
        
        List<ElementBoundingBox> adjustedElements = new ArrayList<>();
        for (ElementBoundingBox element : elements) {
            int adjustedY = element.getY() + heightToAdd;
            
            ElementBoundingBox adjustedElement = new ElementBoundingBox(
                element.getSelectorKey(),
                element.getX(),
                adjustedY,
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
        if (testType.toLowerCase().contains("web") && 
            (deviceName.toLowerCase().contains("iphone") || 
             deviceName.toLowerCase().contains("ipad") || 
             deviceName.toLowerCase().contains("ipod"))) {
            
            return elementUtil.getStatusBarHeightInDevicePixels();
        }
        else if (testType.toLowerCase().contains("web") && 
                 (deviceName.toLowerCase().contains("android") || 
                  platform.toLowerCase().contains("android"))) {
            
            return elementUtil.getAndroidChromeBarHeightInDevicePixels();
        }
        
        return 0;
    }

    private void adjustElementHeights(List<ElementBoundingBox> ignoreElements, List<ElementBoundingBox> selectElements, ElementBoundingBoxUtil elementUtil) {
        int heightToAdd = calculateHeightAdjustment(elementUtil);
        
        if (heightToAdd > 0) {
            adjustElementYCoordinates(ignoreElements, heightToAdd);
            adjustElementYCoordinates(selectElements, heightToAdd);
        }
    }

    private File captureAndSaveScreenshot(String ssDir, int index) {
        File destinationFile = new File(ssDir + "/" + ssDir + "_" + index + ".png");
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
            Thread.sleep(200);
            return scrollOnDevice();
        } catch (Exception e) {
            log.warning("Error during scroll operation: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private int scrollOnDevice() {
        try {
            if (testType.equalsIgnoreCase("app")) {
                if (platform.equals("ios")) {
                    return scrollIOS();
                }
                return scrollAndroid();
            }
            return scrollWeb();
        } catch (Exception e) {
            log.severe("Error in scrollOnDevice: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Android-specific scroll method using scrollGesture
     */
    private int scrollAndroid() {
        try {
            Dimension size = driver.manage().window().getSize();
            int screenHeight = size.getHeight();
            int screenWidth = size.getWidth();

            int scrollEndY = (int) (screenHeight * 0.3); // End at 30% of screen height
            int scrollHeight = (int) (screenHeight * 0.35); // Scroll distance = 35% of screen height
            int scrollWidth = 3;
            int scrollLeft = screenWidth / 2;


            Map<String, Object> scrollParams = new HashMap<>();
            scrollParams.put("left", scrollLeft);
            scrollParams.put("top", scrollEndY);
            scrollParams.put("width", scrollWidth);
            scrollParams.put("height", scrollHeight);
            scrollParams.put("direction", "down");
            scrollParams.put("percent", 1.0);
                            scrollParams.put("speed", 300); // Very slow speed for precise controlled scrolling
            
            ((JavascriptExecutor) driver).executeScript("mobile:scrollGesture", scrollParams);
            return scrollHeight;
            
        } catch (Exception e) {
            log.severe("Android scroll failed: " + e.getMessage());
            return 0;
        }
    }

        /**
     * iOS-specific scroll method using mobile:scroll for better inertia control
     * Falls back to dragFromToForDuration if mobile:scroll fails
     */
    private int scrollIOS() {
        try {
            Dimension size = driver.manage().window().getSize();
            int screenHeight = size.getHeight();
            int screenWidth = size.getWidth();
            

            int scrollHeight = (int) (screenHeight * 0.3); // Scroll distance = 30% of screen height
            
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence dragSequence = new Sequence(finger, 1);

            int startY = (int) (screenHeight * 0.7);
            int endY = (int) (screenHeight * 0.4);
            int x = screenWidth / 2;

            dragSequence.addAction(finger.createPointerMove(Duration.ZERO, 
                PointerInput.Origin.viewport(), x, startY));
            dragSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            dragSequence.addAction(finger.createPointerMove(Duration.ofMillis(1500),
                PointerInput.Origin.viewport(), x, endY));
            dragSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            
            ((RemoteWebDriver) driver).perform(Arrays.asList(dragSequence));
            return scrollHeight;
            
        } catch (Exception e) {
            log.severe("iOS scroll failed completely: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Web-specific scroll method using JavaScript (similar to Golang implementation)
     */
    private int scrollWeb() {
        try {
            Dimension size = driver.manage().window().getSize();
            int screenHeight = size.getHeight();

            int scrollHeight = (int) (screenHeight * 0.4);

            String scrollScript = "window.scrollBy(0, arguments[0]);";
            ((JavascriptExecutor) driver).executeScript(scrollScript, scrollHeight);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warning("Web scroll pause interrupted: " + e.getMessage());
            }

            return scrollHeight;
            
        } catch (Exception e) {
            log.severe("Web JavaScript scroll failed: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Detect platform (iOS/Android/Web)
     */
    private String detectPlatform() {
        try {
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            
            String platformName = "";
            
            try {
                if (caps.getCapability("platformName") != null) {
                    platformName = caps.getCapability("platformName").toString().toLowerCase();
                } else if (caps.getCapability("platform") != null) {
                    platformName = caps.getCapability("platform").toString().toLowerCase();
                } else {
                    log.warning("No platform capability found");
                }

                log.info("platform is ," + platformName);
                
                String detectedPlatform;
                if (platformName.contains("ios")) {
                    detectedPlatform = "ios";
                } else if (platformName.contains("android")) {
                    detectedPlatform = "android";
                } else {
                    detectedPlatform = "web";
                }

                return detectedPlatform;
                
            } catch (Exception e) {
                log.warning("Error accessing capabilities: " + e.getMessage());
                return "web";
            }
            
        } catch (Exception e) {
            log.warning("Failed to detect platform: " + e.getMessage());
            log.warning("Using default platform: web");
            return "web";
        }
    }

    /**
     * Detect device name from WebDriver capabilities
     */
    private String detectDeviceName() {
        try {
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            String deviceName = "";
            
            try {
                // Try to get device name from various capability keys
                if (caps.getCapability("deviceName") != null) {
                    deviceName = caps.getCapability("deviceName").toString();
                } else if (caps.getCapability("device") != null) {
                    deviceName = caps.getCapability("device").toString();
                } else if (caps.getCapability("deviceModel") != null) {
                    deviceName = caps.getCapability("deviceModel").toString();
                } else if (caps.getCapability("deviceType") != null) {
                    deviceName = caps.getCapability("deviceType").toString();
                } else {
                    log.info("No device name capability found, using platform as device identifier");
                    deviceName = platform; // Use platform as fallback
                }
                
                return deviceName;
                
            } catch (Exception e) {
                log.warning("Error accessing device capabilities: " + e.getMessage());
                return platform; // Default to platform name
            }
            
        } catch (Exception e) {
            log.warning("Failed to detect device name: " + e.getMessage());
            return platform; // Default to platform name
        }
    }

    private boolean hasReachedBottom() {
        try {
            Thread.sleep(1000);
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
            }
        } catch (Exception e) {
            log.warning("Error checking if reached bottom: " + e.getMessage());
            return true; // Assume bottom reached on error
        }
        return false;
    }
}