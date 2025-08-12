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
    private final String testType; // Store test type (web or app)
    private final String deviceName; // Store device name

    public FullPageScreenshotUtil(WebDriver driver, String saveDirectoryName, String testType) {
        this.driver = driver;
        this.saveDirectoryName = saveDirectoryName;
        this.testType = testType;
        this.platform = detectPlatform();
        this.deviceName = detectDeviceName();
        log.info("FullPageScreenshotUtil initialized for testType: " + testType + ", platform: " + platform + ", deviceName: " + deviceName);

        // Ensure the directory exists
        File dir = new File(saveDirectoryName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Backward compatibility constructor - defaults to "web" testType
    public FullPageScreenshotUtil(WebDriver driver, String saveDirectoryName) {
        this(driver, saveDirectoryName, "web");
    }

    // Getter for testType
    public String getTestType() {
        return testType;
    }

    // Getter for deviceName
    public String getDeviceName() {
        return deviceName;
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
            elementUtil = new ElementBoundingBoxUtil(driver, testType, deviceName);
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
            int heightToAdd = 0;
            String adjustmentReason = "";
            
            // Apply status bar height adjustment for web tests on iOS devices
            if (testType.toLowerCase().contains("web") && 
                (deviceName.toLowerCase().contains("iphone") || 
                 deviceName.toLowerCase().contains("ipad") || 
                 deviceName.toLowerCase().contains("ipod"))) {
                
                heightToAdd = elementUtil.getStatusBarHeightInDevicePixels();
                adjustmentReason = "iOS status bar height";
                log.info("Web test on iOS device detected - applying status bar height: " + heightToAdd + " device pixels to all elements");
            }
            // Apply Chrome address bar height adjustment for web tests on Android devices
            else if (testType.toLowerCase().contains("web") && 
                     (deviceName.toLowerCase().contains("android") || 
                      platform.toLowerCase().contains("android"))) {
                
                heightToAdd = elementUtil.getAndroidChromeBarHeightInDevicePixels();
                adjustmentReason = "Android Chrome address bar height";
                log.info("Web test on Android device detected - applying Chrome address bar height: " + heightToAdd + " device pixels to all elements");
            }
            
            // Apply height adjustment if needed
            if (heightToAdd > 0) {
                List<ElementBoundingBox> adjustedElements = new ArrayList<>();
                for (ElementBoundingBox element : allElements) {
                    int originalY = element.getY();
                    int adjustedY = originalY + heightToAdd;
                    
                    // Create new ElementBoundingBox with adjusted Y coordinate
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
                    log.info("Element " + element.getSelectorKey() + ": Adjusted Y from " + originalY + " to " + adjustedY + " (added " + heightToAdd + " device pixels for " + adjustmentReason + ")");
                }
                
                // Replace the original list with adjusted elements
                allElements.clear();
                allElements.addAll(adjustedElements);
                
                log.info(adjustmentReason + " adjustment applied to " + allElements.size() + " elements");
            }
            
            Map<String, Object> uploadData = elementUtil.prepareUploadData(allElements);
            
            log.info("Element detection complete: " + allElements.size() + " total elements");
            log.info("Element upload data prepared: " + uploadData.toString());
        }
        
        log.info("Finished capturing all screenshots for full page.");
        return screenshotDir;
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
        ElementBoundingBoxUtil elementUtil = new ElementBoundingBoxUtil(driver, testType, deviceName);
        
        // iOS optimization: Detect all elements once before scrolling starts
        List<ElementBoundingBox> iosIgnoreElements = new ArrayList<>();
        List<ElementBoundingBox> iosSelectElements = new ArrayList<>();
        boolean isIOSOptimized = false;
        
        log.info("Element detection enabled for both ignore and select selectors");
        
        // Check if this is iOS platform or web testType for optimization
        if (platform.equals("ios") || testType.toLowerCase().equals("web")) {
            log.info("iOS platform or web testType detected - detecting all elements once before scrolling starts");
            isIOSOptimized = true;
            
            if (ignoreSelectors != null && !ignoreSelectors.isEmpty()) {
                log.info("Optimization: Detecting ignore selectors before scrolling: " + ignoreSelectors);
                iosIgnoreElements = elementUtil.detectAllElementsIOS(ignoreSelectors, "ignore");
                log.info("Optimization: Detected " + iosIgnoreElements.size() + " ignore elements before scrolling - will use for all chunks");
                
                // Add all iOS ignore elements to the final result immediately
                ignoreElements.addAll(iosIgnoreElements);
            }
            
            if (selectSelectors != null && !selectSelectors.isEmpty()) {
                log.info("Optimization: Detecting select selectors before scrolling: " + selectSelectors);
                iosSelectElements = elementUtil.detectAllElementsIOS(selectSelectors, "select");
                log.info("Optimization: Detected " + iosSelectElements.size() + " select elements before scrolling - will use for all chunks");
                
                // Add all iOS select elements to the final result immediately
                selectElements.addAll(iosSelectElements);
            }
        } else {
            // Standard logging for non-optimized platforms
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
                    // Optimization: No element detection needed during scrolling
                    // Elements were already detected and added to ignoreElements/selectElements before scrolling started
                    // No logging needed since we're doing nothing with elements during scrolling
                } else {
                    // Standard approach for Android app: Detect elements after screenshot
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
            
            // Update scroll position for element detection (only needed when optimization is not used)
            if (!isIOSOptimized) {
                elementUtil.updateScrollPosition(scrollDistance);
            }
            
            // Detect end of page
            isLastScroll = hasReachedBottom();
        }
        
        // Process and log final element data
        int totalIgnoreElements = ignoreElements.size();
        int totalSelectElements = selectElements.size();
        
        int heightToAdd = 0;
        String adjustmentReason = "";
        
        // Apply status bar height adjustment for web tests on iOS devices
        if (testType.toLowerCase().contains("web") && 
            (deviceName.toLowerCase().contains("iphone") || 
             deviceName.toLowerCase().contains("ipad") || 
             deviceName.toLowerCase().contains("ipod"))) {
            
            heightToAdd = elementUtil.getStatusBarHeightInDevicePixels();
            adjustmentReason = "iOS status bar height";
            log.info("Web test on iOS device detected - applying status bar height: " + heightToAdd + " device pixels to all elements");
        }
        // Apply Chrome address bar height adjustment for web tests on Android devices
        else if (testType.toLowerCase().contains("web") && 
                 (deviceName.toLowerCase().contains("android") || 
                  platform.toLowerCase().contains("android"))) {
            
            heightToAdd = elementUtil.getAndroidChromeBarHeightInDevicePixels();
            adjustmentReason = "Android Chrome address bar height";
            log.info("Web test on Android device detected - applying Chrome address bar height: " + heightToAdd + " device pixels to all elements");
        }
        
        // Apply height adjustment if needed
        if (heightToAdd > 0) {
            // Adjust ignore elements
            if (!ignoreElements.isEmpty()) {
                List<ElementBoundingBox> adjustedIgnoreElements = new ArrayList<>();
                for (ElementBoundingBox element : ignoreElements) {
                    int originalY = element.getY();
                    int adjustedY = originalY + heightToAdd;
                    
                    // Create new ElementBoundingBox with adjusted Y coordinate
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
                    
                    adjustedIgnoreElements.add(adjustedElement);
                    log.info("Ignore element " + element.getSelectorKey() + ": Adjusted Y from " + originalY + " to " + adjustedY + " (added " + heightToAdd + " device pixels for " + adjustmentReason + ")");
                }
                
                // Replace the original ignore elements list
                ignoreElements.clear();
                ignoreElements.addAll(adjustedIgnoreElements);
                log.info(adjustmentReason + " adjustment applied to " + ignoreElements.size() + " ignore elements");
            }
            
            // Adjust select elements
            if (!selectElements.isEmpty()) {
                List<ElementBoundingBox> adjustedSelectElements = new ArrayList<>();
                for (ElementBoundingBox element : selectElements) {
                    int originalY = element.getY();
                    int adjustedY = originalY + heightToAdd;
                    
                    // Create new ElementBoundingBox with adjusted Y coordinate
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
                    
                    adjustedSelectElements.add(adjustedElement);
                    log.info("Select element " + element.getSelectorKey() + ": Adjusted Y from " + originalY + " to " + adjustedY + " (added " + heightToAdd + " device pixels for " + adjustmentReason + ")");
                }
                
                // Replace the original select elements list
                selectElements.clear();
                selectElements.addAll(adjustedSelectElements);
                log.info(adjustmentReason + " adjustment applied to " + selectElements.size() + " select elements");
            }
            
            log.info("Total " + adjustmentReason.toLowerCase() + " adjustment applied to " + (totalIgnoreElements + totalSelectElements) + " elements");
        }
        
        if (isIOSOptimized) {
            log.info("Optimization: Element detection complete - " + totalIgnoreElements + " ignore elements, " + totalSelectElements + " select elements (detected once before scrolling)");
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
            // Use testType parameter instead of platform detection
            log.info("Scroll method selection - testType: " + testType);
            
            switch (testType.toLowerCase()) {
                case "app":
                    // For mobile apps, use platform-specific detection
                    switch (platform) {
                        case "android":
                            log.info("Using Android scroll method for app");
                            return scrollAndroid();
                        case "ios":
                            log.info("Using iOS scroll method for app");
                            return scrollIOS();
                        default:
                            log.warning("Unknown platform for app: " + platform + ", using Android scroll method");
                            return scrollAndroid();
                    }
                case "web":
                    log.info("Using Web scroll method");
                    return scrollWeb();
                default:
                    log.warning("Unknown testType: " + testType + ", defaulting to web scroll method");
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
     * Web-specific scroll method using JavaScript (similar to Golang implementation)
     */
    private int scrollWeb() {
        try {
            Dimension size = driver.manage().window().getSize();
            int screenHeight = size.getHeight();

            int scrollHeight = (int) (screenHeight * 0.4); // 60% of screen height
            
            log.info("Web scroll parameters - screenHeight: " + screenHeight + ", scrollHeight: " + scrollHeight + " pixels");

            // Use immediate scrolling without smooth behavior
            String scrollScript = "window.scrollBy(0, arguments[0]);";
            ((JavascriptExecutor) driver).executeScript(scrollScript, scrollHeight);
            
            // Add pause after scroll sequence (similar to Golang's 1000ms pause)
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warning("Web scroll pause interrupted: " + e.getMessage());
            }
            
            log.info("Web JavaScript scroll succeeded");
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
                return "web"; // Default to web
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
            samePageCounter = 0;
            return true; // Assume bottom reached on error
        }
        return false;
    }
}