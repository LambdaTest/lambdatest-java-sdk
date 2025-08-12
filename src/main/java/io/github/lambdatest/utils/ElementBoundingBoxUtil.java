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
    private final String testType; // Store test type (web or app)
    private final String deviceName; // Store device name

    public ElementBoundingBoxUtil(WebDriver driver) {
        this.driver = driver;
        this.testType = "web"; // Default to web for backward compatibility
        this.deviceName = detectDeviceName();
        this.devicePixelRatio = getDevicePixelRatio();
        log.info("ElementBoundingBoxUtil initialized with cumulative scroll position: " + cumulativeScrollPosition + " CSS pixels, device pixel ratio: " + devicePixelRatio + ", testType: " + testType + ", deviceName: " + deviceName);
    }

    public ElementBoundingBoxUtil(WebDriver driver, String testType, String deviceName) {
        this.driver = driver;
        this.testType = testType;
        this.deviceName = deviceName;
        this.devicePixelRatio = getDevicePixelRatio();
        log.info("ElementBoundingBoxUtil initialized with cumulative scroll position: " + cumulativeScrollPosition + " CSS pixels, device pixel ratio: " + devicePixelRatio + ", testType: " + testType + ", deviceName: " + deviceName);
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
                // For web platforms, we might want to be less strict about this
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
     * For web platforms, uses JavaScript for more accurate positioning
     */
    private ElementBoundingBox createBoundingBox(WebElement element, String selectorKey, int chunkIndex, String platform, String purpose) {
        try {
            log.info("Creating bounding box for selector: " + selectorKey);
            
            Point location;
            Dimension size;
            
            if (platform.contains("web")) {
                // Use JavaScript for web platforms - more accurate positioning
                Map<String, Object> elementData = getElementBoundingBoxWeb(element);
                location = new Point(((Number) elementData.get("x")).intValue(), ((Number) elementData.get("y")).intValue());
                size = new Dimension(((Number) elementData.get("width")).intValue(), ((Number) elementData.get("height")).intValue());
                log.info("Using JavaScript for web element positioning");
            } else {
                // Use standard Selenium methods for mobile platforms
                location = element.getLocation();
                size = element.getSize();
                log.info("Using standard Selenium for mobile element positioning");
            }
            
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
     * Check if element is visible in current viewport
     * For web platforms: element must be at least partially visible
     * For mobile platforms: element must be fully visible (stricter)
     */
    private boolean isElementFullyInViewport(ElementBoundingBox boundingBox) {
        try {
            log.info("Checking viewport bounds for element: " + boundingBox.toString());
            
            Dimension viewportSize = driver.manage().window().getSize();
            int scrollY = getCurrentScrollPosition();
            String platform = detectPlatform();
            
            log.info("Viewport size: " + viewportSize.getWidth() + "x" + viewportSize.getHeight() + " CSS pixels");
            log.info("Current scroll position: " + scrollY + " CSS pixels");
            log.info("Platform: " + platform);
            
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
            
            // X bounds check: element must be within viewport width
            boolean xInBounds = cssX >= 0 && 
                               cssX + cssWidth <= viewportSize.getWidth();
            
            // Y bounds check: different logic for web vs mobile
            boolean yInBounds;
            if (platform.contains("web")) {
                // For web: element must be at least partially visible (more lenient)
                // Element is visible if any part of it is in the viewport
                boolean elementPartiallyVisible = (viewportY < viewportSize.getHeight()) && 
                                                (viewportY + cssHeight > 0);
                yInBounds = elementPartiallyVisible;
                log.info("Web platform: Using partial visibility check - element partially visible: " + elementPartiallyVisible);
            } else {
                // For mobile: element must be fully visible (stricter)
                yInBounds = viewportY >= 0 && 
                           viewportY + cssHeight <= viewportSize.getHeight();
                log.info("Mobile platform: Using full visibility check");
            }
            
            // Additional checks for edge cases
            boolean elementAboveViewport = viewportY < 0;
            boolean elementBelowViewport = viewportY + cssHeight > viewportSize.getHeight();
            
            log.info("X bounds check: " + xInBounds + " (CSS X: " + cssX + ", CSS width: " + cssWidth + ", viewport width: " + viewportSize.getWidth() + ")");
            log.info("Y bounds check: " + yInBounds + " (CSS viewport Y: " + viewportY + ", CSS height: " + cssHeight + ", viewport height: " + viewportSize.getHeight() + ")");
            log.info("Element above viewport: " + elementAboveViewport + " (viewportY < 0: " + viewportY + " < 0)");
            log.info("Element below viewport: " + elementBelowViewport + " (CSS viewport Y + height > viewportHeight: " + (viewportY + cssHeight) + " > " + viewportSize.getHeight() + ")");
            
            boolean isVisible = xInBounds && yInBounds;
            log.info("Element visible in viewport: " + isVisible);
            
            return isVisible;
                   
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
                    Object scrollResult = ((JavascriptExecutor) driver).executeScript(
                        "return window.pageYOffset || document.documentElement.scrollTop || 0;"
                    );
                    
                    int scrollPosition;
                    if (scrollResult instanceof Number) {
                        scrollPosition = ((Number) scrollResult).intValue();
                    } else if (scrollResult instanceof String) {
                        scrollPosition = Integer.parseInt((String) scrollResult);
                    } else {
                        log.warning("Unexpected scroll position type: " + scrollResult.getClass().getSimpleName() + ", using tracked position: " + cumulativeScrollPosition);
                        scrollPosition = cumulativeScrollPosition;
                    }
                    
                    log.info("Retrieved scroll position from JavaScript: " + scrollPosition);
                    return scrollPosition;
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
                    Object dprResult = ((JavascriptExecutor) driver).executeScript(
                        "return window.devicePixelRatio || 1;"
                    );
                    
                    double dpr;
                    if (dprResult instanceof Number) {
                        dpr = ((Number) dprResult).doubleValue();
                    } else if (dprResult instanceof String) {
                        dpr = Double.parseDouble((String) dprResult);
                    } else {
                        log.warning("Unexpected device pixel ratio type: " + dprResult.getClass().getSimpleName() + ", using default: 1.0");
                        dpr = 1.0;
                    }
                    
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
     * Get status bar height for iOS devices in CSS pixels
     * Similar to Golang implementation with device-specific height detection
     */
    public int getStatusBarHeightForIOS() {
        try {
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            String deviceName = "";
            
            // Try to get device information from capabilities
            if (caps.getCapability("deviceName") != null) {
                deviceName = caps.getCapability("deviceName").toString();
            }
            
            if (deviceName.isEmpty()) {
                log.info("No device name found, using default status bar height: 20 CSS pixels");
                return 20;
            }
            
            String lowerName = deviceName.toLowerCase();
            log.info("Calculating status bar height for device: " + deviceName);
            
            // iPad handling
            if (lowerName.contains("ipad")) {
                int height = getIPadStatusBarHeight(lowerName);
                log.info("iPad status bar height: " + height + " CSS pixels");
                return height;
            }
            
            if (lowerName.contains("iphone")) {
                int height = getIPhoneStatusBarHeight(lowerName);
                log.info("iPhone status bar height: " + height + " CSS pixels");
                return height;
            }
            
            log.info("Unknown iOS device, using default status bar height: 20 CSS pixels");
            return 20;
            
        } catch (Exception e) {
            log.warning("Failed to get iOS status bar height: " + e.getMessage());
            log.info("Using default status bar height: 20 CSS pixels");
            return 20;
        }
    }

    /**
     * Get iPad status bar height in CSS pixels
     */
    private int getIPadStatusBarHeight(String deviceName) {
        // Newer iPad Pro models have taller status bars
        if (deviceName.contains("pro")) {
            String[] newerYears = {"2018", "2020", "2021", "2022", "2024", "2025"};
            for (String year : newerYears) {
                if (deviceName.contains(year)) {
                    log.info("Detected newer iPad Pro (" + year + "), using status bar height: 24 CSS pixels");
                    return 24;
                }
            }
        }
        log.info("Detected iPad (regular or older Pro), using status bar height: 20 CSS pixels");
        return 20;
    }

    /**
     * Get iPhone status bar height in CSS pixels
     */
    private int getIPhoneStatusBarHeight(String deviceName) {
        if (hasDynamicIsland(deviceName)) {
            log.info("Detected iPhone with Dynamic Island, using status bar height: 54 CSS pixels");
            return 54;
        }
        
        if (hasNotch(deviceName)) {
            log.info("Detected iPhone with notch, using status bar height: 47 CSS pixels");
            return 47;
        }
        
        if (isTraditionalIPhone(deviceName)) {
            log.info("Detected traditional iPhone, using status bar height: 20 CSS pixels");
            return 20;
        }
        
        log.info("Unknown iPhone model, using default status bar height: 20 CSS pixels");
        return 20;
    }

    /**
     * Check if the iPhone has Dynamic Island
     */
    private boolean hasDynamicIsland(String deviceName) {
        String[] dynamicIslandDevices = {
            "14 pro", "14pro",
            "15",
            "16",
            "17" // All iPhone 17 models expected to have Dynamic Island
        };
        
        for (String device : dynamicIslandDevices) {
            if (deviceName.contains(device)) {
                log.info("Detected Dynamic Island device: " + device);
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the iPhone has notch
     */
    private boolean hasNotch(String deviceName) {
        String[] notchDevices = {
            "x", "xs", "xr",
            "11",
            "12",
            "13",
            "14"
        };
        
        for (String device : notchDevices) {
            if (deviceName.contains(device)) {
                // Special case: iPhone 14 Pro models are handled by Dynamic Island
                if (device.equals("14") && (deviceName.contains("pro") || deviceName.contains("14 pro"))) {
                    continue;
                }
                log.info("Detected notch device: " + device);
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the iPhone is a traditional model without notch
     */
    private boolean isTraditionalIPhone(String deviceName) {
        String[] traditionalDevices = {
            "se", "8", "7", "6", "5", "4"
        };
        
        for (String device : traditionalDevices) {
            if (deviceName.contains(device)) {
                log.info("Detected traditional iPhone: " + device);
                return true;
            }
        }
        return false;
    }

    /**
     * Check if status bar height should be added for web tests on iOS devices
     */
    private boolean shouldAddStatusBarHeight() {
        try {
            log.info("Checking status bar height - testType: " + testType + ", deviceName: " + deviceName);
            
            // Only apply status bar height for web tests on iOS devices
            if (testType.toLowerCase().contains("web") && (deviceName.toLowerCase().contains("iphone") || deviceName.toLowerCase().contains("ipad") || deviceName.toLowerCase().contains("ipod"))) {
                log.info("Web test on iOS device detected: " + deviceName + " - status bar height adjustment will be applied");
                return true;
            }
            
            log.info("Not a web test on iOS device, no status bar height adjustment needed");
            return false;
            
        } catch (Exception e) {
            log.warning("Failed to check if status bar height should be added: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get status bar height in device pixels for web tests on iOS devices
     * This method should be used when testType is web and deviceName is iOS device
     * Returns status bar height * device pixel ratio for proper scaling
     */
    public int getStatusBarHeightInDevicePixels() {
        try {
            if (shouldAddStatusBarHeight()) {
                int cssStatusBarHeight = getStatusBarHeightForIOS();
                int devicePixelStatusBarHeight = (int) (cssStatusBarHeight * devicePixelRatio);
                log.info("Web test on iOS device: Status bar height " + cssStatusBarHeight + " CSS pixels * DPR " + devicePixelRatio + " = " + devicePixelStatusBarHeight + " device pixels");
                return devicePixelStatusBarHeight;
            }
            
            log.info("Status bar height adjustment not needed, returning 0");
            return 0;
            
        } catch (Exception e) {
            log.warning("Failed to get status bar height in device pixels: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if Android Chrome address bar height should be added for web tests on Android devices
     */
    private boolean shouldAddAndroidChromeBarHeight() {
        try {
            log.info("Checking Android Chrome bar height - testType: " + testType + ", deviceName: " + deviceName);
            
            // Only apply Chrome bar height for web tests on Android devices
            if (testType.toLowerCase().contains("web") && 
                (deviceName.toLowerCase().contains("android") || 
                 detectPlatform().toLowerCase().contains("android"))) {
                log.info("Web test on Android device detected: " + deviceName + " - Chrome address bar height adjustment will be applied");
                return true;
            }
            
            log.info("Not a web test on Android device, no Chrome address bar height adjustment needed");
            return false;
            
        } catch (Exception e) {
            log.warning("Failed to check if Android Chrome address bar height should be added: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get Chrome address bar height for Android devices in CSS pixels
     * Uses JavaScript to calculate the difference between screen height and viewport height
     */
    public int getChromeAddressBarHeightForAndroid() {
        try {
            if (!shouldAddAndroidChromeBarHeight()) {
                log.info("Android Chrome address bar height not needed, returning 0");
                return 0;
            }
            
            log.info("Calculating Chrome address bar height for Android device: " + deviceName);
            
            // Use JavaScript to get the address bar height (screen height - viewport height)
            String script = 
                "function getChromeBarHeight() {" +
                "    // Get screen height vs viewport height difference" +
                "    const screenHeight = window.screen.height;" +
                "    const viewportHeight = window.innerHeight;" +
                "    const addressBarHeight = screenHeight - viewportHeight;" +
                "    " +
                "    return addressBarHeight;" +
                "}" +
                "return getChromeBarHeight();";
            
            Object result = ((JavascriptExecutor) driver).executeScript(script);
            
            int chromeBarHeight = 0;
            if (result instanceof Number) {
                chromeBarHeight = ((Number) result).intValue();
            } else if (result instanceof String) {
                chromeBarHeight = Integer.parseInt((String) result);
            } else {
                log.warning("Unexpected Chrome bar height result type: " + (result != null ? result.getClass().getSimpleName() : "null"));
                chromeBarHeight = getDefaultAndroidChromeBarHeight();
            }
            
            // Validate the result (should be reasonable for a mobile device)
            if (chromeBarHeight < 0 || chromeBarHeight > 200) {
                log.warning("Chrome bar height seems unreasonable: " + chromeBarHeight + " pixels, using default");
                chromeBarHeight = getDefaultAndroidChromeBarHeight();
            }
            
            log.info("Chrome address bar height for Android: " + chromeBarHeight + " CSS pixels");
            return chromeBarHeight;
            
        } catch (Exception e) {
            log.warning("Failed to get Chrome address bar height from JavaScript: " + e.getMessage());
            int defaultHeight = getDefaultAndroidChromeBarHeight();
            log.info("Using default Android Chrome bar height: " + defaultHeight + " CSS pixels");
            return defaultHeight;
        }
    }

    /**
     * Get default Chrome address bar height for Android devices
     * Fallback values based on common Android Chrome configurations
     */
    private int getDefaultAndroidChromeBarHeight() {
        try {
            // Common Chrome address bar heights on Android (in CSS pixels)
            // These values account for status bar + Chrome address bar
            
            // Try to get Android version for better estimation
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            String androidVersion = "";
            if (caps.getCapability("platformVersion") != null) {
                androidVersion = caps.getCapability("platformVersion").toString();
            }
            
            // Android 11+ typically has:
            // - Status bar: 24px
            // - Chrome address bar: ~56px
            // Total: ~80px
            if (androidVersion.startsWith("11") || androidVersion.startsWith("12") || 
                androidVersion.startsWith("13") || androidVersion.startsWith("14")) {
                log.info("Android 11+ detected, using Chrome bar height: 80 CSS pixels");
                return 80;
            }
            
            // Android 10 typically has similar heights
            if (androidVersion.startsWith("10")) {
                log.info("Android 10 detected, using Chrome bar height: 80 CSS pixels");
                return 80;
            }
            
            // Android 9 and below might have slightly different heights
            if (androidVersion.startsWith("9") || androidVersion.startsWith("8") || 
                androidVersion.startsWith("7")) {
                log.info("Android 9 or below detected, using Chrome bar height: 76 CSS pixels");
                return 76;
            }
            
            // Default for unknown versions
            log.info("Unknown Android version, using default Chrome bar height: 80 CSS pixels");
            return 80;
            
        } catch (Exception e) {
            log.warning("Failed to determine default Chrome bar height: " + e.getMessage());
            log.info("Using fallback Chrome bar height: 80 CSS pixels");
            return 80; // Safe fallback
        }
    }

    /**
     * Get Chrome address bar height in device pixels for web tests on Android devices
     * This method should be used when testType is web and platform is Android
     * Returns Chrome bar height * device pixel ratio for proper scaling
     */
    public int getAndroidChromeBarHeightInDevicePixels() {
        try {
            if (shouldAddAndroidChromeBarHeight()) {
                int cssChromeBarHeight = getChromeAddressBarHeightForAndroid();
                int devicePixelChromeBarHeight = (int) (cssChromeBarHeight * devicePixelRatio);
                log.info("Web test on Android device: Chrome bar height " + cssChromeBarHeight + " CSS pixels * DPR " + devicePixelRatio + " = " + devicePixelChromeBarHeight + " device pixels");
                return devicePixelChromeBarHeight;
            }
            
            log.info("Android Chrome bar height adjustment not needed, returning 0");
            return 0;
            
        } catch (Exception e) {
            log.warning("Failed to get Android Chrome bar height in device pixels: " + e.getMessage());
            return 0;
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
                    deviceName = detectPlatform(); // Use platform as fallback
                }
                
                return deviceName;
                
            } catch (Exception e) {
                log.warning("Error accessing device capabilities: " + e.getMessage());
                return detectPlatform(); // Default to platform name
            }
            
        } catch (Exception e) {
            log.warning("Failed to detect device name: " + e.getMessage());
            return "unknown"; // Default to unknown
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
        
        // Check if we need to add status bar height for web tests on iOS devices
        int statusBarHeightToAdd = 0;
        
        if (shouldAddStatusBarHeight()) {
            statusBarHeightToAdd = getStatusBarHeightInDevicePixels();
            log.info("Web test on iOS device detected - will add status bar height: " + statusBarHeightToAdd + " device pixels to element Y coordinates");
        }
        
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
                
                // Add status bar height to Y coordinate if needed
                int adjustedY = element.getY();
                if (shouldAddStatusBarHeight()) {
                    adjustedY += statusBarHeightToAdd;
                    log.info("Element " + (i + 1) + ": Adjusted Y from " + element.getY() + " to " + adjustedY + " (added " + statusBarHeightToAdd + " device pixels for status bar)");
                }
                
                elementMap.put("x", element.getX());
                elementMap.put("y", adjustedY);
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
        if (shouldAddStatusBarHeight()) {
            log.info("Status bar height adjustment applied: " + statusBarHeightToAdd + " device pixels added to Y coordinates");
        }
        return uploadData;
    }

    /**
     * Find elements using different selector types
     * For web platforms, uses JavaScript for more reliable element detection
     */
    private List<WebElement> findElementsBySelector(String selectorType, String selectorValue) {
        String platform = detectPlatform();
        
        if (platform.contains("web")) {
            // Use JavaScript for web platforms - more reliable for XPath and other selectors
            return findElementsBySelectorWeb(selectorType, selectorValue);
        } else {
            // Use standard Selenium methods for mobile platforms
            return findElementsBySelectorMobile(selectorType, selectorValue);
        }
    }

    /**
     * Find elements using JavaScript for web platforms
     * Uses the same approach as Golang implementation with getBoundingClientRect
     */
    private List<WebElement> findElementsBySelectorWeb(String selectorType, String selectorValue) {
        try {
            List<WebElement> elements = new ArrayList<>();
            
            // Build JavaScript script based on selector type (similar to Golang implementation)
            String script = buildDOMBoxScript(selectorType, selectorValue);
            
            if (script.isEmpty()) {
                log.warning("Unsupported selector type for web: " + selectorType + ", falling back to standard method");
                return findElementsBySelectorMobile(selectorType, selectorValue);
            }
            
            log.info("Using JavaScript for web element detection: " + selectorType + " = " + selectorValue);
            
            // Execute JavaScript to get bounding rect
            Object result = ((JavascriptExecutor) driver).executeScript(script);
            
            if (result != null) {
                // If we got a bounding rect, the element exists - create a WebElement reference
                WebElement element = createWebElementFromSelector(selectorType, selectorValue);
                if (element != null) {
                    elements.add(element);
                }
            }
            
            log.info("JavaScript found " + elements.size() + " elements for " + selectorType + " selector: " + selectorValue);
            return elements;
            
        } catch (Exception e) {
            log.warning("JavaScript element detection failed for " + selectorType + " (" + selectorValue + "): " + e.getMessage());
            log.info("Falling back to standard Selenium method");
            return findElementsBySelectorMobile(selectorType, selectorValue);
        }
    }

    /**
     * Build JavaScript script for getting element bounding rect (similar to Golang implementation)
     * Ignores index parameter as requested
     */
    private String buildDOMBoxScript(String selectorType, String elementSelector) {
        switch (selectorType.toLowerCase()) {
            case "class":
                return "return document.getElementsByClassName(`" + elementSelector + "`)[0].getBoundingClientRect();";
            case "id":
                return "return document.getElementById(`" + elementSelector + "`).getBoundingClientRect();";
            case "xpath":
                return "var element = document.evaluate(`" + elementSelector + "`, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null).snapshotItem(0); return element ? element.getBoundingClientRect() : null;";
            case "css":
                return "return document.querySelectorAll(`" + elementSelector + "`)[0].getBoundingClientRect();";
            case "name":
                return "return document.getElementsByName(`" + elementSelector + "`)[0].getBoundingClientRect();";
            case "accessibilityid":
            case "accessibility_id":
                // For web, accessibility ID is typically aria-label or title
                return "var element = document.querySelector('[aria-label=\"" + elementSelector + "\"], [title=\"" + elementSelector + "\"], [content-desc=\"" + elementSelector + "\"]'); return element ? element.getBoundingClientRect() : null;";
            default:
                return "";
        }
    }

    /**
     * Create a WebElement reference from selector for the found element
     */
    private WebElement createWebElementFromSelector(String selectorType, String selectorValue) {
        try {
            switch (selectorType.toLowerCase()) {
                case "xpath":
                    return driver.findElement(By.xpath(selectorValue));
                case "class":
                    return driver.findElement(By.className(selectorValue));
                case "id":
                    return driver.findElement(By.id(selectorValue));
                case "css":
                    return driver.findElement(By.cssSelector(selectorValue));
                case "name":
                    return driver.findElement(By.name(selectorValue));
                case "accessibilityid":
                case "accessibility_id":
                    return driver.findElement(By.xpath("//*[@aria-label='" + selectorValue + "' or @title='" + selectorValue + "' or @content-desc='" + selectorValue + "']"));
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warning("Failed to create WebElement reference for " + selectorType + " (" + selectorValue + "): " + e.getMessage());
            return null;
        }
    }

    /**
     * Find elements using standard Selenium methods for mobile platforms
     */
    private List<WebElement> findElementsBySelectorMobile(String selectorType, String selectorValue) {
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
     * Reset the found XPaths tracking - useful for debugging or when switching contexts
     */
    public void resetFoundXPathsTracking() {
        foundXPaths.clear();
        log.info("Reset found XPaths tracking - cleared " + foundXPaths.size() + " previously found selectors");
    }

    /**
     * Get the current count of found XPaths for debugging
     */
    public int getFoundXPathsCount() {
        return foundXPaths.size();
    }

    /**
     * Get element bounding box using JavaScript for web platforms
     * Uses the same approach as Golang implementation with getBoundingClientRect
     */
    private Map<String, Object> getElementBoundingBoxWeb(WebElement element) {
        try {
            // Use the same JavaScript approach as Golang implementation
            String script = 
                "var rect = arguments[0].getBoundingClientRect();" +
                "var scrollX = window.pageXOffset || document.documentElement.scrollLeft || 0;" +
                "var scrollY = window.pageYOffset || document.documentElement.scrollTop || 0;" +
                "return {" +
                "  x: rect.left + scrollX," +
                "  y: rect.top + scrollY," +
                "  width: rect.width," +
                "  height: rect.height" +
                "};";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript(script, element);
            
            log.info("JavaScript bounding box (Golang-style): x=" + result.get("x") + ", y=" + result.get("y") + 
                    ", width=" + result.get("width") + ", height=" + result.get("height"));
            
            return result;
            
        } catch (Exception e) {
            log.warning("JavaScript bounding box failed, falling back to Selenium: " + e.getMessage());
            
            // Fallback to Selenium methods
            Point location = element.getLocation();
            Dimension size = element.getSize();
            
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("x", location.getX());
            fallback.put("y", location.getY());
            fallback.put("width", size.getWidth());
            fallback.put("height", size.getHeight());
            
            return fallback;
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