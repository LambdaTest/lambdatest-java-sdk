package io.github.lambdatest.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.util.*;
import java.util.logging.Logger;

public class ElementBoundingBoxUtil {
    private static final double DEFAULT_DEVICE_PIXEL_RATIO = 1.0;
    private static final int DEFAULT_STATUS_BAR_HEIGHT = 20;
    private static final int IPAD_PRO_NEWER_STATUS_BAR_HEIGHT = 24;
    private static final int IPHONE_DYNAMIC_ISLAND_HEIGHT = 54;
    private static final int IPHONE_NOTCH_HEIGHT = 47;
    private static final int ANDROID_CHROME_BAR_HEIGHT_WITH_STATUS_BAR_NEW = 80;
    private static final int ANDROID_CHROME_BAR_HEIGHT_WITH_STATUS_BAR_OLD = 76;
    private static final int ANDROID_CHROME_BAR_HEIGHT = 56;
    private static final int FUTURE_IPHONE_VERSION_THRESHOLD = 17;

    // Device pixel ratios
    private static final double IOS_SCALE_FACTOR_1X = 1.0;
    private static final double IOS_SCALE_FACTOR_2X = 2.0;
    private static final double IOS_SCALE_FACTOR_3X = 3.0;

    // Selector types
    private static final String SELECTOR_XPATH = "xpath";
    private static final String SELECTOR_CLASS = "class";
    private static final String SELECTOR_ID = "id";
    private static final String SELECTOR_CSS = "css";
    private static final String SELECTOR_NAME = "name";
    private static final String SELECTOR_ACCESSIBILITY_ID = "accessibilityid";
    private static final String SELECTOR_ACCESSIBILITY_ID_ALT = "accessibility_id";

    // Platform types
    private static final String PLATFORM_WEB = "web";
    private static final String PLATFORM_IOS = "ios";
    private static final String PLATFORM_ANDROID = "android";
    private static final String PLATFORM_IOS_WEBVIEW = "ios_webview";
    private static final String PLATFORM_IOS_NATIVE = "ios_native";
    private static final String PLATFORM_ANDROID_WEBVIEW = "android_webview";
    private static final String PLATFORM_ANDROID_NATIVE = "android_native";

    private static final Set<String> IPHONE_3X_MODELS = new HashSet<>(Arrays.asList(
            "16", "15", "14", "13", "12", "11", "x", "xs", "xr", "8", "7", "6 plus"
    ));
    private static final Set<String> IPHONE_2X_MODELS = new HashSet<>(Arrays.asList("6", "5", "4", "se"));
    private static final Set<String> IPHONE_1X_MODELS = new HashSet<>(Arrays.asList("3"));

    private static final Set<String> DYNAMIC_ISLAND_DEVICES = new HashSet<>(Arrays.asList("14 pro", "14pro", "15", "16", "17"));
    private static final Set<String> NOTCH_DEVICES = new HashSet<>(Arrays.asList("x", "xs", "xr", "11", "12", "13", "14"));
    private static final Set<String> TRADITIONAL_IPHONES = new HashSet<>(Arrays.asList("se", "8", "7", "6", "5", "4"));

    private static final Set<String> IPAD_PRO_NEWER_YEARS = new HashSet<>(Arrays.asList("2018", "2020", "2021", "2022", "2024", "2025"));
    private static final Set<String> IPOD_2X_GENERATIONS = new HashSet<>(Arrays.asList("5", "6", "7"));

    private static final Set<String> ANDROID_NEW_VERSIONS = new HashSet<>(Arrays.asList("11", "12", "13", "14", "10"));
    private static final Set<String> ANDROID_OLD_VERSIONS = new HashSet<>(Arrays.asList("9", "8", "7"));

    private final WebDriver driver;
    private final Logger log = LoggerUtil.createLogger("lambdatest-java-app-sdk");
    private final String testType;
    private final String deviceName;
    private final double devicePixelRatio;
    private int cumulativeScrollPosition = 0;
    private final Set<String> foundElements = new HashSet<>();

