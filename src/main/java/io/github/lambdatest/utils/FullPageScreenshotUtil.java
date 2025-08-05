package io.github.lambdatest.utils;

import org.openqa.selenium.*;
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

    public FullPageScreenshotUtil(WebDriver driver, String saveDirectoryName) {
        this.driver = driver;
        this.saveDirectoryName = saveDirectoryName;

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
            List<ElementBoundingBox> uniqueElements = elementUtil.deduplicateElements(allElements);
            Map<String, Object> uploadData = elementUtil.prepareUploadData(uniqueElements);
            
            log.info("Element detection complete: " + allElements.size() + " total, " + uniqueElements.size() + " unique elements");
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
            Dimension size = driver.manage().window().getSize();
            int screenHeight = size.getHeight();
            int screenWidth = size.getWidth();
            
            // Use precise scroll parameters similar to Golang implementation
            int scrollEndY = (int) (screenHeight * 0.3); // End at 30% of screen height
            int scrollHeight = (int) (screenHeight * 0.35); // Scroll distance = 35% of screen height
            int scrollWidth = 3; // Narrow width for precise control
            int scrollLeft = screenWidth / 2; // Center the narrow scroll area
            
            log.info("Android scroll parameters - scrollEndY: " + scrollEndY + ", height: " + scrollHeight + ", width: " + scrollWidth);

            // Try Perfecto's native scroll command
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("start", "50%,80%");
                params.put("end", "50%,20%");
                params.put("duration", "5"); // Very slow duration for precise controlled scrolling
                ((JavascriptExecutor) driver).executeScript("mobile:touch:swipe", params);
                log.info("Scroll command succeeded");
                return scrollHeight;
            } catch (Exception ignore) {
                log.info("touch:swipe action failed, trying next method");
            }

            // Try scrollGesture (supported on Android devices) - using precise parameters
            try {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("left", scrollLeft);
                scrollParams.put("top", scrollEndY);
                scrollParams.put("width", scrollWidth);
                scrollParams.put("height", scrollHeight);
                scrollParams.put("direction", "down");
                scrollParams.put("percent", 1.0);
                scrollParams.put("speed", 500); // Very slow speed for precise controlled scrolling
                ((JavascriptExecutor) driver).executeScript("mobile:scrollGesture", scrollParams);
                log.info("scrollGesture scroll succeeded with precise parameters");
                return scrollHeight;
            } catch (Exception ignore) {
                log.info("scrollGesture failed, trying next method");
            }

            // Try iOS-style swipe (supported on iOS devices) - using precise parameters
            try {
                int fromY = scrollEndY + scrollHeight; // Start from bottom of scroll area
                int toY = scrollEndY; // End at top of scroll area
                
                Map<String, Object> swipeObj = new HashMap<>();
                swipeObj.put("fromX", scrollLeft);
                swipeObj.put("fromY", fromY);
                swipeObj.put("toX", scrollLeft);
                swipeObj.put("toY", toY);
                swipeObj.put("duration", 3.5); // Very slow duration for precise controlled scrolling
                ((JavascriptExecutor) driver).executeScript("mobile:dragFromToForDuration", swipeObj);
                log.info("iOS dragFromTo scroll succeeded with precise parameters");
                return scrollHeight;
            } catch (Exception ignore) {
                log.info("iOS dragFromTo scroll failed, trying fallback");
            }

            // Fallback: Use JavaScript to scroll by pixels
            try {
                ((JavascriptExecutor) driver).executeScript(
                        "window.scrollBy(0, arguments[0]);", scrollHeight);
                log.info("JavaScript window.scrollBy succeeded");
                return scrollHeight;
            } catch (Exception e) {
                log.severe("Scroll not supported on this device: " + e.getMessage());
                return 0;
            }
        } catch (Exception e) {
            log.severe("Error in scrollOnDevice: " + e.getMessage());
            return 0;
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
                samePageCounter++;
                log.info("Same page content detected, counter: " + samePageCounter);
                if (samePageCounter >= 3) {
                    log.info("Reached the bottom of the page — no new content found.");
                    samePageCounter = 0;
                    return true;
                }
            } else {
                prevPageSource = currentPageSource;
                samePageCounter = 0;
            }
        } catch (Exception e) {
            log.warning("Error checking if reached bottom: " + e.getMessage());
            samePageCounter = 0;
            return true; // Assume bottom reached on error
        }
        return false;
    }
}