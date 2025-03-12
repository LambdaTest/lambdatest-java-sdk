package io.github.lambdatest.utils;


import com.google.gson.*;
import io.github.lambdatest.models.BuildData;
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


public class HttpClientUtil {
    private final CloseableHttpClient httpClient;
    private Logger log;

    public HttpClientUtil() {
        this.httpClient = HttpClients.createDefault();
        this.log = LoggerUtil.createLogger("lambdatest-java-sdk");
    }

    public String request(String url, String method, String data) throws IOException {
        if (Constants.RequestMethods.POST.equalsIgnoreCase(method)) {
            return post(url, data);
        } else if (Constants.RequestMethods.GET.equalsIgnoreCase(method)) {
            return get(url);
        }
        else {
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

    private String delete(String url,Map<String, String> headers) throws IOException {

        HttpDelete request = new HttpDelete(url);
        if (headers != null && headers.containsKey("projectToken")) {
            String projectToken = headers.get("projectToken").trim();
            if (!projectToken.isEmpty()) {
                request.setHeader("projectToken", projectToken);
            }
        }
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            checkResponseStatus(response);
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
        }
        catch (Exception e){
        log.warning("Exception occured in delete "+ e.getMessage());}
        return "Success";
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

        if (Objects.nonNull(headers) && headers.containsKey("projectToken")) {
            request.setHeader("projectToken", headers.get("projectToken").trim());
        }

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String responseString = entity != null ? EntityUtils.toString(entity) : null;

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                return responseString;
            } else {
                // Try to extract error message
                try {
                    if(Objects.nonNull(responseString)){
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

    public boolean isUserAuthenticated(String projectToken) throws IOException {
        HttpGet request = new HttpGet(Constants.SmartUIRoutes.STAGE_URL + Constants.SmartUIRoutes.SMARTUI_AUTH_ROUTE
        );
        request.setHeader("projectToken", projectToken);

        try(CloseableHttpResponse response = httpClient.execute(request)){
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode == 200){
                log.info("Authenticated used for projectToken :"+ projectToken);
                return true;
            }
            else{
                log.warning("Error in authenticating user ...");
                throw new IllegalArgumentException(Constants.Errors.USER_AUTH_ERROR);
            }
        }
        catch (Exception e){
            log.severe("Exception in authenticating projectToken due to : "+ e.getMessage());
            return false;
        }
    }

    public String isSmartUIRunning() throws IOException {
        return request(SmartUIUtil.getSmartUIServerAddress() + Constants.SmartUIRoutes.SMARTUI_HEALTHCHECK_ROUTE, Constants.RequestMethods.GET, null);
    }

    public String fetchDOMSerializer() throws IOException {
        return request(SmartUIUtil.getSmartUIServerAddress() + Constants.SmartUIRoutes.SMARTUI_DOMSERIALIZER_ROUTE, Constants.RequestMethods.GET, null);
    }

    public String postSnapshot(String data) throws IOException {
        return request(SmartUIUtil.getSmartUIServerAddress() + Constants.SmartUIRoutes.SMARTUI_SNAPSHOT_ROUTE, Constants.RequestMethods.POST, data);
    }


    public String createSmartUIBuild(String createBuildRequest, Map<String,String> headers) throws IOException {
        return postWithHeader(Constants.SmartUIRoutes.STAGE_URL + Constants.SmartUIRoutes.SMARTUI_CREATE_BUILD,  createBuildRequest, headers);
    }

    public void stopBuild(String buildId, Map<String, String> headers) throws IOException {

        if (headers != null && headers.containsKey("projectToken")) {
            String projectToken = headers.get("projectToken").trim();
            if (!projectToken.isEmpty()) {
                headers.put("projectToken", projectToken);
            }
        }
        String response = delete(Constants.SmartUIRoutes.STAGE_URL + Constants.SmartUIRoutes.SMARTUI_FINALISE_BUILD_ROUTE +buildId, headers);
        log.info("Response of stop Build: "+ response + "for buildId" + buildId);
    }

    public String uploadScreenshot(String url, File screenshot , UploadSnapshotRequest uploadScreenshotRequest, BuildData data) throws IOException {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost uploadRequest = new HttpPost(url);
            uploadRequest.setHeader("projectToken", uploadScreenshotRequest.getProjectToken());

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.STRICT);

            builder.addBinaryBody("screenshot", screenshot, ContentType.IMAGE_PNG, screenshot.getName());
            builder.addTextBody("buildId", uploadScreenshotRequest.getBuildId());
            builder.addTextBody("buildName", uploadScreenshotRequest.getBuildName());
            builder.addTextBody("screenshotName", uploadScreenshotRequest.getScreenshotName());
            builder.addTextBody("browser", uploadScreenshotRequest.getBrowserName());
            builder.addTextBody("deviceName", uploadScreenshotRequest.getDeviceName());
            builder.addTextBody("os", uploadScreenshotRequest.getOs());
//            builder.addTextBody("viewport", uploadScreenshotRequest.getViewport());
            if(data.getBaseline()){
                builder.addTextBody("baseline", "true");
            }
            else {
                builder.addTextBody("baseline", "false");
            }

            HttpEntity multipart = builder.build();
            uploadRequest.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(uploadRequest)) {
                System.out.println("Response Code: " + response.getStatusLine().getStatusCode());
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("Response Body: " + responseBody);
                return responseBody;
            }
        }
         catch (IOException e) {
            e.printStackTrace();
        }
        return "Success";
    }
}