    public ElementBoundingBoxUtil(WebDriver driver, String testType, String deviceName) {
        this.driver = driver;
        this.testType = testType;
        this.deviceName = deviceName;
        this.devicePixelRatio = getDevicePixelRatio();
    }

    public List<ElementBoundingBox> detectElements(Map<String, List<String>> selectors, int chunkIndex, String purpose) {
        if (isInvalidSelectorMap(selectors)) {
            return new ArrayList<>();
        }

        List<ElementBoundingBox> detectedElements = new ArrayList<>();
        String platform = detectPlatform();

        for (Map.Entry<String, List<String>> entry : selectors.entrySet()) {
            processSelectorsOfType(entry.getKey(), entry.getValue(), detectedElements, chunkIndex, platform, purpose);
        }

        return convertBoundingBoxesToDevicePixels(detectedElements);
    }

    private void processSelectorsOfType(String selectorType, List<String> selectorValues,
                                        List<ElementBoundingBox> detectedElements, int chunkIndex,
                                        String platform, String purpose) {
        if (selectorValues == null || selectorValues.isEmpty()) {
            return;
        }

        for (String selectorValue : selectorValues) {
            String selectorKey = createSelectorKey(selectorType, selectorValue);

            if (foundElements.contains(selectorKey)) {
                continue;
            }

            processSelector(selectorType, selectorValue, selectorKey, detectedElements, chunkIndex, platform, purpose);
        }
    }

    private void processSelector(String selectorType, String selectorValue, String selectorKey,
                                 List<ElementBoundingBox> detectedElements, int chunkIndex,
                                 String platform, String purpose) {
        try {
            List<WebElement> elements = findElementsBySelector(selectorType, selectorValue);
            boolean hasVisibleElements = processElementsForSelector(elements, selectorKey, detectedElements, chunkIndex, platform, purpose);

            if (hasVisibleElements) {
                foundElements.add(selectorKey);
            }
        } catch (Exception e) {
            log.warning("Failed to detect elements for " + selectorType + " selector '" + selectorValue + "': " + e.getMessage());
        }
    }

    private boolean processElementsForSelector(List<WebElement> elements, String selectorKey,
                                               List<ElementBoundingBox> detectedElements, int chunkIndex,
                                               String platform, String purpose) {
        boolean hasVisibleElements = false;

        for (int i = 0; i < elements.size(); i++) {
            ElementBoundingBox boundingBox = createBoundingBox(elements.get(i), selectorKey, chunkIndex, platform, purpose);

            if (boundingBox != null && isElementFullyInViewport(boundingBox)) {
                detectedElements.add(boundingBox);
                hasVisibleElements = true;
            } else if (boundingBox == null) {
                log.warning("Failed to create bounding box for element " + (i + 1) + " of selector: " + selectorKey);
            }
        }

        return hasVisibleElements;
    }

    private ElementBoundingBox createBoundingBox(WebElement element, String selectorKey, int chunkIndex, String platform, String purpose) {
        try {
            Point location;
            Dimension size;

            if (platform.contains(PLATFORM_WEB)) {
                Map<String, Object> elementData = getElementBoundingBoxWeb(element);
                location = extractPointFromElementData(elementData);
                size = extractDimensionFromElementData(elementData);
            } else {
                location = element.getLocation();
                size = element.getSize();
            }

            int scrollY = getCurrentScrollPosition();
            int absoluteX = location.getX();
            int absoluteY = location.getY() + scrollY;

            return new ElementBoundingBox(selectorKey, absoluteX, absoluteY, size.getWidth(), size.getHeight(), chunkIndex, platform, purpose);
        } catch (Exception e) {
            log.warning("Failed to create bounding box for element with selector '" + selectorKey + "': " + e.getMessage());
            return null;
        }
    }

    private Point extractPointFromElementData(Map<String, Object> elementData) {
        int x = ((Number) elementData.get("x")).intValue();
        int y = ((Number) elementData.get("y")).intValue();
        return new Point(x, y);
    }

