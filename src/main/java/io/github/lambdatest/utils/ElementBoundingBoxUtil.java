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
    private Set<String> foundXPaths = new HashSet<>(); // Track XPaths that have already been found

    public ElementBoundingBoxUtil(WebDriver driver) {
        this.driver = driver;
        this.devicePixelRatio = getDevicePixelRatio();
        log.info("ElementBoundingBoxUtil initialized with cumulative scroll position: " + cumulativeScrollPosition + " CSS pixels, device pixel ratio: " + devicePixelRatio);
    }

    /**
     * Detect elements for given XPaths in current viewport
     * @deprecated Use detectElements(Map selectors, int chunkIndex, String purpose) instead
     */
    @Deprecated
    public List<ElementBoundingBox> detectElements(List<String> xpaths, int chunkIndex) {
        // Convert XPaths to the new selector format for backward compatibility
        Map<String, List<String>> selectors = new HashMap<>();
        selectors.put("xpath", xpaths);
        return detectElements(selectors, chunkIndex, "select"); // Default to select for backward compatibility
    }

    /**
     * Detect elements for given selectors in current viewport
     * Supports XPath, class name, accessibility ID, name, ID, and CSS selectors
     * @param selectors Map of selector types to their values
     * @param chunkIndex Current chunk index for tracking
     * @param purpose Purpose of the elements ("ignore" or "select")
     * @return List of detected ElementBoundingBox objects
     */
    public List<ElementBoundingBox> detectElements(Map<String, List<String>> selectors, int chunkIndex, String purpose) {
        List<ElementBoundingBox> detectedElements = new ArrayList<>();
        String platform = detectPlatform();

        log.info("Starting element detection for chunk " + chunkIndex + " with selectors: " + selectors);
        log.info("Detected platform: " + platform);

        if (selectors == null || selectors.isEmpty()) {
            log.info("No selectors provided for element detection");
            return detectedElements;
        }

        // Process each selector type
        for (Map.Entry<String, List<String>> entry : selectors.entrySet()) {
            String selectorType = entry.getKey();
            List<String> selectorValues = entry.getValue();
            
            if (selectorValues == null || selectorValues.isEmpty()) {
                continue;
            }
            
            log.info("Processing " + selectorType + " selectors: " + selectorValues.size() + " items");
            
            for (int i = 0; i < selectorValues.size(); i++) {
                String selectorValue = selectorValues.get(i);
                String selectorKey = selectorType + ":" + selectorValue;
                
                // Skip selector if it has already been found in a previous chunk
                if (foundXPaths.contains(selectorKey)) {
                    log.info("Skipping " + selectorType + " selector " + (i + 1) + "/" + selectorValues.size() + " (already found): " + selectorValue);
                    continue;
                }
                
                log.info("Processing " + selectorType + " selector " + (i + 1) + "/" + selectorValues.size() + ": " + selectorValue);
                
                try {
                    List<WebElement> elements = findElementsBySelector(selectorType, selectorValue);
                    log.info("Found " + elements.size() + " elements for " + selectorType + " selector: " + selectorValue);
                    
                    boolean selectorHasVisibleElements = false;
                    
                    for (int j = 0; j < elements.size(); j++) {
                        WebElement element = elements.get(j);
                        log.info("Processing element " + (j + 1) + "/" + elements.size() + " for " + selectorType + " selector: " + selectorValue);
                        
                        ElementBoundingBox boundingBox = createBoundingBox(element, selectorKey, chunkIndex, platform, purpose);
                        if (boundingBox != null) {
                            log.info("Created bounding box: " + boundingBox.toString());
                            
                            if (isElementFullyInViewport(boundingBox)) {
                                detectedElements.add(boundingBox);
                                selectorHasVisibleElements = true;
                                log.info("Element added to detected list: " + boundingBox.toString());
                            } else {
                                log.info("Element not fully in viewport, skipping: " + boundingBox.toString());
                            }
                        } else {
                            log.warning("Failed to create bounding box for element " + (j + 1) + " of " + selectorType + " selector: " + selectorValue);
                        }
                    }
                    
                    // Only mark selector as found if it has visible elements in the current viewport
                    if (selectorHasVisibleElements) {
                        foundXPaths.add(selectorKey);
                        log.info(selectorType + " selector marked as found (has visible elements), will be skipped in future chunks: " + selectorValue);
                    } else {
                        log.info(selectorType + " selector has elements but none are in viewport, will be checked again in future chunks: " + selectorValue);
                    }
                } catch (Exception e) {
                    log.warning("Failed to detect elements for " + selectorType + " selector '" + selectorValue + "': " + e.getMessage());
                    log.warning("Exception details: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            }
        }

        log.info("Element detection completed for chunk " + chunkIndex + ". Total elements detected: " + detectedElements.size());
        
        // Convert all bounding boxes from CSS pixels to device pixels for final storage
        return convertBoundingBoxesToDevicePixels(detectedElements);
    }

    /**
     * Create bounding box from WebElement with absolute coordinates
     * All computations done in CSS pixels - no device pixel conversion here
     */
    private ElementBoundingBox createBoundingBox(WebElement element, String selectorKey, int chunkIndex, String platform, String purpose) {
        try {
            log.info("Creating bounding box for selector: " + selectorKey);
            
            // Get element location and size relative to viewport (in CSS pixels)
            Point location = element.getLocation();
            Dimension size = element.getSize();
            
            log.info("Element viewport location (CSS pixels): (" + location.getX() + ", " + location.getY() + ")");
            log.info("Element size (CSS pixels): " + size.getWidth() + "x" + size.getHeight());
            
            // Get cumulative scroll position (in CSS pixels)
            int scrollY = getCurrentScrollPosition();
            log.info("Cumulative scroll position (CSS pixels): " + scrollY);
            
            // Calculate absolute page coordinates in CSS pixels
            // Both native and web apps: element Y + scroll position (all in CSS pixels)
            int absoluteX = location.getX();
            int absoluteY = location.getY() + scrollY;
            
            log.info("Coordinate calculation details (CSS pixels):");
            log.info("  Element viewport Y: " + location.getY());
            log.info("  Current scroll Y: " + scrollY);
            log.info("  Absolute Y: " + absoluteY + " (viewport Y + scroll Y)");
            log.info("Calculated absolute coordinates (CSS pixels): (" + absoluteX + ", " + absoluteY + ")");
            
            // Element dimensions are already in CSS pixels
            int width = size.getWidth();
            int height = size.getHeight();
            
            // Store bounding box with CSS pixel coordinates
            ElementBoundingBox boundingBox = new ElementBoundingBox(selectorKey, absoluteX, absoluteY, width, height, chunkIndex, platform, purpose);
            log.info("Successfully created bounding box (CSS pixels): " + boundingBox.toString());
            
            return boundingBox;
            
        } catch (Exception e) {
            log.warning("Failed to create bounding box for element with selector '" + selectorKey + "': " + e.getMessage());
            log.warning("Exception type: " + e.getClass().getSimpleName());
            log.warning("Exception stack trace: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if element is completely within current viewport
     * Works directly with CSS pixel coordinates (no conversion needed)
     */
    private boolean isElementFullyInViewport(ElementBoundingBox boundingBox) {
        try {
            log.info("Checking viewport bounds for element: " + boundingBox.toString());
            
            Dimension viewportSize = driver.manage().window().getSize();
            int scrollY = getCurrentScrollPosition();
            
            log.info("Viewport size: " + viewportSize.getWidth() + "x" + viewportSize.getHeight() + " CSS pixels");
            log.info("Current scroll position: " + scrollY + " CSS pixels");
            
            // Bounding box coordinates are already in CSS pixels
            int cssX = boundingBox.getX();
            int cssY = boundingBox.getY();
            int cssWidth = boundingBox.getWidth();
            int cssHeight = boundingBox.getHeight();
            
            log.info("Element CSS pixel coordinates: (" + cssX + ", " + cssY + ") size: " + cssWidth + "x" + cssHeight);
            
            // Calculate viewport-relative position in CSS pixels
            // cssY contains absolute page position in CSS pixels, so subtract scroll to get viewport position
            int viewportY = cssY - scrollY;
            log.info("Element viewport-relative Y position: " + viewportY + " CSS pixels");
            
            // Check if element is completely within viewport (all in CSS pixels)
            boolean xInBounds = cssX >= 0 && 
                               cssX + cssWidth <= viewportSize.getWidth();
            
            // Y bounds check: element must be within viewport height
            // viewportY >= 0: element is not above the viewport
            // viewportY + cssHeight <= viewportSize.getHeight(): element is not below the viewport
            boolean yInBounds = viewportY >= 0 && 
                               viewportY + cssHeight <= viewportSize.getHeight();
            
            // Additional checks for edge cases
            boolean elementAboveViewport = viewportY < 0;
            boolean elementBelowViewport = viewportY + cssHeight > viewportSize.getHeight();
            
            log.info("X bounds check: " + xInBounds + " (CSS X: " + cssX + ", CSS width: " + cssWidth + ", viewport width: " + viewportSize.getWidth() + ")");
            log.info("Y bounds check: " + yInBounds + " (CSS viewport Y: " + viewportY + ", CSS height: " + cssHeight + ", viewport height: " + viewportSize.getHeight() + ")");
            log.info("Element above viewport: " + elementAboveViewport + " (viewportY < 0: " + viewportY + " < 0)");
            log.info("Element below viewport: " + elementBelowViewport + " (viewportY + height > viewportHeight: " + (viewportY + cssHeight) + " > " + viewportSize.getHeight() + ")");
            
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
            } else if (platform.contains("ios")) {
                // For iOS devices, use hardcoded DPR map
                return getIOSDevicePixelRatio();
            } else {
                // For Android native apps, try to get from capabilities or use default
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
                
                // Default values for common Android devices
                log.info("Using default device pixel ratio for Android app: 1.0");
                return 1.0;
            }
        } catch (Exception e) {
            log.warning("Failed to get device pixel ratio: " + e.getMessage());
            log.warning("Using default device pixel ratio: 1.0");
            return 1.0;
        }
    }

    /**
     * Get iOS device pixel ratio from hardcoded map based on ios-resolution.com
     */
    private double getIOSDevicePixelRatio() {
        try {
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            String deviceName = "";
            String model = "";
            
            // Try to get device information from capabilities
            if (caps.getCapability("deviceName") != null) {
                deviceName = caps.getCapability("deviceName").toString().toLowerCase();
            }
            if (caps.getCapability("model") != null) {
                model = caps.getCapability("model").toString().toLowerCase();
            }
            
            log.info("iOS device detection - deviceName: " + deviceName + ", model: " + model);
            
            // iPhone DPR Map based on ios-resolution.com scale factors
            if (deviceName.contains("iphone") || model.contains("iphone")) {
                // iPhone 16 series (Scale Factor: 3)
                if (deviceName.contains("16") || model.contains("16")) {
                    log.info("Detected iPhone 16 series, using DPR: 3.0 (Scale Factor: 3)");
                    return 3.0;
                }
                // iPhone 15 series (Scale Factor: 3)
                if (deviceName.contains("15") || model.contains("15")) {
                    log.info("Detected iPhone 15 series, using DPR: 3.0 (Scale Factor: 3)");
                    return 3.0;
                }
                // iPhone 14 series (Scale Factor: 3)
                if (deviceName.contains("14") || model.contains("14")) {
                    log.info("Detected iPhone 14 series, using DPR: 3.0 (Scale Factor: 3)");
                    return 3.0;
                }
                // iPhone 13 series (Scale Factor: 3)
                if (deviceName.contains("13") || model.contains("13")) {
                    log.info("Detected iPhone 13 series, using DPR: 3.0 (Scale Factor: 3)");
                    return 3.0;
                }
                // iPhone 12 series (Scale Factor: 3)
                if (deviceName.contains("12") || model.contains("12")) {
                    log.info("Detected iPhone 12 series, using DPR: 3.0 (Scale Factor: 3)");
                    return 3.0;
                }
                // iPhone 11 series (Scale Factor: 3)
                if (deviceName.contains("11") || model.contains("11")) {
                    log.info("Detected iPhone 11 series, using DPR: 3.0 (Scale Factor: 3)");
                    return 3.0;
                }
                // iPhone X series (Scale Factor: 3)
                if (deviceName.contains("x") || model.contains("x")) {
                    log.info("Detected iPhone X series, using DPR: 3.0 (Scale Factor: 3)");
                    return 3.0;
                }
                // iPhone 8 series (Scale Factor: 3)
                if (deviceName.contains("8") || model.contains("8")) {
                    log.info("Detected iPhone 8 series, using DPR: 3.0 (Scale Factor: 3)");
                    return 3.0;
                }
                // iPhone 7 series (Scale Factor: 3)
                if (deviceName.contains("7") || model.contains("7")) {
                    log.info("Detected iPhone 7 series, using DPR: 3.0 (Scale Factor: 3)");
                    return 3.0;
                }
                // iPhone 6 Plus (Scale Factor: 3)
                if (deviceName.contains("6 plus") || model.contains("6 plus")) {
                    log.info("Detected iPhone 6 Plus, using DPR: 3.0 (Scale Factor: 3)");
                    return 3.0;
                }
                // iPhone 6 (Scale Factor: 2)
                if (deviceName.contains("6") || model.contains("6")) {
                    log.info("Detected iPhone 6, using DPR: 2.0 (Scale Factor: 2)");
                    return 2.0;
                }
                // iPhone 5 series (Scale Factor: 2)
                if (deviceName.contains("5") || model.contains("5")) {
                    log.info("Detected iPhone 5 series, using DPR: 2.0 (Scale Factor: 2)");
                    return 2.0;
                }
                // iPhone 4 series (Scale Factor: 2)
                if (deviceName.contains("4") || model.contains("4")) {
                    log.info("Detected iPhone 4 series, using DPR: 2.0 (Scale Factor: 2)");
                    return 2.0;
                }
                // iPhone SE series (Scale Factor: 2)
                if (deviceName.contains("se") || model.contains("se")) {
                    log.info("Detected iPhone SE series, using DPR: 2.0 (Scale Factor: 2)");
                    return 2.0;
                }
                // iPhone 3GS and earlier (Scale Factor: 1)
                if (deviceName.contains("3") || model.contains("3")) {
                    log.info("Detected iPhone 3GS or earlier, using DPR: 1.0 (Scale Factor: 1)");
                    return 1.0;
                }
                // Future iPhone models (Scale Factor: 3) - for iPhone 17+ and beyond
                if (extractIPhoneNumber(deviceName, model) > 16) {
                    log.info("Detected future iPhone model (17+), using DPR: 3.0 (Scale Factor: 3)");
                    return 3.0;
                }
                // Default for other iPhones (Scale Factor: 3)
                log.info("Detected iPhone (unknown model), using DPR: 3.0 (Scale Factor: 3)");
                return 3.0;
            }
            
            // iPad DPR Map based on ios-resolution.com scale factors
            if (deviceName.contains("ipad") || model.contains("ipad")) {
                // iPad Pro series (Scale Factor: 2)
                if (deviceName.contains("pro") || model.contains("pro")) {
                    log.info("Detected iPad Pro series, using DPR: 2.0 (Scale Factor: 2)");
                    return 2.0;
                }
                // iPad Air series (Scale Factor: 2)
                if (deviceName.contains("air") || model.contains("air")) {
                    log.info("Detected iPad Air series, using DPR: 2.0 (Scale Factor: 2)");
                    return 2.0;
                }
                // iPad mini series (Scale Factor: 2)
                if (deviceName.contains("mini") || model.contains("mini")) {
                    log.info("Detected iPad mini series, using DPR: 2.0 (Scale Factor: 2)");
                    return 2.0;
                }
                // iPad (regular) series (Scale Factor: 2)
                if (deviceName.contains("ipad") || model.contains("ipad")) {
                    log.info("Detected iPad (regular) series, using DPR: 2.0 (Scale Factor: 2)");
                    return 2.0;
                }
                // Default for other iPads (Scale Factor: 2)
                log.info("Detected iPad (unknown model), using DPR: 2.0 (Scale Factor: 2)");
                return 2.0;
            }
            
            // iPod touch DPR Map based on ios-resolution.com scale factors
            if (deviceName.contains("ipod") || model.contains("ipod")) {
                // iPod touch 5th gen and later (Scale Factor: 2)
                if (deviceName.contains("5") || model.contains("5") || 
                    deviceName.contains("6") || model.contains("6") || 
                    deviceName.contains("7") || model.contains("7")) {
                    log.info("Detected iPod touch 5th gen+, using DPR: 2.0 (Scale Factor: 2)");
                    return 2.0;
                }
                // iPod touch 4th gen and earlier (Scale Factor: 1)
                log.info("Detected iPod touch 4th gen or earlier, using DPR: 1.0 (Scale Factor: 1)");
                return 1.0;
            }
            
            // Fallback for iOS devices
            log.info("iOS device detected but model unknown, using DPR: 3.0 (Scale Factor: 3)");
            return 3.0;
            
        } catch (Exception e) {
            log.warning("Failed to get iOS device pixel ratio: " + e.getMessage());
            log.info("Using default iOS DPR: 3.0 (Scale Factor: 3)");
            return 3.0;
        }
    }

    /**
     * Extract iPhone number from device name or model for future device detection
     */
    private int extractIPhoneNumber(String deviceName, String model) {
        try {
            // Look for patterns like "iPhone 17", "iPhone17", "17 Pro", etc.
            String combined = (deviceName + " " + model).toLowerCase();
            
            // Extract numbers that could be iPhone versions
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("iphone\\s*(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(combined);
            
            if (matcher.find()) {
                int version = Integer.parseInt(matcher.group(1));
                log.info("Extracted iPhone version: " + version);
                return version;
            }
            
            // If no specific iPhone pattern, try to find standalone numbers that might be versions
            pattern = java.util.regex.Pattern.compile("\\b(\\d{2,})\\b");
            matcher = pattern.matcher(combined);
            
            if (matcher.find()) {
                int version = Integer.parseInt(matcher.group(1));
                // Only consider reasonable iPhone version numbers (17+ for future devices)
                if (version >= 17) {
                    log.info("Extracted potential future iPhone version: " + version);
                    return version;
                }
            }
            
            return 0; // No version found
        } catch (Exception e) {
            log.warning("Failed to extract iPhone number: " + e.getMessage());
            return 0;
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
                
                if (caps.getCapability("platformName") != null) {
                    platformName = caps.getCapability("platformName").toString().toLowerCase();
                } else if (caps.getCapability("platform") != null) {
                    platformName = caps.getCapability("platform").toString().toLowerCase();

                } else {
                    log.warning("No platform capability found");
                }
                
                if (caps.getCapability("browserName") != null) {
                    browserName = caps.getCapability("browserName").toString().toLowerCase();
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
     * Convert a list of bounding boxes from CSS pixels to device pixels
     */
    private List<ElementBoundingBox> convertBoundingBoxesToDevicePixels(List<ElementBoundingBox> cssElements) {
        List<ElementBoundingBox> devicePixelElements = new ArrayList<>();
        for (ElementBoundingBox cssElement : cssElements) {
            int deviceX = (int) (cssElement.getX() * devicePixelRatio);
            int deviceY = (int) (cssElement.getY() * devicePixelRatio);
            int deviceWidth = (int) (cssElement.getWidth() * devicePixelRatio);
            int deviceHeight = (int) (cssElement.getHeight() * devicePixelRatio);

            ElementBoundingBox deviceElement = new ElementBoundingBox(
                cssElement.getSelectorKey(),
                deviceX,
                deviceY,
                deviceWidth,
                deviceHeight,
                cssElement.getChunkIndex(),
                cssElement.getPlatform(),
                cssElement.getPurpose()
            );

            devicePixelElements.add(deviceElement);
            log.info("Converted CSS pixel element to device pixel element: " + deviceElement.toString());
        }
        log.info("Converted " + cssElements.size() + " elements from CSS pixels to device pixels");
        return devicePixelElements;
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
                
                // Add selector information
                elementMap.put("selectorType", element.getSelectorType());
                elementMap.put("selectorValue", element.getSelectorValue());
                elementMap.put("selectorKey", element.getSelectorKey());
                
                // Keep xpath for backward compatibility
                elementMap.put("xpath", element.getXpath());
                
                elementMap.put("x", element.getX());
                elementMap.put("y", element.getY());
                elementMap.put("width", element.getWidth());
                elementMap.put("height", element.getHeight());
                elementMap.put("chunkIndex", element.getChunkIndex());
                elementMap.put("timestamp", element.getTimestamp());
                elementMap.put("platform", element.getPlatform());
                elementMap.put("purpose", element.getPurpose());
                
                elementData.add(elementMap);
                log.info("Added element data to upload: " + elementMap.toString());
            }
        }
        
        uploadData.put("elements", elementData);
        
        log.info("Upload data preparation complete. Total elements in upload data: " + elementData.size());
        return uploadData;
    }

    /**
     * Find elements using different selector types
     */
    private List<WebElement> findElementsBySelector(String selectorType, String selectorValue) {
        switch (selectorType.toLowerCase()) {
            case "xpath":
                return driver.findElements(By.xpath(selectorValue));
            case "class":
                return driver.findElements(By.className(selectorValue));
            case "accessibilityid":
            case "accessibility_id":
                return driver.findElements(By.xpath("//*[@content-desc='" + selectorValue + "']"));
            case "name":
                return driver.findElements(By.name(selectorValue));
            case "id":
                return driver.findElements(By.id(selectorValue));
            case "css":
                return driver.findElements(By.cssSelector(selectorValue));
            default:
                log.warning("Unsupported selector type: " + selectorType + ", falling back to XPath");
                return driver.findElements(By.xpath(selectorValue));
        }
    }

    /**
     * iOS-optimized: Detect all elements once at start with absolute positions
     * No viewport filtering needed since iOS makes all elements accessible
     */
    public List<ElementBoundingBox> detectAllElementsIOS(Map<String, List<String>> selectors, String purpose) {
        try {
            log.info("iOS optimization: Detecting all elements once at start with absolute positions");
            List<ElementBoundingBox> allElements = new ArrayList<>();
            String platform = detectPlatform();
            
            if (selectors == null || selectors.isEmpty()) {
                log.info("No selectors provided for iOS element detection");
                return allElements;
            }
            
            // Process each selector type
            for (Map.Entry<String, List<String>> entry : selectors.entrySet()) {
                String selectorType = entry.getKey();
                List<String> selectorValues = entry.getValue();
                
                if (selectorValues == null || selectorValues.isEmpty()) {
                    continue;
                }
                
                log.info("iOS: Processing " + selectorType + " selectors: " + selectorValues.size() + " items");
                
                for (String selectorValue : selectorValues) {
                    String selectorKey = selectorType + ":" + selectorValue;
                    
                    try {
                        List<WebElement> elements = findElementsBySelector(selectorType, selectorValue);
                        log.info("iOS: Found " + elements.size() + " elements for " + selectorType + " selector: " + selectorValue);
                        
                        for (WebElement element : elements) {
                            // Create bounding box with absolute position (no scroll offset needed for iOS)
                            ElementBoundingBox boundingBox = createBoundingBox(element, selectorKey, 0, platform, purpose);
                            
                            if (boundingBox != null) {
                                allElements.add(boundingBox);
                                log.info("iOS: Added element with absolute position: " + boundingBox.toString());
                            } else {
                                log.warning("iOS: Failed to create bounding box for element: " + selectorKey);
                            }
                        }
                    } catch (Exception e) {
                        log.warning("iOS: Failed to detect elements for " + selectorType + " (" + selectorValue + "): " + e.getMessage());
                    }
                }
            }
            
            log.info("iOS optimization: Detected " + allElements.size() + " elements with absolute positions");
            
            // Convert CSS pixels to device pixels for iOS elements (same as Android/Web)
            List<ElementBoundingBox> devicePixelElements = convertBoundingBoxesToDevicePixels(allElements);
            log.info("iOS optimization: Converted " + allElements.size() + " elements from CSS pixels to device pixels using DPR: " + devicePixelRatio);
            
            return devicePixelElements;
            
        } catch (Exception e) {
            log.severe("iOS element detection failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

} 