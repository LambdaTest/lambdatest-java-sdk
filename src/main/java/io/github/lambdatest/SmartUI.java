package io.github.lambdatest;

import io.github.lambdatest.exceptions.SmartUIException;
import io.github.lambdatest.models.BuildData;
import io.github.lambdatest.utils.LoggerUtil;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Main SmartUI class for server management and snapshot functionality
 */
public class SmartUI {
    private final SmartUIConfig config;
    private Process serverProcess;
    private boolean isServerRunning = false;
    private BuildData buildData;
    
    private static final Logger log = LoggerUtil.createLogger("SmartUI");
    private static final String SMARTUI_CLI_COMMAND = "smartui";
    private static final String SMARTUI_CLI_INSTALL_COMMAND = "npm install -g @lambdatest/smartui-cli";
    
    public SmartUI(SmartUIConfig config) {
        this.config = config;
        if (config.getProjectToken() == null || config.getProjectToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Project token is required");
        }
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
                processBuilder.command("cmd", "/c", "npm", "install", "-g", "@lambdatest/smartui-cli");
            } else {
                processBuilder.command("npm", "install", "-g", "@lambdatest/smartui-cli");
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
    
    public void startServer() throws SmartUIException {
        if (isServerRunning) {
            log.info("Server is already running");
            return;
        }
        
        if (config.isAutoInstall() && !isSmartUICLIInstalled()) {
            installSmartUICLI();
        }
        
        if (!isSmartUICLIInstalled()) {
            throw new SmartUIException("SmartUI CLI is not installed and auto-installation is disabled");
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
    
    private void                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                startServerWithRetry() throws SmartUIException {
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
            
            // Add buildName if specified
            if (config.getBuildName() != null && !config.getBuildName().trim().isEmpty()) {
                command.add("--buildName");
                command.add(config.getBuildName());
            }
            
            Map<String, String> env = processBuilder.environment();
            env.put("PROJECT_TOKEN", config.getProjectToken());
            env.put("SMARTUI_SERVER_ADDRESS", "http://localhost:" + config.getPort());
            
            processBuilder.command(command);
            
            Thread.sleep(2000);
            
            // Verify that the server actually started
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
    
    public void stopServer() throws SmartUIException {
        if (!isServerRunning) {
            log.info("Server is not running");
            return;
        }
        
        try {
            log.info("Stopping SmartUI server...");

            // Use CLI to stop the server on the specific port
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
    
    public boolean pingServer() throws SmartUIException {
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
    
    public boolean isServerRunning() {
        return isServerRunning;
    }
    
    public BuildData getBuildData() {
        return buildData;
    }
    
    public String getBuildId() {
        return buildData != null ? buildData.getBuildId() : null;
    }
    
    public String getBuildName() {
        return buildData != null ? buildData.getName() : null;
    }
    
    public void takeSnapshot(org.openqa.selenium.WebDriver driver, String snapshotName) throws SmartUIException {
        takeSnapshot(driver, snapshotName, new HashMap<>());
    }
    
    public void takeSnapshot(org.openqa.selenium.WebDriver driver, String snapshotName, Map<String, Object> options) throws SmartUIException {
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
            
//            if (buildData != null && buildData.getBuildId() != null) {
//                options.put("buildId", buildData.getBuildId());
//                options.put("buildName", buildData.getName());
//            }
            
            options.put("serverAddress", config.getServerAddress());
            
            SmartUISnapshot.smartuiSnapshot(driver, snapshotName, options);
            
            log.info("Snapshot captured successfully: " + snapshotName);
            
        } catch (Exception e) {
            String errorMsg = "Failed to take snapshot '" + snapshotName + "': " + e.getMessage();
            log.severe(errorMsg);
            throw new SmartUIException(errorMsg, e);
        }
    }
    
    public String getServerAddress() {
        return config.getServerAddress();
    }

    private void forceCleanup() {
        log.warning("Performing forced cleanup...");
        try {
            if (serverProcess != null && serverProcess.isAlive()) {
                serverProcess.destroyForcibly();
                serverProcess = null;
            }
            isServerRunning = false;
            buildData = null;
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
            
            // Set environment variables
            Map<String, String> env = stopProcessBuilder.environment();
            env.put("PROJECT_TOKEN", config.getProjectToken());
            env.put("SMARTUI_SERVER_ADDRESS", "http://localhost:" + port);
            
            stopProcessBuilder.directory(new java.io.File(System.getProperty("user.dir")));
            
            stopProcessBuilder.command(stopCommand);
            Process stopProcess = stopProcessBuilder.start();
            
            if (stopProcess.waitFor(15, TimeUnit.SECONDS)) {
                int exitCode = stopProcess.exitValue();
                if (exitCode == 0) {
                    // Give the server a moment to shut down
                    Thread.sleep(2000);
                    
                    // Verify server is actually stopped
                    return !pingServer();
                } else {
                    log.warning("CLI stop command failed with exit code: " + exitCode);
                    return false;
                }
            } else {
                log.warning("CLI stop command timed out");
                stopProcess.destroyForcibly();
                return false;
            }
            
        } catch (Exception e) {
            log.warning("Failed to execute CLI stop command: " + e.getMessage());
            return false;
        }
    }
}