    private Dimension extractDimensionFromElementData(Map<String, Object> elementData) {
        int width = ((Number) elementData.get("width")).intValue();
        int height = ((Number) elementData.get("height")).intValue();
        return new Dimension(width, height);
    }

    private boolean isElementFullyInViewport(ElementBoundingBox boundingBox) {
        try {
            Dimension viewportSize = driver.manage().window().getSize();
            int scrollY = getCurrentScrollPosition();
            int viewportY = boundingBox.getY() - scrollY;

            boolean xInBounds = boundingBox.getX() >= 0 &&
                    boundingBox.getX() + boundingBox.getWidth() <= viewportSize.getWidth();
            boolean yInBounds = viewportY >= 0 &&
                    viewportY + boundingBox.getHeight() <= viewportSize.getHeight();

            return xInBounds && yInBounds;
        } catch (Exception e) {
            log.warning("Failed to check viewport bounds: " + e.getMessage());
            return false;
        }
    }

    private double getDevicePixelRatio() {
        String platform = detectPlatform();

        if (platform.contains(PLATFORM_WEB)) {
            return getWebDevicePixelRatio();
        } else if (platform.contains(PLATFORM_IOS)) {
            return getIOSDevicePixelRatio();
        } else {
            return getAndroidDevicePixelRatio();
        }
    }

    private double getWebDevicePixelRatio() {
        try {
            Object dprResult = ((JavascriptExecutor) driver).executeScript("return window.devicePixelRatio || 1;");
            return parseDevicePixelRatio(dprResult);
        } catch (Exception e) {
            log.warning("Failed to get device pixel ratio from JavaScript: " + e.getMessage());
            return DEFAULT_DEVICE_PIXEL_RATIO;
        }
    }

    private double getAndroidDevicePixelRatio() {
        try {
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            Object pixelRatio = caps.getCapability("devicePixelRatio");
            return pixelRatio != null ? Double.parseDouble(pixelRatio.toString()) : DEFAULT_DEVICE_PIXEL_RATIO;
        } catch (Exception e) {
            log.warning("Failed to get device pixel ratio from capabilities: " + e.getMessage());
            return DEFAULT_DEVICE_PIXEL_RATIO;
        }
    }

    private double parseDevicePixelRatio(Object dprResult) {
        if (dprResult instanceof Number) {
            return ((Number) dprResult).doubleValue();
        } else if (dprResult instanceof String) {
            return Double.parseDouble((String) dprResult);
        } else {
            return DEFAULT_DEVICE_PIXEL_RATIO;
        }
    }

    private double getIOSDevicePixelRatio() {
        try {
            DeviceInfo deviceInfo = getDeviceInfo();
            return determineIOSPixelRatio(deviceInfo.deviceName, deviceInfo.model);
        } catch (Exception e) {
            log.warning("Failed to get iOS device pixel ratio: " + e.getMessage());
            return IOS_SCALE_FACTOR_3X;
        }
    }

    private double determineIOSPixelRatio(String deviceName, String model) {
        if (isDevice(deviceName, model, "iphone")) {
            return getIPhonePixelRatio(deviceName, model);
        } else if (isDevice(deviceName, model, "ipad")) {
            return IOS_SCALE_FACTOR_2X;
        } else if (isDevice(deviceName, model, "ipod")) {
            return getIPodPixelRatio(deviceName, model);
        }
        return IOS_SCALE_FACTOR_3X;
    }

    private double getIPhonePixelRatio(String deviceName, String model) {
        if (containsAnyModel(deviceName, model, IPHONE_3X_MODELS)) {
            return IOS_SCALE_FACTOR_3X;
        } else if (containsAnyModel(deviceName, model, IPHONE_2X_MODELS)) {
            return IOS_SCALE_FACTOR_2X;
        } else if (containsAnyModel(deviceName, model, IPHONE_1X_MODELS)) {
            return IOS_SCALE_FACTOR_1X;
        } else if (extractIPhoneNumber(deviceName, model) > FUTURE_IPHONE_VERSION_THRESHOLD) {
            return IOS_SCALE_FACTOR_3X;
        }
        return IOS_SCALE_FACTOR_3X;
    }

