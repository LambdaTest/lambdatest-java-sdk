//package io.github.lambdatest.test;
//
//import org.testng.annotations.Test;
//import io.github.lambdatest.utils.SmartUIUtil;
//
//import io.appium.java_client.AppiumDriver;
//import io.github.lambdatest.SmartUIAppSnapshot;
//import org.openqa.selenium.Platform;
//import org.openqa.selenium.remote.DesiredCapabilities;
//
//import java.net.URL;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//import java.util.logging.Logger;
//
//public class Perfecto_SmartUi_Native {
//    private AppiumDriver driver;
//    private SmartUIAppSnapshot smartUIAppSnapshot = new SmartUIAppSnapshot();
//    private SmartUIUtil smartUIUtil = new SmartUIUtil();
//    private final Logger log;
//
//    // Default constructor
//    public Perfecto_SmartUi_Native() {
//        this.log = Logger.getLogger("lt-smart-ui-java-sdk");
//    }
//    // Parameterized constructor
//    public Perfecto_SmartUi_Native(AppiumDriver driver,SmartUIAppSnapshot smartUIAppSnapshot, SmartUIUtil smartUIUtil) {
//        this();
//        this.driver = driver;
//        this.smartUIAppSnapshot = smartUIAppSnapshot;
//        this.smartUIUtil = smartUIUtil;
//    }
//
//    @Test
//    public void appiumTest() throws Exception{
//        Map<String, Object>options = new HashMap<>();
//        options.put("projectToken", "DummyTest12345");
//        if(!options.containsKey("buildName")) {
//            options.put("buildName", "smartui-" + UUID.randomUUID().toString().substring(0, 8));
//        }
//
//        if (smartUIUtil == null) {
//            throw new IllegalStateException("smartUIUtil is not initialized!");
//        }
////        String projectToken = smartUIUtil.getProjectToken(options);
//        String deviceName = "AdityaTest";
//        DesiredCapabilities cap = new DesiredCapabilities(deviceName, "1.1", Platform.ANY);
////        cap.setCapability("projectToken", projectToken);
//        cap.setCapability("enableAppiumBehavior", true);
//        cap.setCapability("model", "Galaxy S24");
//        cap.setCapability("platformVersion", "14");
//        cap.setCapability("platformName", Platform.ANDROID.name());
//        cap.setCapability("deviceName",deviceName);
//
//        try {
//            String perfectoURL = "ourCustomCloudUrl"; // Replace with actual Perfecto cloud URL
//            this.driver = new AppiumDriver(new URL("https://" + perfectoURL + "/nexperience/perfectomobile/wd/hub"), cap);
//            driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
//            log.info("Driver created with capabilities: "+ driver.getCapabilities());
//        } catch (RuntimeException e){
//            log.warning("Couldn't create driver with capabilities: "  + cap +" due to error: "+ e.getMessage());
//            throw  new RuntimeException("Could not create driver with capabilities due to Exception: "+ e.getMessage());
//        }
////        smartUIAppSnapshot.start(driver, "testaditya123", options);
//        smartUIAppSnapshot.smartUiAppSnapshot(driver, "testScreenshot");
//        smartUIAppSnapshot.stop();
//    }
//}
