package io.github.lambdatest.models;

public class ErrAPIResponse {

    private String errorMessage;
    private int errorCode;

    // Default constructor
    public ErrAPIResponse() {}

    // Constructor with error message and error code
    public ErrAPIResponse(String errorMessage, int errorCode) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    // Getter for errorMessage
    public String getErrorMessage() {
        return errorMessage;
    }

    // Setter for errorMessage
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Getter for errorCode
    public int getErrorCode() {
        return errorCode;
    }

    // Setter for errorCode
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}

