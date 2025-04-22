package io.github.lambdatest.utils;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.PerformsTouchActions;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;

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
    private int samePageCounter = 1;    //Init with value 1 , finalise at 3
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
        Dimension screenSize = driver.manage().window().getSize();
        int screenHeight = screenSize.getHeight();
        int screenWidth = screenSize.getWidth();

        // Define start and end points for scrolling
        int startX = 4; //start from 4 pixels from the left, to avoid click on action items/webview
        int startY = (int) (screenHeight * 0.70); // Start at 70% of the screen height
        int endY = (int) (screenHeight * 0.45);   // Scroll up to 25%
        int scrollHeight = startY - endY;

        try {
            // Try iOS style swipe
            JavascriptExecutor javascriptExecutorIos = (JavascriptExecutor) driver;
//            Map<String, Object> swipeObj = new HashMap<>();
//            swipeObj.put("direction", "up");
//            swipeObj.put("velocity", 400);
//            javascriptExecutorIos.executeScript("mobile: swipe", swipeObj);
            Map<String, Object> swipeObj = new HashMap<>();
            swipeObj.put("fromX", startX);
            swipeObj.put("fromY", startY);
            swipeObj.put("toX", startX);
            swipeObj.put("toY", endY);
            swipeObj.put("duration", 0.5);
            javascriptExecutorIos.executeScript("mobile: dragFromToForDuration", swipeObj);

        } catch (Exception iosException) {
            try {
                // If iOS swipe fails, assume it's Android and do scrollGesture
                JavascriptExecutor jsExecutorAndroid = (JavascriptExecutor) driver;
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("left", startX);
                scrollParams.put("top", endY);
                scrollParams.put("width", screenWidth - startX);
                scrollParams.put("height", scrollHeight);
                scrollParams.put("direction", "down");
                scrollParams.put("percent", 1.0);
                scrollParams.put("speed", 2500);
                jsExecutorAndroid.executeScript("mobile:scrollGesture", scrollParams);
            } catch (Exception e) {
                log.warning("Error during Android scroll operation: " + e.getMessage());
                e.printStackTrace();
            }
        }

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            log.warning("Error during scroll operation: " + e.getMessage());
            e.printStackTrace();
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