    private double getIPodPixelRatio(String deviceName, String model) {
        return containsAnyModel(deviceName, model, IPOD_2X_GENERATIONS) ? IOS_SCALE_FACTOR_2X : IOS_SCALE_FACTOR_1X;
    }

    public int getStatusBarHeightForIOS() {
        try {
            String deviceName = getCapabilityAsString("deviceName");
            if (deviceName.isEmpty()) {
                return DEFAULT_STATUS_BAR_HEIGHT;
            }

            String lowerName = deviceName.toLowerCase();
            if (lowerName.contains("ipad")) {
                return getIPadStatusBarHeight(lowerName);
            } else if (lowerName.contains("iphone")) {
                return getIPhoneStatusBarHeight(lowerName);
            }

            return DEFAULT_STATUS_BAR_HEIGHT;
        } catch (Exception e) {
            log.warning("Failed to get iOS status bar height: " + e.getMessage());
            return DEFAULT_STATUS_BAR_HEIGHT;
        }
    }

    private int getIPadStatusBarHeight(String deviceName) {
        if (deviceName.contains("pro") && containsAny(deviceName, IPAD_PRO_NEWER_YEARS)) {
            return IPAD_PRO_NEWER_STATUS_BAR_HEIGHT;
        }
        return DEFAULT_STATUS_BAR_HEIGHT;
    }

    private int getIPhoneStatusBarHeight(String deviceName) {
        if (containsAny(deviceName, DYNAMIC_ISLAND_DEVICES)) {
            return IPHONE_DYNAMIC_ISLAND_HEIGHT;
        } else if (hasNotch(deviceName)) {
            return IPHONE_NOTCH_HEIGHT;
        } else if (containsAny(deviceName, TRADITIONAL_IPHONES)) {
            return DEFAULT_STATUS_BAR_HEIGHT;
        }
        return DEFAULT_STATUS_BAR_HEIGHT;
    }

