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
    private SmartUIUtil smartUIUtil = new SmartUIUtil();
    private final Logger log;

    private final String SMARTUI_PROJECT_TOKEN = System.getenv("PROJECT_TOKEN");


    // Default constructor
    public PerfectoSmartUiNative() {
        this.log = Logger.getLogger("lt-smart-ui-java-sdk");
    }
    // Parameterized constructor
    public PerfectoSmartUiNative(AppiumDriver driver,SmartUIAppSnapshot smartUIAppSnapshot, SmartUIUtil smartUIUtil) {
        this();
        this.driver = driver;
        this.smartUIAppSnapshot = smartUIAppSnapshot;
        this.smartUIUtil = smartUIUtil;
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
        } catch (RuntimeException e) {
            log.warning("Couldn't create driver due to: " + e.getMessage());
        }
    }

    @Test
    public void appiumTest() throws Exception {
        if (Objects.isNull(driver)) {
            throw new IllegalStateException("Driver not initialized!");
        }
        if(Objects.isNull(smartUIUtil) || Objects.isNull(smartUIAppSnapshot)){
            throw new IllegalStateException("Terminating session for "+ driver.getCapabilities());
        }

        // Start SmartUI Visual Testing
        try{
        smartUIAppSnapshot.start(driver,  new HashMap<>());}
        catch (Exception e){
            log.severe("Exception in starting smartUiAppSnapshot: "+ e.getMessage());
            return;
        }

        // Capture and upload visual snapshot
        smartUIAppSnapshot.smartUiAppSnapshot(driver, "home_screen", new HashMap<>());

        // More actions...
        // e.g., Navigate,to another UI
        smartUIAppSnapshot.smartUiAppSnapshot(driver, "after_navigation", new HashMap<>());

        smartUIAppSnapshot.stop();
    }

    @AfterTest
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            log.info("Driver closed successfully. Reached after test annotation!");
        }
    }
}

