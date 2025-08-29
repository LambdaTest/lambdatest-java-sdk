package io.github.lambdatest;

import io.github.lambdatest.exceptions.SmartUIException;
import io.github.lambdatest.models.BuildData;
import io.github.lambdatest.utils.LoggerUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
            // Reset the running state if start failed
            isServerRunning = false;
            throw new SmartUIException("Failed to start SmartUI server: " + e.getMessage(), e);
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
                    // Try to stop any existing server on the port before retrying
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
            
            // Set environment variables for the process
            Map<String, String> env = processBuilder.environment();
            env.put("PROJECT_TOKEN", config.getProjectToken());
            env.put("SMARTUI_SERVER_ADDRESS", "http://localhost:" + config.getPort());
            
            processBuilder.command(command);
            
            // Start the CLI command in background - don't wait for it to complete
            // The CLI will start the server in background and exit
            Process process = processBuilder.start();
            
            // Give the CLI a moment to start the server, then check if it's running
            Thread.sleep(2000);
            
            // Verify that the server actually started
            if (!pingServer()) {
                log.warning("CLI start command executed, but server is not responding yet");
            } else {
                log.info("CLI start command executed successfully, server is responding");
            }
            
            // Don't wait for the CLI process to complete since it starts server in background
            // Don't store the process reference since CLI manages it
            // serverProcess = null;
            
        } catch (Exception e) {
            throw new SmartUIException("Failed to start server process: " + e.getMessage(), e);
        }
    }
    
    /**
     * Stop the server process we started (legacy method, now uses CLI)
     * @return true if process was stopped successfully, false otherwise
     */
    private boolean stopServerProcess() {
        // Since we now start via CLI, use CLI to stop
        return stopServerViaCLI(config.getPort());
    }
    
    private void waitForServerReady() throws SmartUIException {
        long startTime = System.currentTimeMillis();
        long timeout = config.getTimeout();
        
        while (System.currentTimeMillis() - startTime < timeout) {
            if (pingServer()) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SmartUIException("Interrupted while waiting for server", e);
            }
        }
        
        throw new SmartUIException("Server did not become ready within " + timeout + "ms");
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
                // Fallback to stopping server on configured port
                stopServerOnConfiguredPort();
                
                if (isServerActuallyStopped()) {
                    isServerRunning = false;
                    log.info("SmartUI server stopped successfully");
                } else {
                    log.warning("Server may not have stopped completely");
                    isServerRunning = false;
                }
            }
            
        } catch (Exception e) {
            log.severe("Error during server shutdown: " + e.getMessage());
            forceCleanup();
            throw new SmartUIException("Failed to stop SmartUI server: " + e.getMessage(), e);
        }
    }
    
    private boolean isServerActuallyStopped() {
        try {
            Thread.sleep(2000);
            
            return !pingServer();
        } catch (Exception e) {
            log.warning("Could not verify server shutdown status: " + e.getMessage());
            return true;
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
            
            if (buildData != null && buildData.getBuildId() != null) {
                options.put("buildId", buildData.getBuildId());
                options.put("buildName", buildData.getName());
            }
            
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
     * Stop any SmartUI server on a specific port (external or internal)
     * This method attempts to stop servers regardless of who started them
     * 
     * @param port The port to check and stop
     * @return true if server was stopped, false if no server found
     */
    public boolean stopServerOnPort(int port) {
        try {
            log.info("Attempting to stop SmartUI server on port " + port);

            // Try to ping the server to confirm it's a SmartUI server
            if (!pingServerOnPort(port)) {
                log.info("No SmartUI server responding on port " + port);
                return false;
            }
            
            // Try CLI stop command first (most graceful)
            if (stopServerViaCLI(port)) {
                log.info("Successfully stopped external server on port " + port + " via CLI");
                return true;
            }
            
            // Fallback to process termination if CLI fails
            if (stopServerViaProcessTermination(port)) {
                log.info("Successfully stopped external server on port " + port + " via process termination");
                return true;
            }
            
            log.warning("Failed to stop server on port " + port + " using all methods");
            return false;
            
        } catch (Exception e) {
            log.severe("Error stopping external server on port " + port + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a port is in use by any process
     * @param port The port to check
     * @return true if port is in use, false otherwise
     */
    private boolean isPortInUse(int port) {
        try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(port)) {
            serverSocket.close();
            return false; // Port is available
        } catch (Exception e) {
            return true; // Port is in use
        }
    }
    
    /**
     * Ping a SmartUI server on a specific port
     * @param port The port to ping
     * @return true if server responds, false otherwise
     */
    private boolean pingServerOnPort(int port) {
        try {
            String serverUrl = "http://localhost:" + port + "/healthcheck";
            URL url = new URL(serverUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "SmartUI-Java-SDK/1.0");
            
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
            
        } catch (Exception e) {
            log.fine("Could not ping server on port " + port + ": " + e.getMessage());
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
            
            // Set working directory to current directory
            stopProcessBuilder.directory(new java.io.File(System.getProperty("user.dir")));
            
            stopProcessBuilder.command(stopCommand);
            Process stopProcess = stopProcessBuilder.start();
            
            // Wait for CLI command to complete
            if (stopProcess.waitFor(15, TimeUnit.SECONDS)) {
                int exitCode = stopProcess.exitValue();
                if (exitCode == 0) {
                    // Give the server a moment to shut down
                    Thread.sleep(2000);
                    
                    // Verify server is actually stopped
                    return !pingServerOnPort(port);
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
    
    /**
     * Stop server by terminating the process using the port
     * @param port The port where server is running
     * @return true if process was terminated, false otherwise
     */
    private boolean stopServerViaProcessTermination(int port) {
        try {
            // Find process using the port
            ProcessBuilder findProcessBuilder = new ProcessBuilder();
            java.util.List<String> findCommand = new java.util.ArrayList<>();
            
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                findCommand.add("cmd");
                findCommand.add("/c");
                findCommand.add("netstat");
                findCommand.add("-ano");
                findCommand.add("|");
                findCommand.add("findstr");
                findCommand.add(":" + port);
            } else {
                findCommand.add("lsof");
                findCommand.add("-ti");
                findCommand.add(":" + port);
            }
            
            findProcessBuilder.command(findCommand);
            Process findProcess = findProcessBuilder.start();
            
            if (findProcess.waitFor(10, TimeUnit.SECONDS)) {
                // Read the output to get process ID
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(findProcess.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && !line.trim().isEmpty()) {
                        String processId = extractProcessId(line, port);
                        if (processId != null) {
                            return terminateProcess(processId);
                        }
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.warning("Failed to find process using port " + port + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract process ID from command output
     * @param output The command output line
     * @param port The port number
     * @return Process ID as string, or null if not found
     */
    private String extractProcessId(String output, int port) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // Windows netstat output format: TCP    127.0.0.1:49152    0.0.0.0:0    LISTENING    12345
                String[] parts = output.trim().split("\\s+");
                if (parts.length >= 5) {
                    return parts[4];
                }
            } else {
                // Unix lsof output format: 12345
                return output.trim();
            }
        } catch (Exception e) {
            log.fine("Failed to extract process ID from output: " + output);
        }
        return null;
    }
    
    /**
     * Terminate a process by ID
     * @param port The port number
     * @return true if process was terminated, false otherwise
     */
    private boolean terminateProcess(String processId) {
        try {
            ProcessBuilder killProcessBuilder = new ProcessBuilder();
            java.util.List<String> killCommand = new java.util.ArrayList<>();
            
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                killProcessBuilder.command("cmd", "/c", "taskkill", "/PID", processId, "/F");
            } else {
                killProcessBuilder.command("kill", "-9", processId);
            }
            
            Process killProcess = killProcessBuilder.start();
            
            boolean completed = killProcess.waitFor(10, TimeUnit.SECONDS);
            if (completed) {
                int exitCode = killProcess.exitValue();
                if (exitCode == 0) {
                    log.info("Successfully terminated process " + processId);
                    return true;
                } else {
                    log.warning("Failed to terminate process " + processId + " with exit code: " + exitCode);
                    return false;
                }
            } else {
                log.warning("Process termination command timed out for PID " + processId);
                killProcess.destroyForcibly();
                return false;
            }
            
        } catch (Exception e) {
            log.warning("Failed to terminate process " + processId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Stop any SmartUI server on the configured port
     * @return true if server was stopped, false if no server found
     */
    public boolean stopServerOnConfiguredPort() {
        return stopServerOnPort(config.getPort());
    }
    
    /**
     * Gracefully shutdown a process by sending SIGTERM first, then SIGKILL if needed
     * @param processId The process ID to shutdown
     * @return true if process was stopped gracefully, false otherwise
     */
    private boolean gracefulShutdown(String processId) {
        try {
            log.info("Sending SIGTERM to process " + processId);
            
            ProcessBuilder killProcessBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                killProcessBuilder.command("cmd", "/c", "taskkill", "/PID", processId);
            } else {
                killProcessBuilder.command("kill", processId);
            }
            
            Process killProcess = killProcessBuilder.start();
            
            // Wait for graceful shutdown
            if (killProcess.waitFor(10, TimeUnit.SECONDS)) {
                int exitCode = killProcess.exitValue();
                if (exitCode == 0) {
                    log.info("Process " + processId + " stopped gracefully with SIGTERM");
                    return true;
                } else {
                    log.warning("SIGTERM failed for process " + processId + " with exit code: " + exitCode);
                }
            } else {
                log.warning("SIGTERM timed out for process " + processId);
            }
            
            // If graceful shutdown failed, try SIGKILL
            log.info("Attempting SIGKILL for process " + processId);
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                killProcessBuilder.command("cmd", "/c", "taskkill", "/PID", processId, "/F");
            } else {
                killProcessBuilder.command("kill", "-9", processId);
            }
            
            Process forceKillProcess = killProcessBuilder.start();
            if (forceKillProcess.waitFor(5, TimeUnit.SECONDS)) {
                int exitCode = forceKillProcess.exitValue();
                if (exitCode == 0) {
                    log.info("Process " + processId + " stopped with SIGKILL");
                    return true;
                } else {
                    log.warning("SIGKILL failed for process " + processId + " with exit code: " + exitCode);
                }
            } else {
                log.warning("SIGKILL timed out for process " + processId);
            }
            
            return false;
            
        } catch (Exception e) {
            log.warning("Failed to gracefully shutdown process " + processId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find the process ID of the server running on a specific port
     * @param port The port to check
     * @return Process ID as string, or null if not found
     */
    private String findProcessIdOnPort(int port) {
        try {
            ProcessBuilder findProcessBuilder = new ProcessBuilder();
            java.util.List<String> findCommand = new java.util.ArrayList<>();
            
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                findCommand.add("cmd");
                findCommand.add("/c");
                findCommand.add("netstat");
                findCommand.add("-ano");
                findCommand.add("|");
                findCommand.add("findstr");
                findCommand.add(":" + port);
            } else {
                findCommand.add("lsof");
                findCommand.add("-ti");
                findCommand.add(":" + port);
            }
            
            findProcessBuilder.command(findCommand);
            Process findProcess = findProcessBuilder.start();
            
            if (findProcess.waitFor(5, TimeUnit.SECONDS)) {
                // Read the output to get process ID
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(findProcess.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && !line.trim().isEmpty()) {
                        return extractProcessId(line, port);
                    }
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.warning("Failed to find process ID on port " + port + ": " + e.getMessage());
            return null;
        }
    }
}

