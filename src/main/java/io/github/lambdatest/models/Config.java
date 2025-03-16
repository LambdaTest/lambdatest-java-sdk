package io.github.lambdatest.models;

public class Config {
    private MobileConfig mobile;
    private Integer waitForPageRender;
    private Integer waitForTimeout;
    private Boolean enableJavaScript;
    private Integer scrollTime;

    // Default Constructor
    public Config() {
    }

    // All-Arguments Constructor
    public Config(MobileConfig mobile, Integer waitForPageRender, Integer waitForTimeout, Boolean enableJavaScript, Integer scrollTime) {
        this.mobile = mobile;
        this.waitForPageRender = waitForPageRender;
        this.waitForTimeout = waitForTimeout;
        this.enableJavaScript = enableJavaScript;
        this.scrollTime = scrollTime;
    }

    // Getter and Setter for mobileConfig
    public MobileConfig getMobile() {
        return mobile;
    }

    public void setMobile(MobileConfig mobile) {
        this.mobile = mobile;
    }

    // Getter and Setter for waitForPageRender
    public Integer getWaitForPageRender() {
        return waitForPageRender;
    }

    public void setWaitForPageRender(Integer waitForPageRender) {
        this.waitForPageRender = waitForPageRender;
    }

    // Getter and Setter for waitForTimeout
    public Integer getWaitForTimeout() {
        return waitForTimeout;
    }

    public void setWaitForTimeout(Integer waitForTimeout) {
        this.waitForTimeout = waitForTimeout;
    }

    // Getter and Setter for enableJavaScript
    public Boolean getEnableJavaScript() {
        return enableJavaScript;
    }

    public void setEnableJavaScript(Boolean enableJavaScript) {
        this.enableJavaScript = enableJavaScript;
    }

    // Getter and Setter for scrollTime
    public Integer getScrollTime() {
        return scrollTime;
    }

    public void setScrollTime(Integer scrollTime) {
        this.scrollTime = scrollTime;
    }
}


