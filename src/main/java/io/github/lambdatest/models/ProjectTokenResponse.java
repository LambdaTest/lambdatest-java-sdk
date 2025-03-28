
package io.github.lambdatest.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProjectTokenResponse {
    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("projectToken")
    private String projectToken;

    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getProjectToken() { return projectToken; }
    public void setProjectToken(String projectToken) { this.projectToken = projectToken; }

    // Helper method to check if verification was successful
    public boolean isSuccessful() {
        return "Success".equalsIgnoreCase(status);
    }
}