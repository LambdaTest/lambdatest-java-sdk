package io.github.lambdatest.utils;


import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    String lastPageSource ="";


    public List<File> captureFullPage() {
        int chunkCount = 0; int maxCount = 10;
        boolean isLastScroll = false;
        List<File> screenshotDir = new ArrayList<>();
        while (!isLastScroll && chunkCount < maxCount) {
            // Capture and save screenshot asynchronously
            File screenshotFile= captureAndSaveScreenshot(this.saveDirectoryName,chunkCount);
            if(screenshotFile != null) {
                screenshotDir.add(screenshotFile);
            }
            //Perform scroll
            scrollDown();
            chunkCount++;
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
        int screenWidth = screenSize.getWidth();
        int screenHeight = screenSize.getHeight();

        // Define start and end points for scrolling
        int startX = screenWidth / 2;
        int startY = (int) (screenHeight * 0.75); // Start at 75% of the screen height
        int endY = (int) (screenHeight * 0.25);   // Scroll up to 25% of the screen height

        try {
        // Create a PointerInput action for touch gestures
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 0);

        // Press (touch) at the start position
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        // Move to end position (scrolling)
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), startX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            if (driver instanceof AppiumDriver) {
                AppiumDriver appiumDriver = (AppiumDriver) driver;
                appiumDriver.perform(Collections.singleton(swipe));
            } else {
                log.warning("Driver is not an instance of AppiumDriver, scrolling by typecasting");
                AppiumDriver appiumDriver = (AppiumDriver) driver;
                appiumDriver.perform(Collections.singleton(swipe));
            }
            // Allow time for UI to update
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
        if (currentPageSource.equals(lastPageSource)) {
            log.info("Reached the bottom of the page — no new content found.");
            return true;
        } else {
            lastPageSource = currentPageSource;
            return false;
        }
    }

}