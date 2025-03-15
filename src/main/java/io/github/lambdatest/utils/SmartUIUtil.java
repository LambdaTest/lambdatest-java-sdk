package io.github.lambdatest.utils;

import java.util.logging.Logger;
import org.json.JSONObject;
import com.google.gson.Gson;
import io.github.lambdatest.constants.Constants;
import io.github.lambdatest.models.Snapshot;
import io.github.lambdatest.models.SnapshotData;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

public class SmartUIUtil {
    private final HttpClientUtil httpClient;
    private Logger log;

    public SmartUIUtil() {
        this.httpClient = new HttpClientUtil();
        this.log = LoggerUtil.createLogger("lambdatest-java-sdk");
    }

    public boolean isSmartUIRunning() {
        try {
            httpClient.isSmartUIRunning();
            return true;
        } catch (Exception e) {
            log.severe(e.getMessage());
            return false;
        }
    }

    public String fetchDOMSerializer() throws Exception {
        try {
            return httpClient.fetchDOMSerializer();
        } catch (Exception e) {
            log.severe(e.getMessage());
            throw new Exception(Constants.Errors.FETCH_DOM_FAILED, e);
        }
    }

    public String postSnapshot(Object snapshotDOM, Map<String, Object> options, String url, String snapshotName, String testType) throws Exception {
        // Create Snapshot and SnapshotData objects
        Snapshot snapshot = new Snapshot();
        snapshot.setDom(snapshotDOM);
        snapshot.setName(snapshotName);
        snapshot.setOptions(options);
        snapshot.setURL(url);

        SnapshotData data = new SnapshotData();
        data.setSnapshot(snapshot);
        data.setTestType(testType);

        // Serialize to JSON using Gson
        Gson gson = new Gson();
        String jsonData = gson.toJson(data);

        try {
            return httpClient.postSnapshot(jsonData);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getSmartUIServerAddress() {
        String smartUiServerAddress = System.getenv(Constants.SMARTUI_SERVER_ADDRESS);
        if (smartUiServerAddress != null && !smartUiServerAddress.isEmpty()) {
            return smartUiServerAddress;
        } else {
            return "http://localhost:49152";
        }
    }
    
    
}