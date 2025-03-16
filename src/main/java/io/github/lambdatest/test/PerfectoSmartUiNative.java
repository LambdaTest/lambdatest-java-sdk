package io.github.lambdatest.test;

import io.appium.java_client.AppiumDriver;
import io.github.lambdatest.utils.SmartUIUtil;
import io.github.lambdatest.SmartUIAppSnapshot;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PerfectoSmartUiNative {
    private AppiumDriver driver;
    private SmartUIAppSnapshot smartUIAppSnapshot = new SmartUIAppSnapshot();
    private final SmartUIUtil smartUIUtil = new SmartUIUtil();
    private Logger log;

    public PerfectoSmartUiNative() {
        this.log = Logger.getLogger("lt-smart-ui-java-sdk");
    }

    public PerfectoSmartUiNative(AppiumDriver driver,SmartUIAppSnapshot smartUIAppSnapshot, SmartUIUtil smartUIUtil) {
        this.driver = driver;
        this.smartUIAppSnapshot = smartUIAppSnapshot;
    }

    @BeforeTest
    public void setup() throws Exception {
        DesiredCapabilities cap = new DesiredCapabilities();
        cap.setCapability("enableAppiumBehavior", true);
        cap.setCapability("model", "Galaxy S24");
        cap.setCapability("platformVersion", "14");
        cap.setCapability("platformName", Platform.ANDROID.name());

        try {
            String PERFECTO_URL = "your-cloud.perfectomobile.com";  //Custom cloud url
            this.driver = new AppiumDriver(new URL("https://" + PERFECTO_URL + "/nexperience/perfectomobile/wd/hub"), cap);
            driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
            log.info("Driver created successfully! with capabilities :"+ cap);
        } catch (Exception e) {
            log.warning("Couldn't create driver due to: " + e.getMessage());
            throw new Exception("Couldn't create driver due to: " + e.getMessage());
        }
    }

    @Test
    public void appiumTest() throws Exception {

        Map<String, String> options = new HashMap<>();
        options.put("projectToken", "<lambdatest-token>");
        options.put("buildName", "testBuild"); //Optional name for your SmartUI Build
        options.put("deviceName", "Samsung S24");
        options.put("platform", "Android 15");
        try{
            smartUIAppSnapshot.start(options);}
        catch (Exception e){
            log.severe("Exception in starting smartUiApp: "+ e.getMessage());
            return;
        }

        // Capture and upload visual snapshot
        smartUIAppSnapshot.smartuiAppSnapshot(driver, "home_screen", options);

        // More actions...
        // e.g., Navigate,to another UI
        smartUIAppSnapshot.smartuiAppSnapshot(driver, "after_navigation", options);

        try{
            smartUIAppSnapshot.stop();
        } catch (Exception e){
            log.severe("Stop Smart UI failed" + " due to "+ e.getMessage());
        }
    }

    @AfterTest
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            log.info("Driver closed successfully. Reached after test annotation!");
        }
    }
}