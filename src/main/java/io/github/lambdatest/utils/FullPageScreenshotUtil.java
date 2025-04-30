package io.github.lambdatest.utils;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileBy;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import org.openqa.selenium.*;

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
        if(pageCount<=0){
            pageCount = maxCount;
        }
        if (pageCount < maxCount) {
            maxCount = pageCount;
        }
        int chunkCount = 0;
        boolean isLastScroll = false;
        List<File> screenshotDir = new ArrayList<>();
        while (!isLastScroll && chunkCount < maxCount) {
            File screenshotFile= captureAndSaveScreenshot(this.saveDirectoryName,chunkCount);
            if(screenshotFile != null) {
                screenshotDir.add(screenshotFile);
                chunkCount++;
            }
            //Perform scroll
            scrollDown();
            log.info("Scrolling attempt # " + chunkCount);
            // Detect end of page
            isLastScroll = hasReachedBottom();
        }
        log.info("Finished capturing all screenshots for full page.");
        return screenshotDir;
    }

    private File captureAndSaveScreenshot(String ssDir, int index) {
        File destinationFile = new File(ssDir + "/" + ssDir +"_" + index + ".png");
        try {
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshotFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved screenshot: " + destinationFile.getAbsolutePath());
        } catch (IOException e) {
            log.warning("Error saving screenshot: " + e.getMessage());
        }
        return destinationFile;
    }

    private void scrollDown() {
        try {
            Thread.sleep(1000);
            scrollOnPerfecto((AppiumDriver) driver);
        } catch (Exception e) {
            log.warning("Error during scroll operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void scrollOnPerfecto(AppiumDriver driver) {
        try {
            Dimension size = driver.manage().window().getSize();
            int screenHeight = size.getHeight();
            int screenWidth = size.getWidth();
            int startX = 4;
            int startY = (int) (screenHeight * 0.70);
            int endY = (int) (screenHeight * 0.45);
            int scrollHeight = startY - endY;

            // First try Perfecto's native scroll command
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("start", "50%,80%");
                params.put("end", "50%,20%");
                params.put("duration", "2");

                driver.executeScript("mobile:touch:swipe", params);
                log.info("Perfecto scroll command succeeded");
                return;
            } catch (Exception ignore) {}

            //Trying Scroll Gestures to scroll - supported on Android devices
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
                log.info("ScrollGestures scroll succeeded");
            } catch (Exception ignore) {}

            //Try ios style swipe - supported on IOS devices
            try {
                Map<String, Object> swipeObj = new HashMap<>();
                swipeObj.put("fromX", startX);
                swipeObj.put("fromY", startY);
                swipeObj.put("toX", startX);
                swipeObj.put("toY", endY);
                swipeObj.put("duration", 0.8);
                ((JavascriptExecutor) driver).executeScript("mobile: dragFromToForDuration", swipeObj);
                log.info("DragFromTo scroll succeeded");
            } catch (Exception ignore) {}
            // Fallback to TouchAction if none work
            try {
            TouchAction touchAction = new TouchAction(driver);
            touchAction.press(PointOption.point(startX, startY))
                    .waitAction(WaitOptions.waitOptions(Duration.ofMillis(1000))) // Longer wait for Perfecto
                    .moveTo(PointOption.point(startX, endY))
                    .release()
                    .perform();
            log.info("TouchAction scroll succeeded");
            } catch (Exception ignored) {}
        } catch (Exception e) {
            log.severe("Scroll not supported on this device : " + e.getMessage());
        }
    }

    private boolean hasReachedBottom() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
        return false;
    }
}