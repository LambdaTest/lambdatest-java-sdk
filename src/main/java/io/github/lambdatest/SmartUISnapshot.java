package io.github.lambdatest;

import org.openqa.selenium.WebDriver;
import io.github.lambdatest.utils.LoggerUtil;
import io.github.lambdatest.utils.SmartUIUtil;
import io.github.lambdatest.constants.Constants;
import io.github.lambdatest.models.ResponseData;
import io.github.lambdatest.models.SnapshotResponse;
import org.openqa.selenium.JavascriptExecutor;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.json.JSONObject;
import java.util.List;
import java.io.IOException;


public class SmartUISnapshot {

    // Method with options parameter
    public static void smartuiSnapshot(WebDriver driver, String snapshotName, Map<String, Object> options, String testType) throws Exception {
        if (driver == null) {
            throw new IllegalArgumentException(Constants.Errors.SELENIUM_DRIVER_NULL);
        }
        if (snapshotName == null || snapshotName.isEmpty()) {
            throw new IllegalArgumentException(Constants.Errors.SNAPSHOT_NAME_NULL);
        }
        Logger log = LoggerUtil.createLogger("lambdatest-java-sdk");

        Gson gson = new Gson();

        SmartUIUtil smartUIUtils = new SmartUIUtil();

        if (!smartUIUtils.isSmartUIRunning()) {
            throw new IllegalStateException(Constants.Errors.SMARTUI_NOT_RUNNING);
        }
        try {
            // Check if driver is an instance of JavascriptExecutor
            if (driver instanceof JavascriptExecutor) {
                JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

                // Fetch DOM serializer and execute in Selenium
                String response = smartUIUtils.fetchDOMSerializer();
                if (response == null || response.isEmpty()) {
                    throw new IllegalStateException(Constants.Errors.EMPTY_RESPONSE_DOMSERIALIZER);
                }

                // Parse the response JSON into ResponseData object
                ResponseData responseData = gson.fromJson(response, ResponseData.class);

                // Validate the responseData and extract DOM string
                if (responseData == null || responseData.getData() == null || responseData.getData().getDom() == null || responseData.getData().getDom().isEmpty()) {
                    throw new IllegalStateException(Constants.Errors.INVALID_RESPONSE_DATA);
                }

                String domString = responseData.getData().getDom();

                // Execute script with the fetched DOM string
                ((JavascriptExecutor) driver).executeScript(domString);

                // Append sessionId to options
                String sessionId = ((org.openqa.selenium.remote.RemoteWebDriver) driver).getSessionId().toString();
                if (!sessionId.isEmpty()) {
                    options.put("sessionId", sessionId);
                }

                // Convert the options map to JSON string
                String jsonOptions = gson.toJson(options);

                // Use String.format to inject the JSON options into the script
                String script = String.format("return {'dom':SmartUIDOM.serialize(%s)}", jsonOptions);

                Map<String, Object> resultMap = (Map<String, Object>) jsExecutor.executeScript(script);
                if (resultMap == null || !resultMap.containsKey("dom")) {
                    throw new IllegalStateException(Constants.Errors.NULL_RESULT_MAP);
                }

                Map<String, Object> dom = (Map<String, Object>) resultMap.get("dom");
                if (dom == null || !dom.containsKey("html")) {
                    throw new IllegalStateException(Constants.Errors.MISSING_HTML_KEY);
                }

                String url = driver.getCurrentUrl();

                String ResponseMap = smartUIUtils.postSnapshot(dom, options, url, snapshotName, testType);

                // Parse the JSON response into a SnapshotResponse object using Gson
                SnapshotResponse postSnapResponse = gson.fromJson(ResponseMap, SnapshotResponse.class);

                List<String> warnings = postSnapResponse.getData().getWarnings();

                // Check if there are any warnings
                if (warnings != null && !warnings.isEmpty()) {
                    for (String warning : warnings) {
                        log.warning(warning);
                    }
                }

                log.info("Snapshot captured: " + snapshotName);
            } else {
                throw new IllegalStateException(Constants.Errors.JAVA_SCRIPT_NOT_SUPPORTED);
            }

        } catch (Exception e) {
            log.severe(String.format(Constants.Errors.SMARTUI_SNAPSHOT_FAILED, snapshotName));
        }
    }

    // Overloaded method without options parameter
    public static void smartuiSnapshot(WebDriver driver, String snapshotName) throws Exception {
        smartuiSnapshot(driver, snapshotName, new HashMap<>()); // Pass an empty map for options
    }

    public static void smartuiSnapshot(WebDriver driver, String snapshotName, Map<String, Object> options) throws Exception {
        smartuiSnapshot(driver, snapshotName, options, "lambdatest-java-sdk");
    }
}
