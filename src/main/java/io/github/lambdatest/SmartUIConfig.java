package io.github.lambdatest;

/**
 * Configuration class for SmartUI operations
 * Includes essential SmartUI CLI configuration options
 */
public class SmartUIConfig {
    private int port = 49152;
    private String host = "localhost";
    private boolean autoInstall = true;
    private String serverAddress = "http://localhost:49152";
    
    private String projectToken;
    private String buildName;
    private String configFile;
    
    public SmartUIConfig withPort(int port) {
        this.port = port;
        this.serverAddress = "http://" + this.host + ":" + port;
        return this;
    }

    public SmartUIConfig withConfigFile(String file) {
        this.configFile = file;
        return this;
    }

    public SmartUIConfig withHost(String host) {
        this.host = host;
        this.serverAddress = "http://" + host + ":" + this.port;
        return this;
    }
    
    public SmartUIConfig withAutoInstall(boolean autoInstall) {
        this.autoInstall = autoInstall;
        return this;
    }
    
    public SmartUIConfig withServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
        return this;
    }
    
    public SmartUIConfig withProjectToken(String projectToken) {
        this.projectToken = projectToken;
        return this;
    }
    
    public SmartUIConfig withBuildName(String buildName) {
        this.buildName = buildName;
        return this;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getHost() {
        return host;
    }
    
    public boolean isAutoInstall() {
        return autoInstall;
    }
    
    public String getServerAddress() {
        return serverAddress;
    }
    
    public String getProjectToken() {
        return projectToken;
    }
    
    public String getBuildName() {
        return buildName;
    }

    public String getConfigFile() {
        return configFile;
    }
    
    /**
     * Validate the configuration
     * @return true if configuration is valid, false otherwise
     */
    public boolean isValid() {
        return projectToken != null && !projectToken.trim().isEmpty() &&
               port > 0 && port <= 65535;
    }
    
    /**
     * Get validation errors if any
     * @return List of validation error messages
     */
    public java.util.List<String> getValidationErrors() {
        java.util.List<String> errors = new java.util.ArrayList<>();
        
        if (projectToken == null || projectToken.trim().isEmpty()) {
            errors.add("Project token is required");
        }
        
        if (port <= 0 || port > 65535) {
            errors.add("Port must be between 1 and 65535");
        }
        
        return errors;
    }
    
    /**
     * Create configuration from environment variables
     * @return A new SmartUIConfig instance populated from environment variables
     */
    public static SmartUIConfig fromEnvironment() {
        SmartUIConfig config = new SmartUIConfig();
        
        String envPort = System.getenv("SMARTUI_PORT");
        if (envPort != null) {
            try {
                config.port = Integer.parseInt(envPort);
                config.serverAddress = "http://localhost:" + config.port;
            } catch (NumberFormatException e) {
                // Use default port
            }
        }

        String serverAddress = System.getenv("SMARTUI_SERVER_ADDRESS");
        if (serverAddress != null) {
            config.serverAddress = serverAddress;
        }
        
        String envAutoInstall = System.getenv("SMARTUI_AUTO_INSTALL");
        if (envAutoInstall != null) {
            config.autoInstall = Boolean.parseBoolean(envAutoInstall);
        }
        
        String envProjectToken = System.getenv("PROJECT_TOKEN");
        if (envProjectToken != null) {
            config.projectToken = envProjectToken;
        }
        
        String envBuildName = System.getenv("SMARTUI_BUILD_NAME");
        if (envBuildName != null) {
            config.buildName = envBuildName;
        }
        
        config.serverAddress = "http://" + config.host + ":" + config.port;
        
        return config;
    }
    
    @Override
    public String toString() {
        return "SmartUIConfig{" +
                "port=" + port +
                ", autoInstall=" + autoInstall +
                ", serverAddress='" + serverAddress + '\'' +
                ", projectToken='" + (projectToken != null ? "***" + projectToken.substring(Math.max(0, projectToken.length() - 4)) : null) + '\'' +
                ", buildName='" + buildName + '\'' +
                '}';
    }
}
