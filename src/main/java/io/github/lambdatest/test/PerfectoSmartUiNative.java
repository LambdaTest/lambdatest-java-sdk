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

    // Default constructor
    public PerfectoSmartUiNative() {
        this.log = Logger.getLogger("lt-smart-ui-java-sdk");
    }
    // Parameterized constructor
    public PerfectoSmartUiNative(AppiumDriver driver,SmartUIAppSnapshot smartUIAppSnapshot, SmartUIUtil smartUIUtil) {
        this.driver = driver;
        this.smartUIAppSnapshot = smartUIAppSnapshot;
    }

    @BeforeTest
    public void setup() throws Exception {

        Map<String, String> option = new HashMap<>();
        option.put("projectToken", "<lambdatest-token>");
        option.put("buildName", "testBuild");
        DesiredCapabilities cap = new DesiredCapabilities();
        cap.setCapability("enableAppiumBehavior", true);
        cap.setCapability("model", "Galaxy S24");
        cap.setCapability("platformVersion", "14");
        cap.setCapability("platformName", Platform.ANDROID.name());
        cap.setCapability("ltOptions", option);


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

        Map<String, String> options = new HashMap<>();
        Object capabilityObj = driver.getCapabilities().getCapability("ltOptions");

        if (Objects.nonNull(capabilityObj) && capabilityObj instanceof Map) {
            Map<?, ?> capabilityMap = (Map<?, ?>) capabilityObj;

            // Extract projectToken if it exists
            if (capabilityMap.containsKey("projectToken") && capabilityMap.get("projectToken") instanceof String) {
                String projectToken = ((String) capabilityMap.get("projectToken")).trim();
                options.put("projectToken", projectToken);
            }

            // Extract buildName if it exists
            if (capabilityMap.containsKey("buildName") && capabilityMap.get("buildName") instanceof String) {
                String buildName = (String) ((String) capabilityMap.get("buildName")).trim();
                options.put("buildName", buildName);
            }
        }

        try{
            smartUIAppSnapshot.start(options);}
        catch (Exception e){
            log.severe("Exception in starting smartUiApp: "+ e.getMessage());
            return;
        }

        // Capture and upload visual snapshot
        smartUIAppSnapshot.smartUiAppSnapshot(driver, "home_screen", options);

        // More actions...
        // e.g., Navigate,to another UI
        smartUIAppSnapshot.smartUiAppSnapshot(driver, "after_navigation", options);

        try{
            smartUIAppSnapshot.stop();} catch (Exception e){
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