package io.github.lambdatest.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Logger;

public class FullPageScreenshotUtil {
    private final WebDriver driver;
    private final String saveDirectoryName;
    private final Logger log = LoggerUtil.createLogger("lambdatest-java-app-sdk");
    private final String platform; // Store detected platform

    public FullPageScreenshotUtil(WebDriver driver, String saveDirectoryName) {
        this.driver = driver;
        this.saveDirectoryName = saveDirectoryName;
        this.platform = detectPlatform();
        log.info("FullPageScreenshotUtil initialized for platform: " + platform);

        // Ensure the directory exists
        File dir = new File(saveDirectoryName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private String prevPageSource = "";
    private int samePageCounter = 1;
    private int maxCount = 10;

    public List<File> captureFullPage(int pageCount) {
        return captureFullPage(pageCount, null);
    }

    public List<File> captureFullPage(int pageCount, List<String> xpaths) {
        if (pageCount <= 0) {
            pageCount = maxCount;
        }
        if (pageCount < maxCount) {
            maxCount = pageCount;
        }
        int chunkCount = 0;
        boolean isLastScroll = false;
        List<File> screenshotDir = new ArrayList<>();
        List<ElementBoundingBox> allElements = new ArrayList<>();
        ElementBoundingBoxUtil elementUtil = null;
        
        // Initialize element detection if XPaths provided
        if (xpaths != null && !xpaths.isEmpty()) {
            elementUtil = new ElementBoundingBoxUtil(driver);
            log.info("Element detection enabled for " + xpaths.size() + " XPaths");
        }
        
        while (!isLastScroll && chunkCount < maxCount) {
            File screenshotFile = captureAndSaveScreenshot(this.saveDirectoryName, chunkCount);
            if (screenshotFile != null) {
                screenshotDir.add(screenshotFile);
                
                // Detect elements after screenshot if XPaths provided
                if (elementUtil != null) {
                    List<ElementBoundingBox> chunkElements = elementUtil.detectElements(xpaths, chunkCount);
                    allElements.addAll(chunkElements);
                    log.info("Detected " + chunkElements.size() + " elements in chunk " + chunkCount);
                }
                
                chunkCount++;
            }
            // Perform scroll
            int scrollDistance = scrollDown();
            log.info("Scrolling attempt # " + chunkCount + " with distance: " + scrollDistance);
            
            // Update scroll position for element detection
            if (elementUtil != null) {
                elementUtil.updateScrollPosition(scrollDistance);
            }
            // Detect end of page
            isLastScroll = hasReachedBottom();
        }
        
        // Process and log final element data
        if (elementUtil != null && !allElements.isEmpty()) {
            Map<String, Object> uploadData = elementUtil.prepareUploadData(allElements);
            
            log.info("Element detection complete: " + allElements.size() + " total elements");
            log.info("Element upload data prepared: " + uploadData.toString());
        }
        
        log.info("Finished capturing all screenshots for full page.");
        return screenshotDir;
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
            Thread.sleep(1000);
            return scrollOnDevice();
        } catch (Exception e) {
            log.warning("Error during scroll operation: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private int scrollOnDevice() {
        try {
            // Use platform-specific scroll methods
            switch (platform) {
                case "android":
                    return scrollAndroid();
                case "ios":
                    return scrollIOS();
                case "web":
                    return scrollWeb();
                default:
                    log.warning("Unknown platform: " + platform + ", using web scroll method");
                    return scrollWeb();
            }
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
            
            // Use precise scroll parameters similar to Golang implementation
            int scrollEndY = (int) (screenHeight * 0.3); // End at 30% of screen height
            int scrollHeight = (int) (screenHeight * 0.35); // Scroll distance = 35% of screen height
            int scrollWidth = 3; // Narrow width for precise control
            int scrollLeft = screenWidth / 2; // Center the narrow scroll area
            
            log.info("Android scroll parameters - scrollEndY: " + scrollEndY + ", height: " + scrollHeight + ", width: " + scrollWidth);
            


            Map<String, Object> scrollParams = new HashMap<>();
            scrollParams.put("left", scrollLeft);
            scrollParams.put("top", scrollEndY);
            scrollParams.put("width", scrollWidth);
            scrollParams.put("height", scrollHeight);
            scrollParams.put("direction", "down");
            scrollParams.put("percent", 1.0);
                            scrollParams.put("speed", 300); // Very slow speed for precise controlled scrolling
            
            ((JavascriptExecutor) driver).executeScript("mobile:scrollGesture", scrollParams);
            log.info("Android scrollGesture succeeded");
            return scrollHeight;
            
        } catch (Exception e) {
            log.severe("Android scroll failed: " + e.getMessage());
            return 0;
        }
    }

    /**
     * iOS-specific scroll method using dragFromToForDuration
     */
    private int scrollIOS() {
        try {
            Dimension size = driver.manage().window().getSize();
            int screenHeight = size.getHeight();
            int screenWidth = size.getWidth();
            
            // Use precise scroll parameters
            int scrollEndY = (int) (screenHeight * 0.3); // End at 30% of screen height
            int scrollHeight = (int) (screenHeight * 0.35); // Scroll distance = 35% of screen height
            int scrollWidth = 3; // Narrow width for precise control
            int scrollLeft = screenWidth / 2; // Center the narrow scroll area
            
            log.info("iOS scroll parameters - scrollEndY: " + scrollEndY + ", height: " + scrollHeight + ", width: " + scrollWidth);

            int fromY = scrollEndY + scrollHeight; // Start from bottom of scroll area
            int toY = scrollEndY; // End at top of scroll area
            
            Map<String, Object> swipeObj = new HashMap<>();
            swipeObj.put("fromX", scrollLeft);
            swipeObj.put("fromY", fromY);
            swipeObj.put("toX", scrollLeft);
            swipeObj.put("toY", toY);
            swipeObj.put("duration", 3.5); // Very slow duration for precise controlled scrolling
            
            ((JavascriptExecutor) driver).executeScript("mobile:dragFromToForDuration", swipeObj);
            log.info("iOS dragFromTo scroll succeeded");
            return scrollHeight;
            
        } catch (Exception e) {
            log.severe("iOS scroll failed: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Web-specific scroll method using JavaScript
     */
    private int scrollWeb() {
        try {
            Dimension size = driver.manage().window().getSize();
            int screenHeight = size.getHeight();
            int scrollHeight = (int) (screenHeight * 0.35); // Scroll distance = 35% of screen height
            
            log.info("Web scroll parameters - height: " + scrollHeight);

            ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, arguments[0]);", scrollHeight);
            log.info("Web JavaScript scroll succeeded");
            return scrollHeight;
            
        } catch (Exception e) {
            log.severe("Web scroll failed: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Detect platform (iOS/Android/Web)
     */
    private String detectPlatform() {
        try {
            log.info("Detecting platform...");
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            
            String platformName = "";
            String browserName = "";
            
            try {
                // Try to get platform name
                if (caps.getCapability("platformName") != null) {
                    platformName = caps.getCapability("platformName").toString().toLowerCase();
                    log.info("Found platformName capability: " + platformName);
                } else if (caps.getCapability("platform") != null) {
                    platformName = caps.getCapability("platform").toString().toLowerCase();
                    log.info("Found platform capability: " + platformName);
                } else {
                    log.warning("No platform capability found");
                }
                
                // Try to get browser name
                if (caps.getCapability("browserName") != null) {
                    browserName = caps.getCapability("browserName").toString().toLowerCase();
                    log.info("Found browserName capability: " + browserName);
                } else {
                    log.warning("No browserName capability found");
                }
                
                String detectedPlatform;
                if (platformName.contains("ios")) {
                    detectedPlatform = "ios";
                } else if (platformName.contains("android")) {
                    detectedPlatform = "android";
                } else {
                    detectedPlatform = "web";
                }
                
                log.info("Detected platform: " + detectedPlatform);
                return detectedPlatform;
                
            } catch (Exception e) {
                log.warning("Error accessing capabilities: " + e.getMessage());
                return "web"; // Default to web
            }
            
        } catch (Exception e) {
            log.warning("Failed to detect platform: " + e.getMessage());
            log.warning("Using default platform: web");
            return "web";
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
            samePageCounter = 0;
            return true; // Assume bottom reached on error
        }
        return false;
    }
}