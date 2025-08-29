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
    
    private boolean allowInsecure = false;
    private String proxyHost;
    private int proxyPort;
    private String proxyProtocol = "http";
    
    private String projectToken;
    private String buildName;
    private boolean createBuild = true;
    
    private String configFile;
    private String environment;
    private String branch;
    private String commitId;
    
    private String logLevel = "info";
    private boolean verbose = false;
    
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
    
    public SmartUIConfig withProxy(String host, int port) {
        this.proxyHost = host;
        this.proxyPort = port;
        return this;
    }
    
    public SmartUIConfig withProxy(String protocol, String host, int port, boolean allowInsecure) {
        this.proxyProtocol = protocol;
        this.proxyHost = host;
        this.proxyPort = port;
        this.allowInsecure = allowInsecure;
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
    
    public SmartUIConfig withCreateBuild(boolean createBuild) {
        this.createBuild = createBuild;
        return this;
    }
    
    public SmartUIConfig withConfigFile(String configFile) {
        this.configFile = configFile;
        return this;
    }
    
    public SmartUIConfig withEnvironment(String environment) {
        this.environment = environment;
        return this;
    }
    
    public SmartUIConfig withBranch(String branch) {
        this.branch = branch;
        return this;
    }
    
    public SmartUIConfig withCommitId(String commitId) {
        this.commitId = commitId;
        return this;
    }
    
    public SmartUIConfig withLogLevel(String logLevel) {
        this.logLevel = logLevel;
        return this;
    }
    
    public SmartUIConfig withVerbose(boolean verbose) {
        this.verbose = verbose;
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
    
    public String getProxyHost() {
        return proxyHost;
    }
    
    public int getProxyPort() {
        return proxyPort;
    }
    
    public String getProxyProtocol() {
        return proxyProtocol;
    }
    
    public boolean isAllowInsecure() {
        return allowInsecure;
    }
    
    public String getProjectToken() {
        return projectToken;
    }
    
    public String getBuildName() {
        return buildName;
    }

    public boolean isCreateBuild() {
        return createBuild;
    }
    
    public String getConfigFile() {
        return configFile;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public String getBranch() {
        return branch;
    }
    
    public String getCommitId() {
        return commitId;
    }
    
    public String getLogLevel() {
        return logLevel;
    }
    
    public boolean isVerbose() {
        return verbose;
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
        
        if (proxyHost != null && (proxyPort <= 0 || proxyPort > 65535)) {
            errors.add("Proxy port must be between 1 and 65535");
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
        copy.allowInsecure = this.allowInsecure;
        copy.proxyHost = this.proxyHost;
        copy.proxyPort = this.proxyPort;
        copy.proxyProtocol = this.proxyProtocol;
        copy.projectToken = this.projectToken;
        copy.buildName = this.buildName;
        copy.createBuild = this.createBuild;
        copy.configFile = this.configFile;
        copy.environment = this.environment;
        copy.branch = this.branch;
        copy.commitId = this.commitId;
        copy.logLevel = this.logLevel;
        copy.verbose = this.verbose;
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
        if (other.allowInsecure != false) merged.allowInsecure = other.allowInsecure;
        if (other.proxyHost != null) merged.proxyHost = other.proxyHost;
        if (other.proxyPort > 0) merged.proxyPort = other.proxyPort;
        if (other.proxyProtocol != null) merged.proxyProtocol = other.proxyProtocol;
        if (other.projectToken != null) merged.projectToken = other.projectToken;
        if (other.buildName != null) merged.buildName = other.buildName;
        if (other.createBuild != true) merged.createBuild = other.createBuild;
        if (other.configFile != null) merged.configFile = other.configFile;
        if (other.environment != null) merged.environment = other.environment;
        if (other.branch != null) merged.branch = other.branch;
        if (other.commitId != null) merged.commitId = other.commitId;
        if (other.logLevel != null) merged.logLevel = other.logLevel;
        if (other.verbose != false) merged.verbose = other.verbose;
        
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
        map.put("allowInsecure", allowInsecure);
        map.put("proxyHost", proxyHost);
        map.put("proxyPort", proxyPort);
        map.put("proxyProtocol", proxyProtocol);
        map.put("projectToken", projectToken != null ? "***" + projectToken.substring(Math.max(0, projectToken.length() - 4)) : null);
        map.put("buildName", buildName);
        map.put("createBuild", createBuild);
        map.put("configFile", configFile);
        map.put("environment", environment);
        map.put("branch", branch);
        map.put("commitId", commitId);
        map.put("logLevel", logLevel);
        map.put("verbose", verbose);
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
        
        String envCreateBuild = System.getenv("SMARTUI_CREATE_BUILD");
        if (envCreateBuild != null) {
            config.createBuild = Boolean.parseBoolean(envCreateBuild);
        }
        
        String envConfigFile = System.getenv("SMARTUI_CONFIG_FILE");
        if (envConfigFile != null) {
            config.configFile = envConfigFile;
        }
        
        String envEnvironment = System.getenv("SMARTUI_ENVIRONMENT");
        if (envEnvironment != null) {
            config.environment = envEnvironment;
        }
        
        String envBranch = System.getenv("SMARTUI_BRANCH");
        if (envBranch != null) {
            config.branch = envBranch;
        }
        
        String envCommitId = System.getenv("SMARTUI_COMMIT_ID");
        if (envCommitId != null) {
            config.commitId = envCommitId;
        }
        
        String envLogLevel = System.getenv("SMARTUI_LOG_LEVEL");
        if (envLogLevel != null) {
            config.logLevel = envLogLevel;
        }
        
        String envVerbose = System.getenv("SMARTUI_VERBOSE");
        if (envVerbose != null) {
            config.verbose = Boolean.parseBoolean(envVerbose);
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
                ", allowInsecure=" + allowInsecure +
                ", proxyHost='" + proxyHost + '\'' +
                ", proxyPort=" + proxyPort +
                ", proxyProtocol='" + proxyProtocol + '\'' +
                ", projectToken='" + (projectToken != null ? "***" + projectToken.substring(Math.max(0, projectToken.length() - 4)) : null) + '\'' +
                ", buildName='" + buildName + '\'' +
                ", createBuild=" + createBuild +
                ", configFile='" + configFile + '\'' +
                ", environment='" + environment + '\'' +
                ", branch='" + branch + '\'' +
                ", commitId='" + commitId + '\'' +
                ", logLevel='" + logLevel + '\'' +
                ", verbose=" + verbose +
                '}';
    }
}
