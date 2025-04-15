package io.github.lambdatest.utils;

import com.google.gson.*;
import io.github.lambdatest.models.BuildData;
import io.github.lambdatest.models.ProjectTokenResponse;
import io.github.lambdatest.models.UploadSnapshotRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import io.github.lambdatest.constants.Constants;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import javax.net.ssl.SSLContext;

import static io.github.lambdatest.constants.Constants.TEST_TYPE;

public class HttpClientUtil {
    private final CloseableHttpClient httpClient;
    private Logger log = LoggerUtil.createLogger("lambdatest-java-sdk");

    public HttpClientUtil() {
        this.httpClient = HttpClients.createDefault();
    }

    public HttpClientUtil(String proxyHost, int proxyPort) throws Exception {
        this(proxyHost, proxyPort, false);
    }

    public HttpClientUtil(String proxyHost, int proxyPort, boolean allowInsecure) throws Exception {
        try {
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setProxy(proxy)
                    .build();
            // Build the HttpClient with conditional SSL settings
            CloseableHttpClient clientBuilder;

            // If allowInsecure is true, disable SSL verification
            if (allowInsecure) {
                SSLContext sslContext = new SSLContextBuilder()
                        .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                        .build();

                clientBuilder = HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build();
            } else {
                // Build standard HttpClient with proxy
                clientBuilder = HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .build();
            }

            this.httpClient = clientBuilder;
            String proxyConfig = String.format("%s:%d (Insecure: %b)", proxyHost, proxyPort, allowInsecure);
            log.info(proxyConfig);

        } catch (Exception e) {
            log.severe("Error configuring HttpClient" + e.getMessage());
            throw new Exception("Failed to create HttpClient" + e.getMessage());
        }
    }

    public HttpClientUtil(String proxyProtocol, String proxyHost, int proxyPort, boolean allowInsecure)
            throws Exception {
        try {
            HttpHost proxy = new HttpHost(proxyHost, proxyPort, proxyProtocol);

            RequestConfig requestConfig = RequestConfig.custom()
                    .setProxy(proxy)
                    .build();

            // Build the HttpClient with conditional SSL settings
            CloseableHttpClient clientBuilder;

            // If allowInsecure is true, disable SSL verification
            if (allowInsecure) {
                SSLContext sslContext = new SSLContextBuilder()
                        .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                        .build();

                clientBuilder = HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build();

            } else {
                // Build standard HttpClient with proxy
                clientBuilder = HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .build();
            }

            // Assign the built HttpClient
            this.httpClient = clientBuilder;

            String proxyConfig = String.format("%s://%s:%d (Insecure: %b)",
                    proxyProtocol, proxyHost, proxyPort, allowInsecure);
            log.info(proxyConfig);
        } catch (Exception e) {
            log.severe("Error configuring HttpClient" + e.getMessage());
            throw new Exception("Failed to create HttpClient" + e.getMessage());
        }
    }

    public String request(String url, String method, String data) throws IOException {
        if (Constants.RequestMethods.POST.equalsIgnoreCase(method)) {
            return post(url, data);
        } else if (Constants.RequestMethods.GET.equalsIgnoreCase(method)) {
            return get(url);
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }

    private String get(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            checkResponseStatus(response);
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
        }
    }

