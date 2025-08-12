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
        return captureFullPage(pageCount, (Map<String, List<String>>) null);
    }

    public List<File> captureFullPage(int pageCount, List<String> xpaths) {
        // Convert XPaths to the new selector format for backward compatibility
        Map<String, List<String>> selectors = null;
        if (xpaths != null && !xpaths.isEmpty()) {
            selectors = new HashMap<>();
            selectors.put("xpath", xpaths);
        }
        return captureFullPage(pageCount, selectors);
    }

    /**
     * Capture full page screenshots with multi-selector element detection
     * Supports XPath, class, accessibility ID, name, ID, and CSS selectors
     */
    public List<File> captureFullPage(int pageCount, Map<String, List<String>> selectors) {
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
        
        // Initialize element detection if selectors provided
        if (selectors != null && !selectors.isEmpty()) {
            elementUtil = new ElementBoundingBoxUtil(driver);
            log.info("Element detection enabled for selectors: " + selectors);
        }
        
        while (!isLastScroll && chunkCount < maxCount) {
            File screenshotFile = captureAndSaveScreenshot(this.saveDirectoryName, chunkCount);
            if (screenshotFile != null) {
                screenshotDir.add(screenshotFile);
                
                // Detect elements after screenshot if selectors provided
                if (elementUtil != null) {
                    List<ElementBoundingBox> chunkElements = elementUtil.detectElements(selectors, chunkCount, "ignore"); // Default to ignore for backward compatibility
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

    /**
     * Capture full page screenshots and return both screenshots and detected elements
     * This method allows the caller to get both the screenshot files and the detected elements
     * without having to do duplicate element detection
     */
    public Map<String, Object> captureFullPageWithElements(int pageCount, Map<String, List<String>> selectors, String purpose) {
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
        
        // iOS optimization: Detect all elements once before scrolling starts
        List<ElementBoundingBox> iosAllElements = new ArrayList<>();
        boolean isIOSOptimized = false;
        
        // Initialize element detection if selectors provided
        if (selectors != null && !selectors.isEmpty()) {
            elementUtil = new ElementBoundingBoxUtil(driver);
            log.info("Element detection enabled for " + purpose + " selectors: " + selectors);
            
            // Check if this is iOS platform for optimization
            if (platform.equals("ios")) {
                log.info("iOS platform detected - detecting all elements once before scrolling starts");
                iosAllElements = elementUtil.detectAllElementsIOS(selectors, purpose);
                isIOSOptimized = true;
                log.info("iOS optimization: Detected " + iosAllElements.size() + " elements before scrolling - will use for all chunks");
                
                // Add all iOS elements to the final result immediately
                allElements.addAll(iosAllElements);
            }
        }
        
        while (!isLastScroll && chunkCount < maxCount) {
            File screenshotFile = captureAndSaveScreenshot(this.saveDirectoryName, chunkCount);
            if (screenshotFile != null) {
                screenshotDir.add(screenshotFile);
                
                // Element detection logic
                if (elementUtil != null) {
                    if (isIOSOptimized) {
                        // iOS optimization: No element detection needed during scrolling
                        // Elements were already detected and added to allElements before scrolling started
                        // No logging needed since we're doing nothing with elements during scrolling
                    } else {
                        // Standard approach for Android/Web: Detect elements after screenshot
                        List<ElementBoundingBox> chunkElements = elementUtil.detectElements(selectors, chunkCount, purpose);
                        allElements.addAll(chunkElements);
                        log.info("Detected " + chunkElements.size() + " " + purpose + " elements in chunk " + chunkCount);
                    }
                }
                
                chunkCount++;
            }
            // Perform scroll
            int scrollDistance = scrollDown();
            log.info("Scrolling attempt # " + chunkCount + " with distance: " + scrollDistance);
            
            // Update scroll position for element detection (only needed for non-iOS platforms)
            if (elementUtil != null && !isIOSOptimized) {
                elementUtil.updateScrollPosition(scrollDistance);
            }
            
            // Detect end of page
            isLastScroll = hasReachedBottom();
        }
        
        // Process and log final element data
        if (elementUtil != null && !allElements.isEmpty()) {
            if (isIOSOptimized) {
                log.info("iOS optimization: Element detection complete - " + allElements.size() + " total " + purpose + " elements (detected once before scrolling)");
            } else {
                log.info("Element detection complete: " + allElements.size() + " total " + purpose + " elements");
            }
        }
        
        log.info("Finished capturing all screenshots for full page with " + purpose + " elements.");
        
        // Return both screenshots and elements
        Map<String, Object> result = new HashMap<>();
        result.put("screenshots", screenshotDir);
        result.put("elements", allElements);
        return result;
    }

    /**
     * Capture full page screenshots and detect elements for both ignore and select purposes
     * This method captures screenshots once and processes both selector types efficiently
     */
    public Map<String, Object> captureFullPageWithBothSelectors(int pageCount, 
                                                              Map<String, List<String>> ignoreSelectors, 
                                                              Map<String, List<String>> selectSelectors) {
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
        ElementBoundingBoxUtil elementUtil = new ElementBoundingBoxUtil(driver);
        
        // iOS optimization: Detect all elements once before scrolling starts
        List<ElementBoundingBox> iosIgnoreElements = new ArrayList<>();
        List<ElementBoundingBox> iosSelectElements = new ArrayList<>();
        boolean isIOSOptimized = false;
        
        log.info("Element detection enabled for both ignore and select selectors");
        
        // Check if this is iOS platform for optimization
        if (platform.equals("ios")) {
            log.info("iOS platform detected - detecting all elements once before scrolling starts");
            isIOSOptimized = true;
            
            if (ignoreSelectors != null && !ignoreSelectors.isEmpty()) {
                log.info("iOS: Detecting ignore selectors before scrolling: " + ignoreSelectors);
                iosIgnoreElements = elementUtil.detectAllElementsIOS(ignoreSelectors, "ignore");
                log.info("iOS optimization: Detected " + iosIgnoreElements.size() + " ignore elements before scrolling - will use for all chunks");
                
                // Add all iOS ignore elements to the final result immediately
                ignoreElements.addAll(iosIgnoreElements);
            }
            
            if (selectSelectors != null && !selectSelectors.isEmpty()) {
                log.info("iOS: Detecting select selectors before scrolling: " + selectSelectors);
                iosSelectElements = elementUtil.detectAllElementsIOS(selectSelectors, "select");
                log.info("iOS optimization: Detected " + iosSelectElements.size() + " select elements before scrolling - will use for all chunks");
                
                // Add all iOS select elements to the final result immediately
                selectElements.addAll(iosSelectElements);
            }
        } else {
            // Standard logging for non-iOS platforms
            if (ignoreSelectors != null && !ignoreSelectors.isEmpty()) {
                log.info("Ignore selectors: " + ignoreSelectors);
            }
            if (selectSelectors != null && !selectSelectors.isEmpty()) {
                log.info("Select selectors: " + selectSelectors);
            }
        }
        
        while (!isLastScroll && chunkCount < maxCount) {
            File screenshotFile = captureAndSaveScreenshot(this.saveDirectoryName, chunkCount);
            if (screenshotFile != null) {
                screenshotDir.add(screenshotFile);
                
                if (isIOSOptimized) {
                    // iOS optimization: No element detection needed during scrolling
                    // Elements were already detected and added to ignoreElements/selectElements before scrolling started
                    // No logging needed since we're doing nothing with elements during scrolling
                } else {
                    // Standard approach for Android/Web: Detect elements after screenshot
                    if (ignoreSelectors != null && !ignoreSelectors.isEmpty()) {
                        List<ElementBoundingBox> chunkIgnoreElements = elementUtil.detectElements(ignoreSelectors, chunkCount, "ignore");
                        ignoreElements.addAll(chunkIgnoreElements);
                        log.info("Detected " + chunkIgnoreElements.size() + " ignore elements in chunk " + chunkCount);
                    }
                    
                    if (selectSelectors != null && !selectSelectors.isEmpty()) {
                        List<ElementBoundingBox> chunkSelectElements = elementUtil.detectElements(selectSelectors, chunkCount, "select");
                        selectElements.addAll(chunkSelectElements);
                        log.info("Detected " + chunkSelectElements.size() + " select elements in chunk " + chunkCount);
                    }
                }
                
                chunkCount++;
            }
            // Perform scroll
            int scrollDistance = scrollDown();
            log.info("Scrolling attempt # " + chunkCount + " with distance: " + scrollDistance);
            
            // Update scroll position for element detection (only needed for non-iOS platforms)
            if (!isIOSOptimized) {
                elementUtil.updateScrollPosition(scrollDistance);
            }
            
            // Detect end of page
            isLastScroll = hasReachedBottom();
        }
        
        // Process and log final element data
        int totalIgnoreElements = ignoreElements.size();
        int totalSelectElements = selectElements.size();
        
        if (isIOSOptimized) {
            log.info("iOS optimization: Element detection complete - " + totalIgnoreElements + " ignore elements, " + totalSelectElements + " select elements (detected once before scrolling)");
        } else {
            log.info("Element detection complete: " + totalIgnoreElements + " ignore elements, " + totalSelectElements + " select elements");
        }
        
        log.info("Finished capturing all screenshots for full page with both selector types.");
        
        // Return screenshots and both types of elements
        Map<String, Object> result = new HashMap<>();
        result.put("screenshots", screenshotDir);
        result.put("ignoreElements", ignoreElements);
        result.put("selectElements", selectElements);
        return result;
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
            Thread.sleep(200); // Very short wait time between scrolls
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
     * iOS-specific scroll method using mobile:scroll for better inertia control
     * Falls back to dragFromToForDuration if mobile:scroll fails
     */
    private int scrollIOS() {
        try {
            Dimension size = driver.manage().window().getSize();
            int screenHeight = size.getHeight();
            int screenWidth = size.getWidth();
            
            // Calculate scroll distance
            int scrollHeight = (int) (screenHeight * 0.3); // Scroll distance = 30% of screen height
            
            log.info("iOS scroll distance: " + scrollHeight + " pixels");

            // Use W3C Actions API for precise iOS scrolling with zero inertia
            log.info("Using iOS W3C Actions API for precise scrolling");
            
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence dragSequence = new Sequence(finger, 1);
            
            // Calculate precise coordinates
            int startY = (int) (screenHeight * 0.7); // Start at 70% of screen height
            int endY = (int) (screenHeight * 0.4);   // End at 40% of screen height (30% scroll distance)
            int x = screenWidth / 2; // Center horizontally
            
            log.info("iOS scroll coordinates - startY: " + startY + ", endY: " + endY + ", x: " + x);
            
            // Create controlled touch gesture without momentum
            dragSequence.addAction(finger.createPointerMove(Duration.ZERO, 
                PointerInput.Origin.viewport(), x, startY));
            dragSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            dragSequence.addAction(finger.createPointerMove(Duration.ofMillis(1500),
                PointerInput.Origin.viewport(), x, endY));
            dragSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            
            ((RemoteWebDriver) driver).perform(Arrays.asList(dragSequence));
            log.info("iOS W3C Actions API scroll completed");
            return scrollHeight;
            
        } catch (Exception e) {
            log.severe("iOS scroll failed completely: " + e.getMessage());
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
            int scrollHeight = (int) (screenHeight * 0.8); // Scroll distance = 80% of screen height
            
            log.info("Web scroll parameters - height: " + scrollHeight);

            // Use smooth scrolling with more control
            String scrollScript = "window.scrollBy({top: arguments[0], left: 0, behavior: 'smooth'});";
            ((JavascriptExecutor) driver).executeScript(scrollScript, scrollHeight);
            
            // Alternative: Force immediate scroll if smooth scrolling doesn't work well
            // ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, arguments[0]);", scrollHeight);
            
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
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            
            String platformName = "";
            String browserName = "";
            
            try {
                // Try to get platform name
                if (caps.getCapability("platformName") != null) {
                    platformName = caps.getCapability("platformName").toString().toLowerCase();
                } else if (caps.getCapability("platform") != null) {
                    platformName = caps.getCapability("platform").toString().toLowerCase();
                } else {
                    log.warning("No platform capability found");
                }
                
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