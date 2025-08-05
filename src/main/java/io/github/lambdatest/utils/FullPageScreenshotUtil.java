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
            int startX = screenWidth / 2; // Center of the screen
            int startY = (int) (screenHeight * 0.70); // Start at 70% of screen height
            int endY = (int) (screenHeight * 0.45); // End at 45% of screen height
            int scrollHeight = startY - endY;

            // Try Perfecto's native scroll command
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("start", "50%,80%");
                params.put("end", "50%,20%");
                params.put("duration", "2");
                ((JavascriptExecutor) driver).executeScript("mobile:touch:swipe", params);
                log.info("Scroll command succeeded");
                return scrollHeight;
            } catch (Exception ignore) {
                log.info("touch:swipe action failed, trying next method");
            }

            // Try scrollGesture (supported on Android devices)
            try {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("left", startX);
                scrollParams.put("top", endY);
                scrollParams.put("width", screenWidth - startX);
                scrollParams.put("height", scrollHeight);
                scrollParams.put("direction", "down");
                scrollParams.put("percent", 1.0);
                scrollParams.put("speed", 2500);
                ((JavascriptExecutor) driver).executeScript("mobile:scrollGesture", scrollParams);
                log.info("scrollGesture scroll succeeded");
                return scrollHeight;
            } catch (Exception ignore) {
                log.info("scrollGesture failed, trying next method");
            }

            // Try iOS-style swipe (supported on iOS devices)
            try {
                Map<String, Object> swipeObj = new HashMap<>();
                swipeObj.put("fromX", startX);
                swipeObj.put("fromY", startY);
                swipeObj.put("toX", startX);
                swipeObj.put("toY", endY);
                swipeObj.put("duration", 0.8);
                ((JavascriptExecutor) driver).executeScript("mobile:dragFromToForDuration", swipeObj);
                log.info("iOS dragFromTo scroll succeeded");
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