    private String delete(String url, Map<String, String> headers) throws IOException {

        HttpDelete request = new HttpDelete(url);
        if (headers != null && headers.containsKey(Constants.PROJECT_TOKEN)) {
            String projectToken = headers.get(Constants.PROJECT_TOKEN).trim();
            if (!projectToken.isEmpty()) {
                request.setHeader(Constants.PROJECT_TOKEN, projectToken);
            }
        }
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            checkResponseStatus(response);
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
        } catch (Exception e) {
            log.warning("Exception occurred in Delete" + e);
            throw e;
        }
    }

    private String post(String url, String data) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(data, StandardCharsets.UTF_8));
        request.setHeader("Content-type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String responseString = entity != null ? EntityUtils.toString(entity) : null;

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                // Request was successful
                return responseString;
            } else {
                // Request failed, attempt to parse error message from response
                try {
                    JsonElement element = new JsonParser().parse(responseString);
                    if (element.isJsonObject()) {
                        JsonObject jsonResponse = element.getAsJsonObject();
                        if (jsonResponse.has("error") && jsonResponse.get("error").isJsonObject()) {
                            JsonObject errorObject = jsonResponse.getAsJsonObject("error");
                            if (errorObject.has("message")) {
                                String errorMessage = errorObject.get("message").getAsString();
                                log.severe(String.format(Constants.Errors.POST_SNAPSHOT_FAILED, errorMessage));
                            }
                        }
                    }
                } catch (JsonSyntaxException e) {
                    throw new IOException("Failed to parse error response: " + responseString, e);
                }
                throw new IOException("Unexpected status code: " + statusCode);
            }
        }
    }

    private String postWithHeader(String url, String data, Map<String, String> headers) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(data, StandardCharsets.UTF_8));
        request.setHeader("Content-Type", "application/json");

        if (Objects.nonNull(headers) && headers.containsKey(Constants.PROJECT_TOKEN)) {
            request.setHeader(Constants.PROJECT_TOKEN, headers.get(Constants.PROJECT_TOKEN).trim());
        }

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String responseString = entity != null ? EntityUtils.toString(entity) : null;
            log.info(" postWithHeader responseString : " + responseString);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                return responseString;
            } else {
                // Try to extract error message
                try {
                    if (Objects.nonNull(responseString)) {
                        JsonElement element;
                        element = JsonParser.parseString(responseString);
                        if (element.isJsonObject()) {
                            JsonObject jsonResponse = element.getAsJsonObject();
                            if (jsonResponse.has("error") && jsonResponse.get("error").isJsonObject()) {
                                JsonObject errorObject = jsonResponse.getAsJsonObject("error");
                                if (errorObject.has("message")) {
                                    String errorMessage = errorObject.get("message").getAsString();
                                    log.severe(String.format("Error in POST request " + errorMessage));
                                }
                            }
                        }
                    }
                } catch (JsonSyntaxException e) {
                    throw new IOException("Failed to parse error response: " + responseString, e);
                }
                throw new IOException("Unexpected status code: " + statusCode);
            }
        }
    }

    private void checkResponseStatus(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new IOException("Request failed with status code: " + statusCode + " and response: " + response);
        }
    }

    public boolean isUserAuthenticated(String projectToken) throws Exception {
        try {
            String hostUrl = Constants.getHostUrlFromEnvOrDefault();
            String url = hostUrl + Constants.SmartUIRoutes.SMARTUI_AUTH_ROUTE;
            HttpGet request = new HttpGet(url);
            request.setHeader(Constants.PROJECT_TOKEN, projectToken);
            log.info("Authenticating user for projectToken :" + projectToken);
            log.info("URL : " + url);
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            String responseString = entity != null ? EntityUtils.toString(entity) : null;
            log.info("responseString : " + responseString);
            log.info("response.getStatusLine() : " + response.getStatusLine());

            ProjectTokenResponse projectResponse = parseResponse(responseString);
            if (projectResponse.isSuccessful()) {
                return true;
            } else {
                log.info("User authentication failed for projectToken response : " + responseString);
                return false;
            }
        } catch (Exception e) {
            log.severe("Exception in authenticating projectToken due to : " + e.getMessage());
            throw e;
        }
    }

    public String isSmartUIRunning() throws IOException {
        return request(SmartUIUtil.getSmartUIServerAddress() + Constants.SmartUIRoutes.SMARTUI_HEALTHCHECK_ROUTE,
                Constants.RequestMethods.GET, null);
    }

    public String fetchDOMSerializer() throws IOException {
        return request(SmartUIUtil.getSmartUIServerAddress() + Constants.SmartUIRoutes.SMARTUI_DOMSERIALIZER_ROUTE,
                Constants.RequestMethods.GET, null);
    }

    public String postSnapshot(String data) throws IOException {
        return request(SmartUIUtil.getSmartUIServerAddress() + Constants.SmartUIRoutes.SMARTUI_SNAPSHOT_ROUTE,
                Constants.RequestMethods.POST, data);
    }

    public String createSmartUIBuild(String createBuildRequest, Map<String, String> headers) throws IOException {
        String hostUrl = Constants.getHostUrlFromEnvOrDefault();
        return postWithHeader(hostUrl + Constants.SmartUIRoutes.SMARTUI_CREATE_BUILD,
                createBuildRequest, headers);
    }

    public void stopBuild(String buildId, Map<String, String> headers) throws IOException {

        if (headers != null && headers.containsKey(Constants.PROJECT_TOKEN)) {
            String projectToken = headers.get(Constants.PROJECT_TOKEN).trim();
            if (!projectToken.isEmpty()) {
                headers.put(Constants.PROJECT_TOKEN, projectToken);
            }
        }
        String hostUrl = Constants.getHostUrlFromEnvOrDefault();
        String response = delete(
                hostUrl + Constants.SmartUIRoutes.SMARTUI_FINALISE_BUILD_ROUTE + buildId + "&testType="+ TEST_TYPE,
                headers);
    }

    public String uploadScreenshot(String url, File screenshot, UploadSnapshotRequest request,
                                   BuildData data) throws IOException {
        HttpPost uploadRequest = new HttpPost(url);
        uploadRequest.setHeader("projectToken", request.getProjectToken());

        // Build the multipart request entity
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.STRICT);

        // Add the required fields
        builder.addBinaryBody("screenshot", screenshot, ContentType.create("image/png"), request.getScreenshotName());
        builder.addTextBody("buildId", data.getBuildId());
        builder.addTextBody("buildName", data.getName());
        builder.addTextBody("baseline", Boolean.toString(data.getBaseline()));
        builder.addTextBody("screenshotName", request.getScreenshotName());
        builder.addTextBody("browser", request.getBrowserName());
        builder.addTextBody("deviceName", request.getDeviceName());
        builder.addTextBody("os", request.getOs());
        builder.addTextBody("viewport", request.getViewport());
        builder.addTextBody("uploadChunk", request.getUploadChunk());
        builder.addTextBody("projectType", TEST_TYPE);
        builder.addTextBody("screenshotHash", request.getScreenshotHash());

        // Add optional fields if present
        if (Objects.nonNull(request.getFullPage())) {
            builder.addTextBody("fullPage", request.getFullPage());
        }
        if (Objects.nonNull(request.getIsLastChunk())) {
            builder.addTextBody("isLastChunk", request.getIsLastChunk());
        }
        if (Objects.nonNull(request.getChunkCount())) {
            builder.addTextBody("chunkCount", String.valueOf(request.getChunkCount()));
        }

        // Handle status bar height
        String statusBarHeight = "";
        if (request.getStatusBarHeight() == null) {
            builder.addTextBody("statusBarHeight", statusBarHeight);
        } else {
            statusBarHeight = request.getStatusBarHeight();
            //only set cropStatusBar to false when it exists and statusBarHeight is valid
            if (request.getCropStatusBar() != null && Boolean.parseBoolean(request.getCropStatusBar())
                    && isValidNumber(statusBarHeight)) {
                request.setCropStatusBar("false"); // Overwrite since we have custom value from user
            }
            builder.addTextBody("statusBarHeight", statusBarHeight);
        }

        if (request.getCropStatusBar() != null) {
            builder.addTextBody("cropStatusBar", request.getCropStatusBar());
        }

        // Handle navigation bar height
        String navigationBarHeight = "";
        if (request.getNavigationBarHeight() == null) {
            builder.addTextBody("navigationBarHeight", navigationBarHeight);
        } else {
            navigationBarHeight = request.getNavigationBarHeight();
            //only set cropFooter to false when it exists and navigationBarHeight is valid
            if (request.getCropFooter() != null && Boolean.parseBoolean(request.getCropFooter())
                    && isValidNumber(navigationBarHeight)) {
                request.setCropFooter("false"); // Overwrite since we have custom value from user
            }
            builder.addTextBody("navigationBarHeight", navigationBarHeight);
        }

        if (request.getCropFooter() != null) {
            builder.addTextBody("cropFooter", request.getCropFooter());
        }

        // Execute the request
        HttpEntity multipart = builder.build();
        uploadRequest.setEntity(multipart);
        try (CloseableHttpResponse response = httpClient.execute(uploadRequest)) {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {

            log.warning("Exception occurred in uploading screenshot: " + e.getMessage());
            throw new IOException("Failed to upload screenshot", e);
        }
    }
    // Helper method to validate if a string is a valid number or not
    private boolean isValidNumber(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            int strVal = Integer.parseInt(value);
            if(strVal >=1) {
            return true;
            } else {
                throw new NumberFormatException("Invalid value for cropping, pls provide a valid value");
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public ProjectTokenResponse parseResponse(String responseString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(responseString, ProjectTokenResponse.class);
        } catch (Exception e) {
            log.severe("Error parsing response: " + e.getMessage());
            return new ProjectTokenResponse();
        }
    }
}