    private boolean hasNotch(String deviceName) {
        for (String device : NOTCH_DEVICES) {
            if (deviceName.contains(device)) {
                if (device.equals("14") && (deviceName.contains("pro") || deviceName.contains("14 pro"))) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    public int getStatusBarHeightInDevicePixels() {
        try {
            if (shouldAddStatusBarHeight()) {
                int cssStatusBarHeight = getStatusBarHeightForIOS();
                return (int) (cssStatusBarHeight * devicePixelRatio);
            }
            return 0;
        } catch (Exception e) {
            log.warning("Failed to get status bar height in device pixels: " + e.getMessage());
            return 0;
        }
    }

    public int getAndroidChromeBarHeightInDevicePixels() {
        try {
            if (!shouldAddAndroidChromeBarHeight()) {
                return 0;
            }

            int statusBarHeight = 0;
            String statusBarHeightStr = getCapabilityAsString("statBarHeight");
            if (!statusBarHeightStr.isEmpty()) {
                try {
                    statusBarHeight = Integer.parseInt(statusBarHeightStr);
                } catch (NumberFormatException e) {
                    log.warning("Invalid status bar height format: " + statusBarHeightStr);
                }
            }

            int baseHeight;
            if (statusBarHeight > 0) {
                baseHeight = statusBarHeight + (int)devicePixelRatio * ANDROID_CHROME_BAR_HEIGHT;
            } else {
                String androidVersion = getCapabilityAsString("platformVersion");
                if (startsWithAny(androidVersion, ANDROID_NEW_VERSIONS)) {
                    baseHeight = (int)devicePixelRatio * ANDROID_CHROME_BAR_HEIGHT_WITH_STATUS_BAR_NEW;
                } else if (startsWithAny(androidVersion, ANDROID_OLD_VERSIONS)) {
                    baseHeight = (int)devicePixelRatio * ANDROID_CHROME_BAR_HEIGHT_WITH_STATUS_BAR_OLD;
                } else {
                    baseHeight = (int)devicePixelRatio * ANDROID_CHROME_BAR_HEIGHT_WITH_STATUS_BAR_NEW;
                }
            }

            return baseHeight;

        } catch (Exception e) {
            log.warning("Failed to get Android Chrome bar height: " + e.getMessage());
            return 0;
        }
    }

    private boolean shouldAddStatusBarHeight() {
        return isWebTest() && isIOSDevice();
    }

    private boolean shouldAddAndroidChromeBarHeight() {
        return isWebTest() && isAndroidDevice();
    }

    private boolean isWebTest() {
        return testType.toLowerCase().contains(PLATFORM_WEB);
    }

    private boolean isIOSDevice() {
        String lowerDeviceName = deviceName.toLowerCase();
        return lowerDeviceName.contains("iphone") || lowerDeviceName.contains("ipad") || lowerDeviceName.contains("ipod");
    }

    private boolean isAndroidDevice() {
        return deviceName.toLowerCase().contains(PLATFORM_ANDROID) || detectPlatform().toLowerCase().contains(PLATFORM_ANDROID);
    }

    public void updateScrollPosition(int scrollDistance) {
        this.cumulativeScrollPosition += scrollDistance;
        log.info("Updated cumulative scroll position to: " + cumulativeScrollPosition + " CSS pixels");
    }

    public List<ElementBoundingBox> detectAllElementsAtTheStart(Map<String, List<String>> selectors, String purpose) {
        if (isInvalidSelectorMap(selectors)) {
            return new ArrayList<>();
        }

        try {
            List<ElementBoundingBox> allElements = new ArrayList<>();
            String platform = detectPlatform();

            for (Map.Entry<String, List<String>> entry : selectors.entrySet()) {
                processAllElementsOfType(entry.getKey(), entry.getValue(), allElements, platform, purpose);
            }

            return convertBoundingBoxesToDevicePixels(allElements);
        } catch (Exception e) {
            log.severe("Element detection failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void processAllElementsOfType(String selectorType, List<String> selectorValues,
                                          List<ElementBoundingBox> allElements, String platform, String purpose) {
        if (selectorValues == null || selectorValues.isEmpty()) {
            return;
        }

        for (String selectorValue : selectorValues) {
            String selectorKey = createSelectorKey(selectorType, selectorValue);
            processAllElementsForSelector(selectorType, selectorValue, selectorKey, allElements, platform, purpose);
        }
    }

    private void processAllElementsForSelector(String selectorType, String selectorValue, String selectorKey,
                                               List<ElementBoundingBox> allElements, String platform, String purpose) {
        try {
            List<WebElement> elements = findElementsBySelector(selectorType, selectorValue);

            for (WebElement element : elements) {
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

    private String detectPlatform() {
        try {
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            String platformName = getCapabilityAsString(caps, "platformName", "platform");
            String browserName = getCapabilityAsString(caps, "browserName");

            if (platformName.contains(PLATFORM_IOS)) {
                return browserName.equals("safari") ? PLATFORM_IOS_WEBVIEW : PLATFORM_IOS_NATIVE;
            } else if (platformName.contains(PLATFORM_ANDROID)) {
                return browserName.equals("chrome") ? PLATFORM_ANDROID_WEBVIEW : PLATFORM_ANDROID_NATIVE;
            } else {
                return PLATFORM_WEB;
            }
        } catch (Exception e) {
            log.warning("Failed to detect platform: " + e.getMessage());
            return "unknown";
        }
    }

    private List<ElementBoundingBox> convertBoundingBoxesToDevicePixels(List<ElementBoundingBox> cssElements) {
        return cssElements.stream()
                .map(this::convertToDevicePixels)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private ElementBoundingBox convertToDevicePixels(ElementBoundingBox cssElement) {
        return new ElementBoundingBox(
                cssElement.getSelectorKey(),
                (int) (cssElement.getX() * devicePixelRatio),
                (int) (cssElement.getY() * devicePixelRatio),
                (int) (cssElement.getWidth() * devicePixelRatio),
                (int) (cssElement.getHeight() * devicePixelRatio),
                cssElement.getChunkIndex(),
                cssElement.getPlatform(),
                cssElement.getPurpose()
        );
    }

    private List<WebElement> findElementsBySelector(String selectorType, String selectorValue) {
        String platform = detectPlatform();
        return platform.contains(PLATFORM_WEB) ?
                findElementsBySelectorWeb(selectorType, selectorValue) :
                findElementsBySelectorMobile(selectorType, selectorValue);
    }

    private List<WebElement> findElementsBySelectorWeb(String selectorType, String selectorValue) {
        try {
            String script = buildDOMBoxScript(selectorType, selectorValue);
            if (script.isEmpty()) {
                log.warning("Unsupported selector type for web: " + selectorType + ", falling back to standard method");
                return findElementsBySelectorMobile(selectorType, selectorValue);
            }

            Object result = ((JavascriptExecutor) driver).executeScript(script);
            if (result != null) {
                WebElement element = createWebElementFromSelector(selectorType, selectorValue);
                if (element != null) {
                    List<WebElement> elementList = new ArrayList<>();
                    elementList.add(element);
                    return elementList;
                }
                return new ArrayList<>();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.warning("JavaScript element detection failed for " + selectorType + " (" + selectorValue + "): " + e.getMessage());
            return findElementsBySelectorMobile(selectorType, selectorValue);
        }
    }

    private String buildDOMBoxScript(String selectorType, String elementSelector) {
        switch (selectorType.toLowerCase()) {
            case SELECTOR_CLASS:
                return "return document.getElementsByClassName(`" + elementSelector + "`)[0].getBoundingClientRect();";
            case SELECTOR_ID:
                return "return document.getElementById(`" + elementSelector + "`).getBoundingClientRect();";
            case SELECTOR_XPATH:
                return "var element = document.evaluate(`" + elementSelector + "`, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null).snapshotItem(0); return element ? element.getBoundingClientRect() : null;";
            case SELECTOR_CSS:
                return "return document.querySelectorAll(`" + elementSelector + "`)[0].getBoundingClientRect();";
            case SELECTOR_NAME:
                return "return document.getElementsByName(`" + elementSelector + "`)[0].getBoundingClientRect();";
            case SELECTOR_ACCESSIBILITY_ID:
            case SELECTOR_ACCESSIBILITY_ID_ALT:
                return "var element = document.querySelector('[aria-label=\"" + elementSelector + "\"], [title=\"" + elementSelector + "\"], [content-desc=\"" + elementSelector + "\"]'); return element ? element.getBoundingClientRect() : null;";
            default:
                return "";
        }
    }

    private WebElement createWebElementFromSelector(String selectorType, String selectorValue) {
        try {
            switch (selectorType.toLowerCase()) {
                case SELECTOR_XPATH:
                    return driver.findElement(By.xpath(selectorValue));
                case SELECTOR_CLASS:
                    return driver.findElement(By.className(selectorValue));
                case SELECTOR_ID:
                    return driver.findElement(By.id(selectorValue));
                case SELECTOR_CSS:
                    return driver.findElement(By.cssSelector(selectorValue));
                case SELECTOR_NAME:
                    return driver.findElement(By.name(selectorValue));
                case SELECTOR_ACCESSIBILITY_ID:
                case SELECTOR_ACCESSIBILITY_ID_ALT:
                    return driver.findElement(By.xpath("//*[@aria-label='" + selectorValue + "' or @title='" + selectorValue + "' or @content-desc='" + selectorValue + "']"));
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warning("Failed to create WebElement reference for " + selectorType + " (" + selectorValue + "): " + e.getMessage());
            return null;
        }
    }

    private List<WebElement> findElementsBySelectorMobile(String selectorType, String selectorValue) {
        switch (selectorType.toLowerCase()) {
            case SELECTOR_XPATH:
                return driver.findElements(By.xpath(selectorValue));
            case SELECTOR_CLASS:
                return driver.findElements(By.className(selectorValue));
            case SELECTOR_ACCESSIBILITY_ID:
            case SELECTOR_ACCESSIBILITY_ID_ALT:
                return driver.findElements(By.xpath("//*[@content-desc='" + selectorValue + "']"));
            case SELECTOR_NAME:
                return driver.findElements(By.name(selectorValue));
            case SELECTOR_ID:
                return driver.findElements(By.id(selectorValue));
            case SELECTOR_CSS:
                return driver.findElements(By.cssSelector(selectorValue));
            default:
                log.warning("Unsupported selector type: " + selectorType + ", falling back to XPath");
                return driver.findElements(By.xpath(selectorValue));
        }
    }

    private Map<String, Object> getElementBoundingBoxWeb(WebElement element) {
        try {
            String script = "var rect = arguments[0].getBoundingClientRect();" +
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
            return createFallbackBoundingBox(element);
        }
    }

    private Map<String, Object> createFallbackBoundingBox(WebElement element) {
        Point location = element.getLocation();
        Dimension size = element.getSize();

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("x", location.getX());
        fallback.put("y", location.getY());
        fallback.put("width", size.getWidth());
        fallback.put("height", size.getHeight());
        return fallback;
    }

    // Helper methods
    private int getCurrentScrollPosition() {
        return cumulativeScrollPosition;
    }

    private String createSelectorKey(String selectorType, String selectorValue) {
        return selectorType + ":" + selectorValue;
    }

    private boolean isInvalidSelectorMap(Map<String, List<String>> selectors) {
        return selectors == null || selectors.isEmpty();
    }

    private DeviceInfo getDeviceInfo() {
        Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
        String deviceName = getCapabilityAsString(caps, "deviceName", "");
        String model = getCapabilityAsString(caps, "model", "");
        return new DeviceInfo(deviceName.toLowerCase(), model.toLowerCase());
    }

    private String getCapabilityAsString(String capabilityName) {
        Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
        return getCapabilityAsString(caps, capabilityName, "");
    }

    private String getCapabilityAsString(Capabilities caps, String... capabilityNames) {
        for (String capName : capabilityNames) {
            Object capability = caps.getCapability(capName);
            if (capability != null) {
                return capability.toString().toLowerCase();
            }
        }
        return "";
    }

    private boolean isDevice(String deviceName, String model, String deviceType) {
        return deviceName.contains(deviceType) || model.contains(deviceType);
    }

    private boolean containsAnyModel(String deviceName, String model, Set<String> models) {
        return models.stream().anyMatch(m -> deviceName.contains(m) || model.contains(m));
    }

    private boolean containsAny(String text, Set<String> items) {
        return items.stream().anyMatch(text::contains);
    }

    private boolean startsWithAny(String text, Set<String> prefixes) {
        return prefixes.stream().anyMatch(text::startsWith);
    }

    private int extractIPhoneNumber(String deviceName, String model) {
        try {
            String combined = (deviceName + " " + model);
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("iphone\\s*(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(combined);

            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }

            pattern = java.util.regex.Pattern.compile("\\b(\\d{2,})\\b");
            matcher = pattern.matcher(combined);

            if (matcher.find()) {
                int version = Integer.parseInt(matcher.group(1));
                return version >= FUTURE_IPHONE_VERSION_THRESHOLD ? version : 0;
            }

            return 0;
        } catch (Exception e) {
            log.warning("Failed to extract iPhone number: " + e.getMessage());
            return 0;
        }
    }

    private static class DeviceInfo {
        final String deviceName;
        final String model;

        DeviceInfo(String deviceName, String model) {
            this.deviceName = deviceName;
            this.model = model;
        }
    }
}