package io.github.lambdatest.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.util.*;
import java.util.logging.Logger;

public class ElementBoundingBoxUtil {
    private final WebDriver driver;
    private final Logger log = LoggerUtil.createLogger("lambdatest-java-app-sdk");
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
        log.info("ElementBoundingBoxUtil initialized - testType: " + testType + ", deviceName: " + deviceName + ", DPR: " + devicePixelRatio);
    }

    public ElementBoundingBoxUtil(WebDriver driver, String testType, String deviceName) {
        this.driver = driver;
        this.testType = testType;
        this.deviceName = deviceName;
        this.devicePixelRatio = getDevicePixelRatio();
        log.info("ElementBoundingBoxUtil initialized - testType: " + testType + ", deviceName: " + deviceName + ", DPR: " + devicePixelRatio);
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

        log.info("Starting element detection for chunk " + chunkIndex + " on platform: " + platform);

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

            for (int i = 0; i < selectorValues.size(); i++) {
                String selectorValue = selectorValues.get(i);
                String selectorKey = selectorType + ":" + selectorValue;

                // Skip selector if it has already been found in a previous chunk
                if (foundXPaths.contains(selectorKey)) {
                    continue;
                }

                try {
                    List<WebElement> elements = findElementsBySelector(selectorType, selectorValue);

                    boolean selectorHasVisibleElements = false;

                    for (int j = 0; j < elements.size(); j++) {
                        WebElement element = elements.get(j);

                        ElementBoundingBox boundingBox = createBoundingBox(element, selectorKey, chunkIndex, platform, purpose);
                        if (boundingBox != null) {
                            if (isElementFullyInViewport(boundingBox)) {
                                detectedElements.add(boundingBox);
                                selectorHasVisibleElements = true;
                            }
                        } else {
                            log.warning("Failed to create bounding box for element " + (j + 1) + " of " + selectorType + " selector: " + selectorValue);
                        }
                    }

                    // Only mark selector as found if it has visible elements in the current viewport
                    if (selectorHasVisibleElements) {
                        foundXPaths.add(selectorKey);
                    }
                } catch (Exception e) {
                    log.warning("Failed to detect elements for " + selectorType + " selector '" + selectorValue + "': " + e.getMessage());
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
            Point location;
            Dimension size;

            if (platform.contains("web")) {
                // Use JavaScript for web platforms - more accurate positioning
                Map<String, Object> elementData = getElementBoundingBoxWeb(element);
                location = new Point(((Number) elementData.get("x")).intValue(), ((Number) elementData.get("y")).intValue());
                size = new Dimension(((Number) elementData.get("width")).intValue(), ((Number) elementData.get("height")).intValue());
            } else {
                // Use standard Selenium methods for mobile platforms
                location = element.getLocation();
                size = element.getSize();
            }

            // Get cumulative scroll position (in CSS pixels)
            int scrollY = getCurrentScrollPosition();

            // Calculate absolute page coordinates in CSS pixels
            int absoluteX = location.getX();
            int absoluteY = location.getY() + scrollY;

            // Element dimensions are already in CSS pixels
            int width = size.getWidth();
            int height = size.getHeight();

            // Store bounding box with CSS pixel coordinates
            ElementBoundingBox boundingBox = new ElementBoundingBox(selectorKey, absoluteX, absoluteY, width, height, chunkIndex, platform, purpose);

            return boundingBox;

        } catch (Exception e) {
            log.warning("Failed to create bounding box for element with selector '" + selectorKey + "': " + e.getMessage());
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
            Dimension viewportSize = driver.manage().window().getSize();
            int scrollY = getCurrentScrollPosition();
            String platform = detectPlatform();

            // Bounding box coordinates are already in CSS pixels
            int cssX = boundingBox.getX();
            int cssY = boundingBox.getY();
            int cssWidth = boundingBox.getWidth();
            int cssHeight = boundingBox.getHeight();

            // Calculate viewport-relative position in CSS pixels
            int viewportY = cssY - scrollY;

            // X bounds check: element must be within viewport width
            boolean xInBounds = cssX >= 0 &&
                    cssX + cssWidth <= viewportSize.getWidth();

            // Y bounds check: different logic for web vs mobile
            boolean yInBounds;
            if (platform.contains("web")) {
                // For web: element must be at least partially visible (more lenient)
                boolean elementPartiallyVisible = (viewportY < viewportSize.getHeight()) &&
                        (viewportY + cssHeight > 0);
                yInBounds = elementPartiallyVisible;
            } else {
                // For mobile: element must be fully visible (stricter)
                yInBounds = viewportY >= 0 &&
                        viewportY + cssHeight <= viewportSize.getHeight();
            }

            return xInBounds && yInBounds;

        } catch (Exception e) {
            log.warning("Failed to check viewport bounds: " + e.getMessage());
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
                        scrollPosition = cumulativeScrollPosition;
                    }

                    return scrollPosition;
                } catch (Exception e) {
                    log.warning("Failed to get scroll position from JavaScript: " + e.getMessage());
                    return cumulativeScrollPosition;
                }
            } else {
                // For native mobile apps, use tracked cumulative scroll position
                return cumulativeScrollPosition;
            }
        } catch (Exception e) {
            log.warning("Failed to get scroll position: " + e.getMessage());
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
                        dpr = 1.0;
                    }

                    return dpr;
                } catch (Exception e) {
                    log.warning("Failed to get device pixel ratio from JavaScript: " + e.getMessage());
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
                        return dpr;
                    }
                } catch (Exception e) {
                    log.warning("Failed to get device pixel ratio from capabilities: " + e.getMessage());
                }

                return 1.0;
            }
        } catch (Exception e) {
            log.warning("Failed to get device pixel ratio: " + e.getMessage());
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

            // iPhone DPR Map based on ios-resolution.com scale factors
            if (deviceName.contains("iphone") || model.contains("iphone")) {
                // iPhone 16 series (Scale Factor: 3)
                if (deviceName.contains("16") || model.contains("16")) {
                    return 3.0;
                }
                // iPhone 15 series (Scale Factor: 3)
                if (deviceName.contains("15") || model.contains("15")) {
                    return 3.0;
                }
                // iPhone 14 series (Scale Factor: 3)
                if (deviceName.contains("14") || model.contains("14")) {
                    return 3.0;
                }
                // iPhone 13 series (Scale Factor: 3)
                if (deviceName.contains("13") || model.contains("13")) {
                    return 3.0;
                }
                // iPhone 12 series (Scale Factor: 3)
                if (deviceName.contains("12") || model.contains("12")) {
                    return 3.0;
                }
                // iPhone 11 series (Scale Factor: 3)
                if (deviceName.contains("11") || model.contains("11")) {
                    return 3.0;
                }
                // iPhone X series (Scale Factor: 3)
                if (deviceName.contains("x") || model.contains("x")) {
                    return 3.0;
                }
                // iPhone 8 series (Scale Factor: 3)
                if (deviceName.contains("8") || model.contains("8")) {
                    return 3.0;
                }
                // iPhone 7 series (Scale Factor: 3)
                if (deviceName.contains("7") || model.contains("7")) {
                    return 3.0;
                }
                // iPhone 6 Plus (Scale Factor: 3)
                if (deviceName.contains("6 plus") || model.contains("6 plus")) {
                    return 3.0;
                }
                // iPhone 6 (Scale Factor: 2)
                if (deviceName.contains("6") || model.contains("6")) {
                    return 2.0;
                }
                // iPhone 5 series (Scale Factor: 2)
                if (deviceName.contains("5") || model.contains("5")) {
                    return 2.0;
                }
                // iPhone 4 series (Scale Factor: 2)
                if (deviceName.contains("4") || model.contains("4")) {
                    return 2.0;
                }
                // iPhone SE series (Scale Factor: 2)
                if (deviceName.contains("se") || model.contains("se")) {
                    return 2.0;
                }
                // iPhone 3GS and earlier (Scale Factor: 1)
                if (deviceName.contains("3") || model.contains("3")) {
                    return 1.0;
                }
                // Future iPhone models (Scale Factor: 3) - for iPhone 17+ and beyond
                if (extractIPhoneNumber(deviceName, model) > 16) {
                    return 3.0;
                }
                // Default for other iPhones (Scale Factor: 3)
                return 3.0;
            }

            // iPad DPR Map based on ios-resolution.com scale factors
            if (deviceName.contains("ipad") || model.contains("ipad")) {
                // All iPad series (Scale Factor: 2)
                return 2.0;
            }

            // iPod touch DPR Map based on ios-resolution.com scale factors
            if (deviceName.contains("ipod") || model.contains("ipod")) {
                // iPod touch 5th gen and later (Scale Factor: 2)
                if (deviceName.contains("5") || model.contains("5") ||
                        deviceName.contains("6") || model.contains("6") ||
                        deviceName.contains("7") || model.contains("7")) {
                    return 2.0;
                }
                // iPod touch 4th gen and earlier (Scale Factor: 1)
                return 1.0;
            }

            // Fallback for iOS devices
            return 3.0;

        } catch (Exception e) {
            log.warning("Failed to get iOS device pixel ratio: " + e.getMessage());
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
                return 20;
            }

            String lowerName = deviceName.toLowerCase();

            // iPad handling
            if (lowerName.contains("ipad")) {
                int height = getIPadStatusBarHeight(lowerName);
                return height;
            }

            if (lowerName.contains("iphone")) {
                int height = getIPhoneStatusBarHeight(lowerName);
                return height;
            }

            return 20;

        } catch (Exception e) {
            log.warning("Failed to get iOS status bar height: " + e.getMessage());
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
                    return 24;
                }
            }
        }
        return 20;
    }

    /**
     * Get iPhone status bar height in CSS pixels
     */
    private int getIPhoneStatusBarHeight(String deviceName) {
        if (hasDynamicIsland(deviceName)) {
            return 54;
        }

        if (hasNotch(deviceName)) {
            return 47;
        }

        if (isTraditionalIPhone(deviceName)) {
            return 20;
        }

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
            // Only apply status bar height for web tests on iOS devices
            if (testType.toLowerCase().contains("web") && (deviceName.toLowerCase().contains("iphone") || deviceName.toLowerCase().contains("ipad") || deviceName.toLowerCase().contains("ipod"))) {
                return true;
            }

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
                return devicePixelStatusBarHeight;
            }

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
            // Only apply Chrome bar height for web tests on Android devices
            if (testType.toLowerCase().contains("web") &&
                    (deviceName.toLowerCase().contains("android") ||
                            detectPlatform().toLowerCase().contains("android"))) {
                return true;
            }

            return false;

        } catch (Exception e) {
            log.warning("Failed to check if Android Chrome address bar height should be added: " + e.getMessage());
            return false;
        }
    }

    // Get default Chrome address bar height for Android devices
    private int getChromeAddressBarHeightForAndroid() {
        try {
            // Common Chrome address bar heights on Android (in CSS pixels)
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            String androidVersion = "";
            if (caps.getCapability("platformVersion") != null) {
                androidVersion = caps.getCapability("platformVersion").toString();
            }

            // Android 11+ typically has: Status bar: 24px + Chrome address bar: ~56px = ~80px
            if (androidVersion.startsWith("11") || androidVersion.startsWith("12") ||
                    androidVersion.startsWith("13") || androidVersion.startsWith("14")) {
                return 80;
            }

            // Android 10 typically has similar heights
            if (androidVersion.startsWith("10")) {
                return 80;
            }

            // Android 9 and below might have slightly different heights
            if (androidVersion.startsWith("9") || androidVersion.startsWith("8") ||
                    androidVersion.startsWith("7")) {
                return 76;
            }

            // Default for unknown versions
            return 80;

        } catch (Exception e) {
            log.warning("Failed to determine default Chrome bar height: " + e.getMessage());
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
                return devicePixelChromeBarHeight;
            }

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
                return version;
            }

            // If no specific iPhone pattern, try to find standalone numbers that might be versions
            pattern = java.util.regex.Pattern.compile("\\b(\\d{2,})\\b");
            matcher = pattern.matcher(combined);

            if (matcher.find()) {
                int version = Integer.parseInt(matcher.group(1));
                // Only consider reasonable iPhone version numbers (17+ for future devices)
                if (version >= 17) {
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
        this.cumulativeScrollPosition += scrollDistance;
        log.info("Updated cumulative scroll position to: " + cumulativeScrollPosition + " CSS pixels");
    }

    /**
     * Detect platform (iOS/Android, Native/WebView)
     */
    private String detectPlatform() {
        try {
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
        }
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

        // Check if we need to add status bar height for web tests on iOS devices
        int statusBarHeightToAdd = 0;

        if (shouldAddStatusBarHeight()) {
            statusBarHeightToAdd = getStatusBarHeightInDevicePixels();
            log.info("Web test on iOS device - adding status bar height: " + statusBarHeightToAdd + " device pixels");
        }

        // Add element data
        List<Map<String, Object>> elementData = new ArrayList<>();
        if (elements != null) {
            for (int i = 0; i < elements.size(); i++) {
                ElementBoundingBox element = elements.get(i);

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
            }
        }

        uploadData.put("elements", elementData);
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

            // Execute JavaScript to get bounding rect
            Object result = ((JavascriptExecutor) driver).executeScript(script);

            if (result != null) {
                // If we got a bounding rect, the element exists - create a WebElement reference
                WebElement element = createWebElementFromSelector(selectorType, selectorValue);
                if (element != null) {
                    elements.add(element);
                }
            }

            return elements;

        } catch (Exception e) {
            log.warning("JavaScript element detection failed for " + selectorType + " (" + selectorValue + "): " + e.getMessage());
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
        log.info("Reset found XPaths tracking");
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
            log.info("iOS optimization: Detecting all elements with absolute positions");
            List<ElementBoundingBox> allElements = new ArrayList<>();
            String platform = detectPlatform();

            if (selectors == null || selectors.isEmpty()) {
                return allElements;
            }

            // Process each selector type
            for (Map.Entry<String, List<String>> entry : selectors.entrySet()) {
                String selectorType = entry.getKey();
                List<String> selectorValues = entry.getValue();

                if (selectorValues == null || selectorValues.isEmpty()) {
                    continue;
                }

                for (String selectorValue : selectorValues) {
                    String selectorKey = selectorType + ":" + selectorValue;

                    try {
                        List<WebElement> elements = findElementsBySelector(selectorType, selectorValue);

                        for (WebElement element : elements) {
                            // Create bounding box with absolute position (no scroll offset needed for iOS)
                            ElementBoundingBox boundingBox = createBoundingBox(element, selectorKey, 0, platform, purpose);

                            if (boundingBox != null) {
                                allElements.add(boundingBox);
                            } else {
                                log.warning("Failed to create bounding box for element: " + selectorKey);
                            }
                        }
                    } catch (Exception e) {
                        log.warning("Failed to detect elements for " + selectorType + " (" + selectorValue + "): " + e.getMessage());
                    }
                }
            }

            log.info("iOS optimization: Detected " + allElements.size() + " elements with absolute positions");

            // Convert CSS pixels to device pixels for iOS elements (same as Android/Web)
            List<ElementBoundingBox> devicePixelElements = convertBoundingBoxesToDevicePixels(allElements);

            return devicePixelElements;

        } catch (Exception e) {
            log.severe("iOS element detection failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}