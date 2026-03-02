package io.github.lambdatest;

import org.openqa.selenium.WebDriver;
import io.github.lambdatest.utils.LoggerUtil;
import io.github.lambdatest.utils.SmartUIUtil;
import io.github.lambdatest.constants.Constants;
import org.json.JSONObject;

import java.util.logging.Logger;

/**
 * Provides methods to fetch aggregate visual comparison results from SmartUI.
 *
 * <p>Two overloaded methods are available:
 * <ul>
 *   <li>{@link #smartuiResults(WebDriver)} - fetches results for a specific session (driver's sessionId)</li>
 *   <li>{@link #smartuiResults()} - fetches results for the entire active build</li>
 * </ul>
 */
public class SmartUIResults {

    private static final Logger log = LoggerUtil.createLogger("lambdatest-java-sdk");
    private static final SmartUIUtil smartUIUtils = new SmartUIUtil();

    /**
     * Fetches SmartUI visual comparison results for the session associated with the given WebDriver.
     *
     * <p>This method extracts the sessionId from the RemoteWebDriver and queries the SmartUI server
     * for screenshot data filtered by that session. The response contains screenshots grouped by name
     * with a summary indicating variant and screenshot counts.
     *
     * @param driver A Selenium WebDriver instance (must be a RemoteWebDriver)
     * @return JSONObject containing screenshots grouped by name and a summary with type "session"
     * @throws Exception if the driver is null, not a RemoteWebDriver, or the request fails
     */
    public static JSONObject smartuiResults(WebDriver driver) throws Exception {
        if (driver == null) {
            throw new IllegalArgumentException(Constants.Errors.SELENIUM_DRIVER_NULL);
        }

        if (!smartUIUtils.isSmartUIRunning()) {
            throw new IllegalStateException(Constants.Errors.SMARTUI_NOT_RUNNING);
        }

        try {
            // Extract sessionId from the driver (null-safe)
            org.openqa.selenium.remote.SessionId sid = ((org.openqa.selenium.remote.RemoteWebDriver) driver).getSessionId();
            if (sid == null) {
                throw new IllegalStateException("Unable to get sessionId from the driver");
            }
            String sessionId = sid.toString();

            log.info("Fetching SmartUI results for sessionId: " + sessionId);

            // CLI server resolves buildId and projectToken from its context
            String resultsResponse = smartUIUtils.getSmartUIResults(sessionId);
            log.info("SmartUI results fetched successfully for sessionId: " + sessionId);

            return new JSONObject(resultsResponse);

        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Driver must be an instance of RemoteWebDriver to extract sessionId", e);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Fetches SmartUI visual comparison results for the entire active build.
     *
     * <p>This method queries the SmartUI server for all screenshot data in the current build
     * without filtering by session. The response contains screenshots grouped by name
     * with a summary indicating variant and screenshot counts.
     *
     * @return JSONObject containing screenshots grouped by name and a summary with type "build"
     * @throws Exception if the SmartUI server is not running or the request fails
     */
    public static JSONObject smartuiResults() throws Exception {
        if (!smartUIUtils.isSmartUIRunning()) {
            throw new IllegalStateException(Constants.Errors.SMARTUI_NOT_RUNNING);
        }

        try {
            log.info("Fetching SmartUI results for entire build");

            // CLI server resolves buildId from its active build context
            String resultsResponse = smartUIUtils.getSmartUIResults(null);
            log.info("SmartUI results fetched successfully for build");

            return new JSONObject(resultsResponse);

        } catch (Exception e) {
            throw e;
        }
    }
}
