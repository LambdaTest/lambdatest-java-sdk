package io.github.lambdatest;

/**
 * Configuration class for SmartUI operations
 * Includes essential SmartUI CLI configuration options
 */
public class SmartUIConfig {
    private int port = 49152;
    private String host = "localhost";
    private int timeout = 30000;
    private boolean autoInstall = true;
    private String serverAddress = "http://localhost:49152";
    
    private String projectToken;
    private String buildName;
    
    public SmartUIConfig withPort(int port) {
        this.port = port;
        this.serverAddress = "http://" + this.host + ":" + port;
        return this;
    }
    
    public SmartUIConfig withHost(String host) {
        this.host = host;
        this.serverAddress = "http://" + host + ":" + this.port;
        return this;
    }
    
    public SmartUIConfig withTimeout(int timeout) {
        this.timeout = timeout;
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
    
    public int getTimeout() {
        return timeout;
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
    
    /**
     * Validate the configuration
     * @return true if configuration is valid, false otherwise
     */
    public boolean isValid() {
        return projectToken != null && !projectToken.trim().isEmpty() &&
               port > 0 && port <= 65535 &&
               timeout > 0;
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
        
        if (timeout <= 0) {
            errors.add("Timeout must be greater than 0");
        }
        
        return errors;
    }
    
    /**
     * Create a copy of this configuration
     * @return A new SmartUIConfig instance with the same values
     */
    public SmartUIConfig copy() {
        SmartUIConfig copy = new SmartUIConfig();
        copy.port = this.port;
        copy.host = this.host;
        copy.timeout = this.timeout;
        copy.autoInstall = this.autoInstall;
        copy.serverAddress = this.serverAddress;
        copy.projectToken = this.projectToken;
        copy.buildName = this.buildName;
        return copy;
    }
    
    /**
     * Merge this configuration with another, with the other taking precedence
     * @param other The configuration to merge with
     * @return A new merged configuration
     */
    public SmartUIConfig merge(SmartUIConfig other) {
        if (other == null) {
            return this.copy();
        }
        
        SmartUIConfig merged = this.copy();
        
        if (other.port != 49152) merged.port = other.port;
        if (other.timeout != 30000) merged.timeout = other.timeout;
        if (other.autoInstall != true) merged.autoInstall = other.autoInstall;
        if (other.serverAddress != null && !other.serverAddress.equals("http://localhost:49152")) {
            merged.serverAddress = other.serverAddress;
        }
        if (other.projectToken != null) merged.projectToken = other.projectToken;
        if (other.buildName != null) merged.buildName = other.buildName;
        
        return merged;
    }
    
    /**
     * Get configuration as a map for easy serialization
     * @return Map containing all configuration values
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("port", port);
        map.put("timeout", timeout);
        map.put("autoInstall", autoInstall);
        map.put("serverAddress", serverAddress);
        map.put("projectToken", projectToken != null ? "***" + projectToken.substring(Math.max(0, projectToken.length() - 4)) : null);
        map.put("buildName", buildName);
        return map;
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
        
        String envTimeout = System.getenv("SMARTUI_TIMEOUT");
        if (envTimeout != null) {
            try {
                config.timeout = Integer.parseInt(envTimeout);
            } catch (NumberFormatException e) {
                // Use default timeout
            }
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
                ", timeout=" + timeout +
                ", autoInstall=" + autoInstall +
                ", serverAddress='" + serverAddress + '\'' +
                ", projectToken='" + (projectToken != null ? "***" + projectToken.substring(Math.max(0, projectToken.length() - 4)) : null) + '\'' +
                ", buildName='" + buildName + '\'' +
                '}';
    }
}
