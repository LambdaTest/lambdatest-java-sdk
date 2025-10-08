package io.github.lambdatest;

import io.github.lambdatest.constants.Constants;
import io.github.lambdatest.constants.DeviceSpecs;
import io.github.lambdatest.exceptions.SmartUIException;
import io.github.lambdatest.utils.LoggerUtil;
import org.openqa.selenium.chromium.HasCdp;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Main SmartUI class for server management and snapshot functionality
 */
public class SmartUI {
    private final SmartUIConfig config;
    private Process serverProcess;
    private boolean isServerRunning = false;
    private final Map<String, Object> configMap = new HashMap<>();

    private static final Logger log = LoggerUtil.createLogger("lambdatest-java-sdk");
    private static final String SMARTUI_CLI_COMMAND = "smartui";

    public SmartUI(SmartUIConfig config) {
        this.config = config;
        if (config.getProjectToken() == null || config.getProjectToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Project token is required");
        }

        String configFile = config.getConfigFile();
        if (configFile != null && !configFile.trim().isEmpty()) {
            parseConfigFile(configFile);
        }
    }

    public void startServer() throws SmartUIException {
        if (isServerRunning) {
            log.info("Server is already running");
            return;
        }

        if (!isSmartUICLIInstalled()) {
            log.info("SmartUI CLI is not installed, installing it globally.");
            installSmartUICLI();
        }

        try {
            startServerWithRetry();

            waitForServerReady();

            isServerRunning = true;
            log.info("SmartUI server started successfully on " + config.getServerAddress());

        } catch (Exception e) {
            isServerRunning = false;
            throw new SmartUIException("Failed to start SmartUI server: " + e.getMessage(), e);
        }
    }

    public void stopServer() throws SmartUIException {
        if (!isServerRunning) {
            log.info("Server is not running");
            return;
        }

        try {
            log.info("Stopping SmartUI server...");

            if (stopServerViaCLI(config.getPort())) {
                isServerRunning = false;
                log.info("SmartUI server stopped successfully via CLI");
            } else {
                log.warning("Server may not have stopped completely");
                isServerRunning = false;
            }

        } catch (Exception e) {
            log.severe("Error during server shutdown: " + e.getMessage());
            forceCleanup();
            throw new SmartUIException("Failed to stop SmartUI server: " + e.getMessage(), e);
        }
    }

    public void takeSnapshot(org.openqa.selenium.WebDriver driver, String snapshotName, Map<String, Object> options)
            throws SmartUIException {
        if (!isServerRunning) {
            throw new SmartUIException("Cannot take snapshot: SmartUI server is not running");
        }

        if (!isServerHealthy()) {
            throw new SmartUIException("Cannot take snapshot: SmartUI server is not healthy");
        }

        if (driver == null) {
            throw new SmartUIException("Cannot take snapshot: WebDriver is null");
        }

        if (snapshotName == null || snapshotName.trim().isEmpty()) {
            throw new SmartUIException("Cannot take snapshot: Snapshot name is null or empty");
        }

        if (options == null) {
            options = new HashMap<>();
        }

        try {
            log.info("Taking snapshot: " + snapshotName);

            System.setProperty(Constants.SMARTUI_SERVER_ADDRESS, config.getServerAddress());

            String testType = config.getTestType();
            Object resizeViewports = options.remove("resizeViewports");

            captureSnapshot(driver, snapshotName, options, testType);

            if (resizeViewports instanceof java.util.List) {
                java.util.List<?> viewportList = (java.util.List<?>) resizeViewports;
                processWebViewports(driver, snapshotName, options, testType, viewportList);
                processMobileDevices(driver, snapshotName, options, testType, viewportList);
            }

            clearDeviceMetrics(driver);

            log.info("Snapshot captured successfully: " + snapshotName);

        } catch (Exception e) {
            String errorMsg = "Failed to take snapshot '" + snapshotName + "': " + e.getMessage();
            log.severe(errorMsg);
            throw new SmartUIException(errorMsg, e);
        }
    }

    public void takeSnapshot(org.openqa.selenium.WebDriver driver, String snapshotName) throws SmartUIException {
        takeSnapshot(driver, snapshotName, new HashMap<>());
    }

