package io.github.lambdatest.constants;

import static io.github.lambdatest.constants.Constants.SmartUIRoutes.SMARTUI_CLIENT_API_URL;

public interface Constants {

  String SMARTUI_SERVER_ADDRESS = "SMARTUI_SERVER_ADDRESS";
  public static final String PROJECT_TOKEN = "projectToken";
  public final String TEST_TYPE = "lambdatest-java-app-sdk";
  String LOCAL_SERVER_HOST = "http://localhost:8080";

  public static String getHostUrlFromEnvOrDefault() {
    String envUrl = System.getenv("SMARTUI_CLIENT_API_URL");
    return (envUrl != null && !envUrl.isEmpty()) ? envUrl : SMARTUI_CLIENT_API_URL;
  }

  //SmartUI API routes
  interface SmartUIRoutes {
    public static final String SMARTUI_CLIENT_API_URL = "https://api.lambdatest.com/visualui/1.0";
    public static final String SMARTUI_HEALTHCHECK_ROUTE = "/healthcheck";
    public static final String SMARTUI_DOMSERIALIZER_ROUTE = "/domserializer";
    public static final String SMARTUI_SNAPSHOT_ROUTE = "/snapshot";
    public static final String SMARTUI_AUTH_ROUTE = "/token/verify";
    public static final String SMARTUI_CREATE_BUILD = "/build";
    public static final String SMARTUI_FINALISE_BUILD_ROUTE = "/build?buildId=";
    public static final String SMARTUI_UPLOAD_SCREENSHOT_ROUTE = "/screenshot";
    
  }

  //Request methods
  interface RequestMethods {
    public static final String POST = "POST";
    public static final String GET = "GET";
    public static final String DELETE = "DELETE";
  }

  //Logger colors
  interface LoggerColors {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RED = "\u001B[31m";
  }

  //Logger env vars
  interface LogEnvVars {
    public static final String LT_SDK_DEBUG = "LT_SDK_DEBUG";
    public static final String LT_SDK_LOG_LEVEL = "LT_SDK_LOG_LEVEL";
  }
  
  //SmartUI environment variables
  interface SmartUIEnvVars {
    public static final String SMARTUI_SERVER_ADDRESS = "SMARTUI_SERVER_ADDRESS";
    public static final String SMARTUI_PORT = "SMARTUI_PORT";
    public static final String SMARTUI_HOST = "SMARTUI_HOST";
    public static final String SMARTUI_TIMEOUT = "SMARTUI_TIMEOUT";
    public static final String SMARTUI_AUTO_INSTALL = "SMARTUI_AUTO_INSTALL";
    public static final String SMARTUI_BUILD_ID = "SMARTUI_BUILD_ID";
    public static final String SMARTUI_BUILD_NAME = "SMARTUI_BUILD_NAME";
    public static final String SMARTUI_CONFIG_FILE = "SMARTUI_CONFIG_FILE";
    public static final String SMARTUI_ENVIRONMENT = "SMARTUI_ENVIRONMENT";
    public static final String SMARTUI_BRANCH = "SMARTUI_BRANCH";
    public static final String SMARTUI_COMMIT_ID = "SMARTUI_COMMIT_ID";
    public static final String SMARTUI_LOG_LEVEL = "SMARTUI_LOG_LEVEL";
    public static final String SMARTUI_VERBOSE = "SMARTUI_VERBOSE";
    public static final String SMARTUI_ALLOW_INSECURE = "SMARTUI_ALLOW_INSECURE";
    public static final String HTTP_PROXY = "HTTP_PROXY";
    public static final String HTTPS_PROXY = "HTTPS_PROXY";
    public static final String NODE_TLS_REJECT_UNAUTHORIZED = "NODE_TLS_REJECT_UNAUTHORIZED";
  }

  //Error constants
  interface Errors {
    public static final String SELENIUM_DRIVER_NULL = "An instance of the selenium driver object is required.";
    public static final String SNAPSHOT_NAME_NULL = "The `snapshotName` argument is required.";
    public static final String SNAPSHOT_NOT_FOUND = "Screenshot not found.";
    public static final String SMARTUI_NOT_RUNNING = "SmartUI server is not running.";
    public static final String JAVA_SCRIPT_NOT_SUPPORTED = "The driver does not support JavaScript execution.";
    public static final String EMPTY_RESPONSE_DOMSERIALIZER = "Response from fetchDOMSerializer is null or empty.";
    public static final String EMPTY_DATA_FIELD = "Response JSON does not contain 'data' field.";
    public static final String NULL_DATA_OBJECT = "Data object is null or missing 'dom' field.";
    public static final String NULL_DOM_STRING = "'dom' string is null or empty.";
    public static final String NULL_RESULT_MAP = "Result map is null or missing 'dom' key.";
    public static final String MISSING_HTML_KEY = "DOM map is null or missing 'html' key.";
    public static final String FETCH_DOM_FAILED = "fetch DOMSerializer failed";
    public static final String POST_SNAPSHOT_FAILED = "Post snapshot failed: %s";
    public static final String UPLOAD_SNAPSHOT_FAILED = "Upload snapshot failed: ";
    public static final String INVALID_RESPONSE_DATA = "Invalid response from fetchDOMSerializer";
    public static final String SMARTUI_SNAPSHOT_FAILED = "SmartUI snapshot failed";
    public static  final String PROJECT_TOKEN_UNSET = "projectToken cant be empty";
    public static final String USER_AUTH_ERROR = "User authentication failed";
    public static final String STOP_BUILD_FAILED = "Failed to stop build";
    public static final String PAGE_COUNT_ERROR = "Page Count Value is invalid";
    public static final String NULL_OPTIONS_OBJECT = "Options object is null or missing in request.";
    public static final String DEVICE_NAME_NULL = "Device name is a mandatory parameter.";
    public static final String INVALID_PORT_NUMBER = "Invalid port number. Must be between 1 and 65535.";
    public static final String INVALID_TIMEOUT_VALUE = "Invalid timeout value. Must be greater than 0.";
    public static final String INVALID_PROXY_CONFIGURATION = "Invalid proxy configuration.";
    public static final String ENVIRONMENT_VARIABLE_SET_FAILED = "Failed to set environment variable: %s";
    public static final String CONFIGURATION_VALIDATION_FAILED = "Configuration validation failed: %s";
    public static final String PORT_NOT_AVAILABLE = "Port %d is not available.";
    public static final String NO_AVAILABLE_PORTS = "No available ports found after %d attempts.";
  }
}