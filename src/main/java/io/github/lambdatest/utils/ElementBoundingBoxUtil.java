package io.github.lambdatest.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.util.*;
import java.util.logging.Logger;

public class ElementBoundingBoxUtil {
    private final WebDriver driver;
    private final Logger log = LoggerUtil.createLogger("lambdatest-java-app-sdk");
    private static final int PROXIMITY_THRESHOLD = 10; // pixels
    private int cumulativeScrollPosition = 0; // Track cumulative scroll position in CSS pixels for native apps
    private double devicePixelRatio = 1.0; // Track device pixel ratio for scaling

    public ElementBoundingBoxUtil(WebDriver driver) {
        this.driver = driver;
        this.devicePixelRatio = getDevicePixelRatio();
        log.info("ElementBoundingBoxUtil initialized with cumulative scroll position: " + cumulativeScrollPosition + " CSS pixels, device pixel ratio: " + devicePixelRatio);
    }

    /**
     * Detect elements for given XPaths in current viewport
     */
    public List<ElementBoundingBox> detectElements(List<String> xpaths, int chunkIndex) {
        List<ElementBoundingBox> detectedElements = new ArrayList<>();
        String platform = detectPlatform();

        log.info("Starting element detection for chunk " + chunkIndex + " with " + (xpaths != null ? xpaths.size() : 0) + " XPaths");
        log.info("Detected platform: " + platform);

        if (xpaths == null || xpaths.isEmpty()) {
            log.info("No XPaths provided for element detection");
            return detectedElements;
        }

        for (int i = 0; i < xpaths.size(); i++) {
            String xpath = xpaths.get(i);
            log.info("Processing XPath " + (i + 1) + "/" + xpaths.size() + ": " + xpath);
            
            try {
                List<WebElement> elements = driver.findElements(By.xpath(xpath));
                log.info("Found " + elements.size() + " elements for XPath: " + xpath);
                
                for (int j = 0; j < elements.size(); j++) {
                    WebElement element = elements.get(j);
                    log.info("Processing element " + (j + 1) + "/" + elements.size() + " for XPath: " + xpath);
                    
                    ElementBoundingBox boundingBox = createBoundingBox(element, xpath, chunkIndex, platform);
                    if (boundingBox != null) {
                        log.info("Created bounding box: " + boundingBox.toString());
                        
                        if (isElementFullyInViewport(boundingBox)) {
                            detectedElements.add(boundingBox);
                            log.info("Element added to detected list: " + boundingBox.toString());
                        } else {
                            log.info("Element not fully in viewport, skipping: " + boundingBox.toString());
                        }
                    } else {
                        log.warning("Failed to create bounding box for element " + (j + 1) + " of XPath: " + xpath);
                    }
                }
            } catch (Exception e) {
                log.warning("Failed to detect elements for XPath '" + xpath + "': " + e.getMessage());
                log.warning("Exception details: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        log.info("Element detection completed for chunk " + chunkIndex + ". Total elements detected: " + detectedElements.size());
        return detectedElements;
    }

    /**
     * Create bounding box from WebElement with absolute coordinates
     */
    private ElementBoundingBox createBoundingBox(WebElement element, String xpath, int chunkIndex, String platform) {
        try {
            log.info("Creating bounding box for XPath: " + xpath);
            
            // Get element location and size relative to viewport
            Point location = element.getLocation();
            Dimension size = element.getSize();
            
            log.info("Element viewport location: (" + location.getX() + ", " + location.getY() + ")");
            log.info("Element size: " + size.getWidth() + "x" + size.getHeight());
            
            // Get cumulative scroll position based on platform
            int scrollY = getCurrentScrollPosition();
            log.info("Cumulative scroll position: " + scrollY);
            
            // Convert to absolute page coordinates with proper scaling
            // Both native and web apps: element Y + scroll position (all in CSS pixels), then scale by device pixel ratio
            int absoluteX = (int) (location.getX() * devicePixelRatio);
            int absoluteY = (int) ((location.getY() + scrollY) * devicePixelRatio);
            
            log.info("Coordinate calculation details:");
            log.info("  Element viewport Y: " + location.getY());
            log.info("  Current scroll Y: " + scrollY);
            log.info("  Device pixel ratio: " + devicePixelRatio);
            log.info("  Raw absolute Y: " + (location.getY() + scrollY) + " (viewport Y + scroll Y)");
            log.info("  Scaled absolute Y: " + absoluteY + " (raw * device pixel ratio)");
            log.info("Calculated absolute coordinates: (" + absoluteX + ", " + absoluteY + ")");
            
            // Scale element dimensions to device pixels
            int scaledWidth = (int) (size.getWidth() * devicePixelRatio);
            int scaledHeight = (int) (size.getHeight() * devicePixelRatio);
            
            ElementBoundingBox boundingBox = new ElementBoundingBox(xpath, absoluteX, absoluteY, scaledWidth, scaledHeight, chunkIndex, platform);
            log.info("Successfully created bounding box: " + boundingBox.toString());
            
            return boundingBox;
            
        } catch (Exception e) {
            log.warning("Failed to create bounding box for element with XPath '" + xpath + "': " + e.getMessage());
            log.warning("Exception type: " + e.getClass().getSimpleName());
            log.warning("Exception stack trace: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if element is completely within current viewport
     */
    private boolean isElementFullyInViewport(ElementBoundingBox boundingBox) {
        try {
            log.info("Checking viewport bounds for element: " + boundingBox.toString());
            
            Dimension viewportSize = driver.manage().window().getSize();
            int scrollY = getCurrentScrollPosition();
            
            log.info("Viewport size: " + viewportSize.getWidth() + "x" + viewportSize.getHeight());
            log.info("Current scroll position: " + scrollY);
            
            // Calculate viewport-relative position
            // boundingBox.getY() contains absolute page position, so subtract scroll to get viewport position
            int viewportY = boundingBox.getY() - scrollY;
            log.info("Element viewport-relative Y position: " + viewportY);
            
            // Check if element is completely within viewport
            boolean xInBounds = boundingBox.getX() >= 0 && 
                               boundingBox.getX() + boundingBox.getWidth() <= viewportSize.getWidth();
            boolean yInBounds = viewportY >= 0 && 
                               viewportY + boundingBox.getHeight() <= viewportSize.getHeight();
            
            log.info("X bounds check: " + xInBounds + " (X: " + boundingBox.getX() + ", width: " + boundingBox.getWidth() + ", viewport width: " + viewportSize.getWidth() + ")");
            log.info("Y bounds check: " + yInBounds + " (viewport Y: " + viewportY + ", height: " + boundingBox.getHeight() + ", viewport height: " + viewportSize.getHeight() + ")");
            
            boolean isFullyInViewport = xInBounds && yInBounds;
            log.info("Element fully in viewport: " + isFullyInViewport);
            
            return isFullyInViewport;
                   
        } catch (Exception e) {
            log.warning("Failed to check viewport bounds: " + e.getMessage());
            log.warning("Exception type: " + e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Get current scroll position - handles both web and native mobile apps
     */
    private int getCurrentScrollPosition() {
        try {
            String platform = detectPlatform();
            
            if (platform.contains("web")) {
                // For web apps, use JavaScript to get current scroll position
                try {
                    Long scrollPosition = (Long) ((JavascriptExecutor) driver).executeScript(
                        "return window.pageYOffset || document.documentElement.scrollTop || 0;"
                    );
                    log.info("Retrieved scroll position from JavaScript: " + scrollPosition);
                    return scrollPosition.intValue();
                } catch (Exception e) {
                    log.warning("Failed to get scroll position from JavaScript: " + e.getMessage());
                    log.info("Using tracked cumulative scroll position: " + cumulativeScrollPosition + " CSS pixels");
                    return cumulativeScrollPosition;
                }
            } else {
                // For native mobile apps, use tracked cumulative scroll position
                log.info("Native mobile app detected, using tracked cumulative scroll position: " + cumulativeScrollPosition + " CSS pixels");
                return cumulativeScrollPosition;
            }
        } catch (Exception e) {
            log.warning("Failed to get scroll position: " + e.getMessage());
            log.warning("Exception type: " + e.getClass().getSimpleName());
            log.warning("Using default scroll position: 0");
            return 0;
        }
    }

    /**
     * Get device pixel ratio for proper scaling
     */
    private double getDevicePixelRatio() {
        try {
            String platform = detectPlatform();
            
            if (platform.contains("web")) {
                // For web apps, use JavaScript to get device pixel ratio
                try {
                    Double dpr = (Double) ((JavascriptExecutor) driver).executeScript(
                        "return window.devicePixelRatio || 1;"
                    );
                    log.info("Retrieved device pixel ratio from JavaScript: " + dpr);
                    return dpr;
                } catch (Exception e) {
                    log.warning("Failed to get device pixel ratio from JavaScript: " + e.getMessage());
                    log.info("Using default device pixel ratio: 1.0");
                    return 1.0;
                }
            } else {
                // For native mobile apps, try to get from capabilities or use default
                try {
                    Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
                    Object pixelRatio = caps.getCapability("devicePixelRatio");
                    if (pixelRatio != null) {
                        double dpr = Double.parseDouble(pixelRatio.toString());
                        log.info("Retrieved device pixel ratio from capabilities: " + dpr);
                        return dpr;
                    }
                } catch (Exception e) {
                    log.warning("Failed to get device pixel ratio from capabilities: " + e.getMessage());
                }
                
                // Default values for common mobile devices
                log.info("Using default device pixel ratio for native app: 1.0");
                return 1.0;
            }
        } catch (Exception e) {
            log.warning("Failed to get device pixel ratio: " + e.getMessage());
            log.warning("Using default device pixel ratio: 1.0");
            return 1.0;
        }
    }

    /**
     * Update cumulative scroll position for native mobile apps
     * This should be called after each scroll operation with the scroll distance
     */
    public void updateScrollPosition(int scrollDistance) {
        // Store in CSS pixels, not device pixels - scaling will be applied in final calculation
        log.info("Updating cumulative scroll position from " + cumulativeScrollPosition + " by adding " + scrollDistance + " CSS pixels");
        this.cumulativeScrollPosition += scrollDistance;
        log.info("New cumulative scroll position: " + cumulativeScrollPosition + " CSS pixels");
    }

    /**
     * Detect platform (iOS/Android, Native/WebView)
     */
    private String detectPlatform() {
        try {
            log.info("Detecting platform...");
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            
            // Use the older API compatible with Selenium 4.1.2
            String platformName = "";
            String browserName = "";
            
            try {
                // Log all available capabilities for debugging
                log.info("Available capabilities: " + caps.asMap().keySet());
                
                // Try to get platform name using the old API
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
                    detectedPlatform = browserName.equals("safari") ? "ios_webview" : "ios_native";
                } else if (platformName.contains("android")) {
                    detectedPlatform = browserName.equals("chrome") ? "android_webview" : "android_native";
                } else {
                    detectedPlatform = "web";
                }
                
                log.info("Detected platform: " + detectedPlatform);
                return detectedPlatform;
                
            } catch (Exception e) {
                log.warning("Error accessing capabilities: " + e.getMessage());
                return "unknown";
            }
            
        } catch (Exception e) {
            log.warning("Failed to detect platform: " + e.getMessage());
            log.warning("Exception type: " + e.getClass().getSimpleName());
            log.warning("Using default platform: unknown");
            return "unknown";
        }
    }

    /**
     * Deduplicate elements based on position proximity
     * For the same XPath, we should only keep one element (the first one detected)
     */
    public List<ElementBoundingBox> deduplicateElements(List<ElementBoundingBox> elements) {
        log.info("Starting deduplication process for " + (elements != null ? elements.size() : 0) + " elements");
        
        if (elements == null || elements.size() <= 1) {
            log.info("No deduplication needed - " + (elements != null ? elements.size() : 0) + " elements");
            return elements;
        }

        List<ElementBoundingBox> uniqueElements = new ArrayList<>();
        Map<String, ElementBoundingBox> firstElementByXPath = new HashMap<>();

        // For each element, keep only the first occurrence of each XPath
        for (ElementBoundingBox element : elements) {
            String xpath = element.getXpath();
            
            if (!firstElementByXPath.containsKey(xpath)) {
                // First time seeing this XPath, keep it
                firstElementByXPath.put(xpath, element);
                uniqueElements.add(element);
                log.info("First occurrence of XPath, keeping element: " + element.toString());
            } else {
                // Already seen this XPath, log and skip
                ElementBoundingBox existing = firstElementByXPath.get(xpath);
                log.info("Duplicate XPath detected, skipping element: " + element.toString());
                log.info("  Original element: " + existing.toString());
                log.info("  Duplicate element: " + element.toString());
            }
        }

        log.info("Deduplication complete: " + elements.size() + " -> " + uniqueElements.size() + " elements");
        log.info("Kept elements by XPath:");
        for (Map.Entry<String, ElementBoundingBox> entry : firstElementByXPath.entrySet()) {
            log.info("  " + entry.getKey() + " -> " + entry.getValue().toString());
        }
        
        return uniqueElements;
    }

    /**
     * Prepare element data for upload
     */
    public Map<String, Object> prepareUploadData(List<ElementBoundingBox> elements) {
        log.info("Preparing upload data for " + (elements != null ? elements.size() : 0) + " elements");
        
        Map<String, Object> uploadData = new HashMap<>();
        
        // Add metadata
        long timestamp = System.currentTimeMillis();
        String platform = detectPlatform();
        
        uploadData.put("timestamp", timestamp);
        uploadData.put("totalElements", elements != null ? elements.size() : 0);
        uploadData.put("platform", platform);
        
        log.info("Upload metadata - timestamp: " + timestamp + ", totalElements: " + (elements != null ? elements.size() : 0) + ", platform: " + platform);
        
        // Add element data
        List<Map<String, Object>> elementData = new ArrayList<>();
        if (elements != null) {
            for (int i = 0; i < elements.size(); i++) {
                ElementBoundingBox element = elements.get(i);
                log.info("Processing element " + (i + 1) + "/" + elements.size() + " for upload: " + element.toString());
                
                Map<String, Object> elementMap = new HashMap<>();
                elementMap.put("xpath", element.getXpath());
                elementMap.put("x", element.getX());
                elementMap.put("y", element.getY());
                elementMap.put("width", element.getWidth());
                elementMap.put("height", element.getHeight());
                elementMap.put("chunkIndex", element.getChunkIndex());
                elementMap.put("timestamp", element.getTimestamp());
                elementMap.put("platform", element.getPlatform());
                
                elementData.add(elementMap);
                log.info("Added element data to upload: " + elementMap.toString());
            }
        }
        
        uploadData.put("elements", elementData);
        
        log.info("Upload data preparation complete. Total elements in upload data: " + elementData.size());
        return uploadData;
    }
} 