    private boolean isSmartUICLIInstalled() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                processBuilder.command("cmd", "/c", "where", SMARTUI_CLI_COMMAND);
            } else {
                processBuilder.command("which", SMARTUI_CLI_COMMAND);
            }

            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.warning("Failed to check SmartUI CLI installation: " + e.getMessage());
            return false;
        }
    }

    private void installSmartUICLI() throws SmartUIException {
        log.info("SmartUI CLI not found. Attempting to install...");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                processBuilder.command("cmd", "/c", "npm", "install", "-g", "@lambdatest/smartui-cli@latest");
            } else {
                processBuilder.command("npm", "install", "-g", "@lambdatest/smartui-cli@latest");
            }

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new SmartUIException("Failed to install SmartUI CLI. Exit code: " + exitCode);
            }

            log.info("SmartUI CLI installed successfully");
        } catch (Exception e) {
            throw new SmartUIException("Failed to install SmartUI CLI: " + e.getMessage(), e);
        }
    }

    private void startServerWithRetry() throws SmartUIException {
        int maxRetries = 1;
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                startServerProcess();
                return;
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    log.warning("Server start attempt " + (attempt + 1) + " failed, retrying...");
                    try {
                        stopServerViaCLI(config.getPort());
                        Thread.sleep(2000);
                    } catch (Exception ie) {
                        log.warning("Failed to stop existing server before retry: " + ie.getMessage());
                    }
                }
            }
        }

        throw new SmartUIException("Failed to start server after " + (maxRetries + 1) + " attempts", lastException);
    }

    private void startServerProcess() throws SmartUIException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            java.util.List<String> command = new java.util.ArrayList<>();

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                command.add("cmd");
                command.add("/c");
            }

            command.add(SMARTUI_CLI_COMMAND);
            command.add("exec:start");
            command.add("-P");
            command.add(String.valueOf(config.getPort()));

            if (config.getBuildName() != null && !config.getBuildName().trim().isEmpty()) {
                command.add("--buildName");
                command.add(config.getBuildName());
            }

            if (config.getConfigFile() != null && !config.getConfigFile().trim().isEmpty()) {
                command.add("--config");
                command.add(config.getConfigFile());
            }

            Map<String, String> env = processBuilder.environment();
            env.put("PROJECT_TOKEN", config.getProjectToken());
            env.put("SMARTUI_SERVER_ADDRESS", "http://localhost:" + config.getPort());

            logCliContext("start", command, env);

            processBuilder.command(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            streamProcessOutput(process, "[CLI start] ");

            Thread.sleep(2000);

            if (!pingServer()) {
                log.warning("CLI start command executed, but server is not responding yet");
            } else {
                log.info("CLI start command executed successfully, server is responding");
            }

        } catch (Exception e) {
            throw new SmartUIException("Failed to start server process: " + e.getMessage(), e);
        }
    }

    private void waitForServerReady() throws SmartUIException {
        long startTime = System.currentTimeMillis();
        long timeout = 20000;

        while (System.currentTimeMillis() - startTime < timeout) {
            if (pingServer()) {
                log.info("Server is now ready and responding");
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SmartUIException("Interrupted while waiting for server", e);
            }
        }

        throw new SmartUIException("Server did not become ready within 20 seconds");
    }

    public boolean pingServer() {
        try {
            URL url = new URL(config.getServerAddress() + "/healthcheck");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            connection.setRequestProperty("User-Agent", "SmartUI-Java-SDK/1.0");

            int responseCode = connection.getResponseCode();
            return responseCode == 200;

        } catch (Exception e) {
            return false;
        }
    }

    private void forceCleanup() {
        log.warning("Performing forced cleanup...");
        try {
            if (serverProcess != null && serverProcess.isAlive()) {
                serverProcess.destroyForcibly();
                serverProcess = null;
            }
            isServerRunning = false;
        } catch (Exception e) {
            log.severe("Error during forced cleanup: " + e.getMessage());
        }
    }

    public boolean isServerHealthy() {
        try {
            if (!isServerRunning) {
                return false;
            }

            if (!pingServer()) {
                return false;
            }

            if (serverProcess != null && !serverProcess.isAlive()) {
                log.warning("Server process is no longer alive");
                isServerRunning = false;
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warning("Error checking server health: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stop server using SmartUI CLI command
     *
     * @param port The port where server is running
     * @return true if CLI command succeeded, false otherwise
     */
    public boolean stopServerViaCLI(int port) {
        try {
            ProcessBuilder stopProcessBuilder = new ProcessBuilder();
            java.util.List<String> stopCommand = new java.util.ArrayList<>();

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                stopCommand.add("cmd");
                stopCommand.add("/c");
            }

            stopCommand.add(SMARTUI_CLI_COMMAND);
            stopCommand.add("exec:stop");

            Map<String, String> env = stopProcessBuilder.environment();
            env.put("PROJECT_TOKEN", config.getProjectToken());
            env.put("SMARTUI_SERVER_ADDRESS", "http://localhost:" + port);

            logCliContext("stop", stopCommand, env);

            stopProcessBuilder.directory(new java.io.File(System.getProperty("user.dir")));
            stopProcessBuilder.redirectErrorStream(true);
            stopProcessBuilder.command(stopCommand);
            Process stopProcess = stopProcessBuilder.start();

            StringBuilder output = new StringBuilder();
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stopProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[CLI stop] " + line);
                        output.append(line).append("\n");
                    }
                } catch (Exception ex) {
                    log.fine("Failed to read CLI output: " + ex.getMessage());
                }
            });
            outputThread.setDaemon(true);
            outputThread.start();

            int exitCode = stopProcess.waitFor();

            outputThread.join(1000);

            String outputString = output.toString();

            if (exitCode == 0) {
                if (outputString.contains("Server stopped successfully")) {
                    log.info("Server stopped successfully");
                    return true;
                } else {
                    log.warning("Server couldn't be stopped - unexpected output");
                    return false;
                }
            } else {
                log.warning("CLI stop command failed with exit code: " + exitCode);
                return false;
            }

        } catch (Exception e) {
            log.warning("Failed to execute CLI stop command: " + e.getMessage());
            return false;
        }
    }

    private void logCliContext(String action, java.util.List<String> command, Map<String, String> env) {
        try {
            StringBuilder cmd = new StringBuilder();
            for (String part : command) {
                if (part.contains(" ") || part.contains("\t")) {
                    cmd.append('"').append(part).append('"');
                } else {
                    cmd.append(part);
                }
                cmd.append(' ');
            }
            String token = env.get("PROJECT_TOKEN");
            String maskedToken = token == null ? "null" : maskToken(token);
            String serverAddr = env.get("SMARTUI_SERVER_ADDRESS");
            String workingDir = System.getProperty("user.dir");
            log.info("[CLI " + action + "] Command: " + cmd.toString().trim());
            log.info("[CLI " + action + "] Working Directory: " + workingDir);
            log.info("[CLI " + action + "] Env SMARTUI_SERVER_ADDRESS=" + serverAddr);
            log.info("[CLI " + action + "] Env PROJECT_TOKEN=" + maskedToken);
        } catch (Exception ex) {
            log.fine("Failed to log CLI context: " + ex.getMessage());
        }
    }

    private String maskToken(String token) {
        if (token.length() <= 6)
            return "******";
        String suffix = token.substring(Math.max(0, token.length() - 6));
        return "******" + suffix;
    }

    private void streamProcessOutput(Process process, String prefix) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(prefix + line);
                }
            } catch (Exception ex) {
                log.fine("Failed to read CLI output: " + ex.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public Map<String, Object> getConfigMap() {
        return configMap;
    }

    private void parseConfigFile(String filePath) {
        try {
            File configFile = new File(filePath);
            if (!configFile.exists()) {
                log.warning("Config file not found: " + filePath);
                return;
            }

            if (!configFile.isFile()) {
                log.warning("Config path is not a file: " + filePath);
                return;
            }

            try (FileReader reader = new FileReader(configFile)) {
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

                if (jsonObject.has("web")) {
                    JsonObject webConfig = jsonObject.getAsJsonObject("web");
                    Map<String, Object> webMap = new HashMap<>();

                    if (webConfig.has("browsers")) {
                        webMap.put("browsers", new Gson().fromJson(webConfig.get("browsers"), java.util.List.class));
                    }
                    if (webConfig.has("viewports")) {
                        webMap.put("viewports", new Gson().fromJson(webConfig.get("viewports"), java.util.List.class));
                    }

                    configMap.put("web", webMap);
                }

                if (jsonObject.has("mobile")) {
                    JsonObject mobileConfig = jsonObject.getAsJsonObject("mobile");
                    Map<String, Object> mobileMap = new HashMap<>();

                    if (mobileConfig.has("devices")) {
                        mobileMap.put("devices",
                                new Gson().fromJson(mobileConfig.get("devices"), java.util.List.class));
                    }
                    if (mobileConfig.has("fullPage")) {
                        mobileMap.put("fullPage", mobileConfig.get("fullPage").getAsBoolean());
                    }
                    if (mobileConfig.has("orientation")) {
                        mobileMap.put("orientation", mobileConfig.get("orientation").getAsString());
                    }

                    configMap.put("mobile", mobileMap);
                }

                log.info("Successfully parsed config file: " + filePath);

            } catch (IOException e) {
                log.warning("Failed to read config file: " + filePath + " - " + e.getMessage());
            } catch (Exception e) {
                log.warning("Failed to parse config file: " + filePath + " - " + e.getMessage());
            }

        } catch (Exception e) {
            log.warning("Error processing config file: " + filePath + " - " + e.getMessage());
        }
    }

    private void captureSnapshot(org.openqa.selenium.WebDriver driver, String snapshotName, Map<String, Object> options, String testType) throws Exception {
        if (testType != null && !testType.trim().isEmpty()) {
            SmartUISnapshot.smartuiSnapshot(driver, snapshotName, options, testType);
        } else {
            SmartUISnapshot.smartuiSnapshot(driver, snapshotName, options);
        }
    }

    private void processWebViewports(org.openqa.selenium.WebDriver driver, String snapshotName, Map<String, Object> options, String testType, java.util.List<?> viewportList) throws Exception {
        if (!configMap.containsKey("web")) {
            return;
        }

        Map<String, Object> webConfig = (Map<String, Object>) configMap.get("web");
        if (!webConfig.containsKey("viewports")) {
            return;
        }

        java.util.List<?> configViewportsRaw = (java.util.List<?>) webConfig.get("viewports");
        java.util.List<String> configViewports = extractWholeNumbers(configViewportsRaw);

        for (Object viewport : viewportList) {
            String viewportStr = viewport.toString();
            if (configViewports.contains(viewportStr)) {
                setDeviceMetricsAndRefresh(driver, Integer.parseInt(viewportStr), 1080, 1, false);

                Map<String, Object> optionsCopy = new HashMap<>(options);
                Map<String, Object> webViewportConfig = new HashMap<>();
                webViewportConfig.put("viewports", java.util.Arrays.asList(viewport));
                optionsCopy.put("web", webViewportConfig);

                captureSnapshot(driver, snapshotName, optionsCopy, testType);
            }
        }
    }

    private void processMobileDevices(org.openqa.selenium.WebDriver driver, String snapshotName, Map<String, Object> options, String testType, java.util.List<?> viewportList) throws Exception {
        if (!configMap.containsKey("mobile")) {
            return;
        }

        Map<String, Object> mobileConfig = (Map<String, Object>) configMap.get("mobile");
        if (!mobileConfig.containsKey("devices")) {
            return;
        }

        java.util.List<?> configDevices = (java.util.List<?>) mobileConfig.get("devices");

        for (Object viewport : viewportList) {
            if (configDevices.contains(viewport)) {
                DeviceSpecs.DeviceSpec deviceSpec = DeviceSpecs.getDeviceSpec(viewport.toString());
                if (deviceSpec != null) {
                    setDeviceMetricsAndRefresh(driver, deviceSpec.getWidth(), deviceSpec.getHeight(), 3, true);

                    Map<String, Object> optionsCopy = new HashMap<>(options);
                    Map<String, Object> mobileDeviceConfig = new HashMap<>();
                    mobileDeviceConfig.put("devices", java.util.Arrays.asList(viewport));

                    if (mobileConfig.containsKey("fullPage")) {
                        mobileDeviceConfig.put("fullPage", mobileConfig.get("fullPage"));
                    }
                    if (mobileConfig.containsKey("orientation")) {
                        mobileDeviceConfig.put("orientation", mobileConfig.get("orientation"));
                    }

                    optionsCopy.put("mobile", mobileDeviceConfig);
                    captureSnapshot(driver, snapshotName, optionsCopy, testType);
                } else {
                    log.warning("Device spec not found for: " + viewport);
                }
            }
        }
    }

    private java.util.List<String> extractWholeNumbers(java.util.List<?> rawList) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (Object vp : rawList) {
            if (vp instanceof java.util.List) {
                java.util.List<?> vpList = (java.util.List<?>) vp;
                if (!vpList.isEmpty()) {
                    String vpStr = vpList.get(0).toString().split("\\.")[0];
                    result.add(vpStr);
                }
            } else {
                String vpStr = vp.toString().split("\\.")[0];
                result.add(vpStr);
            }
        }
        return result;
    }

    private void setDeviceMetricsAndRefresh(org.openqa.selenium.WebDriver driver, int width, int height, int deviceScaleFactor, boolean mobile) {
        try {
            if (driver instanceof HasCdp) {
                Map<String, Object> cdpParams = new HashMap<>();
                cdpParams.put("width", width);
                cdpParams.put("height", height);
                cdpParams.put("deviceScaleFactor", deviceScaleFactor);
                cdpParams.put("mobile", mobile);

                ((HasCdp) driver).executeCdpCommand("Emulation.setDeviceMetricsOverride", cdpParams);
                driver.navigate().refresh();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            log.warning("Failed to set device metrics: " + e.getMessage());
        }
    }

    private void clearDeviceMetrics(org.openqa.selenium.WebDriver driver) {
        try {
            if (driver instanceof HasCdp) {
                ((HasCdp) driver).executeCdpCommand("Emulation.clearDeviceMetricsOverride", new HashMap<>());
                driver.navigate().refresh();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            log.warning("Failed to clear device metrics: " + e.getMessage());
        }
    }
}
