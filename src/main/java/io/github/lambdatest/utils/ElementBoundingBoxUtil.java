package io.github.lambdatest.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.util.*;
import java.util.logging.Logger;

public class ElementBoundingBoxUtil {
    private final WebDriver driver;
    private final Logger log = LoggerUtil.createLogger("lambdatest-java-app-sdk");
    private static final int PROXIMITY_THRESHOLD = 10; // pixels

    public ElementBoundingBoxUtil(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Detect elements for given XPaths in current viewport
     */
    public List<ElementBoundingBox> detectElements(List<String> xpaths, int chunkIndex) {
        List<ElementBoundingBox> detectedElements = new ArrayList<>();
        String platform = detectPlatform();

        if (xpaths == null || xpaths.isEmpty()) {
            return detectedElements;
        }

        for (String xpath : xpaths) {
            try {
                List<WebElement> elements = driver.findElements(By.xpath(xpath));
                
                for (WebElement element : elements) {
                    ElementBoundingBox boundingBox = createBoundingBox(element, xpath, chunkIndex, platform);
                    if (boundingBox != null && isElementFullyInViewport(boundingBox)) {
                        detectedElements.add(boundingBox);
                        log.info("Element found: " + boundingBox.toString());
                    }
                }
            } catch (Exception e) {
                log.warning("Failed to detect elements for XPath '" + xpath + "': " + e.getMessage());
            }
        }

        return detectedElements;
    }

    /**
     * Create bounding box from WebElement with absolute coordinates
     */
    private ElementBoundingBox createBoundingBox(WebElement element, String xpath, int chunkIndex, String platform) {
        try {
            // Get element location and size relative to viewport
            Point location = element.getLocation();
            Dimension size = element.getSize();
            
            // Get current scroll position
            Long scrollY = getCurrentScrollPosition();
            
            // Convert to absolute page coordinates
            int absoluteX = location.getX();
            int absoluteY = location.getY() + scrollY.intValue();
            
            return new ElementBoundingBox(xpath, absoluteX, absoluteY, size.getWidth(), size.getHeight(), chunkIndex, platform);
            
        } catch (Exception e) {
            log.warning("Failed to create bounding box for element with XPath '" + xpath + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if element is completely within current viewport
     */
    private boolean isElementFullyInViewport(ElementBoundingBox boundingBox) {
        try {
            Dimension viewportSize = driver.manage().window().getSize();
            Long scrollY = getCurrentScrollPosition();
            
            // Calculate viewport-relative position
            int viewportY = boundingBox.getY() - scrollY.intValue();
            
            // Check if element is completely within viewport
            return boundingBox.getX() >= 0 && 
                   boundingBox.getX() + boundingBox.getWidth() <= viewportSize.getWidth() &&
                   viewportY >= 0 && 
                   viewportY + boundingBox.getHeight() <= viewportSize.getHeight();
                   
        } catch (Exception e) {
            log.warning("Failed to check viewport bounds: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get current scroll position
     */
    private Long getCurrentScrollPosition() {
        try {
            return (Long) ((JavascriptExecutor) driver).executeScript(
                "return window.pageYOffset || document.documentElement.scrollTop || 0;"
            );
        } catch (Exception e) {
            log.warning("Failed to get scroll position: " + e.getMessage());
            return 0L;
        }
    }

    /**
     * Detect platform (iOS/Android, Native/WebView)
     */
    private String detectPlatform() {
        try {
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            String platformName = caps.getPlatformName().toString().toLowerCase();
            String browserName = caps.getBrowserName().toString().toLowerCase();
            
            if (platformName.contains("ios")) {
                return browserName.equals("safari") ? "ios_webview" : "ios_native";
            } else if (platformName.contains("android")) {
                return browserName.equals("chrome") ? "android_webview" : "android_native";
            } else {
                return "web";
            }
        } catch (Exception e) {
            log.warning("Failed to detect platform: " + e.getMessage());
            return "unknown";
        }
    }

    /**
     * Deduplicate elements based on position proximity
     */
    public List<ElementBoundingBox> deduplicateElements(List<ElementBoundingBox> elements) {
        if (elements == null || elements.size() <= 1) {
            return elements;
        }

        List<ElementBoundingBox> uniqueElements = new ArrayList<>();
        Set<String> processedXPaths = new HashSet<>();

        for (ElementBoundingBox element : elements) {
            String xpath = element.getXpath();
            
            if (processedXPaths.contains(xpath)) {
                // Check if this element is too close to an already processed element
                boolean isDuplicate = false;
                for (ElementBoundingBox existing : uniqueElements) {
                    if (existing.getXpath().equals(xpath) && 
                        element.distanceTo(existing) <= PROXIMITY_THRESHOLD) {
                        isDuplicate = true;
                        log.info("Deduplicating element: " + element.toString() + " (too close to: " + existing.toString() + ")");
                        break;
                    }
                }
                
                if (!isDuplicate) {
                    uniqueElements.add(element);
                }
            } else {
                uniqueElements.add(element);
                processedXPaths.add(xpath);
            }
        }

        log.info("Deduplication complete: " + elements.size() + " -> " + uniqueElements.size() + " elements");
        return uniqueElements;
    }

    /**
     * Prepare element data for upload
     */
    public Map<String, Object> prepareUploadData(List<ElementBoundingBox> elements) {
        Map<String, Object> uploadData = new HashMap<>();
        
        // Add metadata
        uploadData.put("timestamp", System.currentTimeMillis());
        uploadData.put("totalElements", elements.size());
        uploadData.put("platform", detectPlatform());
        
        // Add element data
        List<Map<String, Object>> elementData = new ArrayList<>();
        for (ElementBoundingBox element : elements) {
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
        }
        
        uploadData.put("elements", elementData);
        
        return uploadData;
    }
} 