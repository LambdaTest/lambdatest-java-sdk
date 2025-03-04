package io.github.lambdatest.models;

import java.util.List;

public class MobileConfig {

    private List<String> devices;
    private Boolean fullPage;
    private String orientation;

    // Default constructor
    public MobileConfig() {
    }

    // All-arguments constructor
    public MobileConfig(List<String> devices, Boolean fullPage, String orientation) {
        this.devices = devices;
        this.fullPage = fullPage;
        this.orientation = orientation;
    }

    public List<String> getDevices() {
        return devices;
    }

    public void setDevices(List<String> devices) {
        this.devices = devices;
    }

    public Boolean getFullPage() {
        return fullPage;
    }

    public void setFullPage(Boolean fullPage) {
        this.fullPage = fullPage;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }
}

