package io.github.lambdatest.models;

public class PdfPage {
    private int pageNumber;
    private String screenshotId;
    private double mismatchPercentage;
    private String status;
    private String screenshotUrl;

    public PdfPage() {}

    public PdfPage(int pageNumber, String screenshotId, double mismatchPercentage, String status, String screenshotUrl) {
        this.pageNumber = pageNumber;
        this.screenshotId = screenshotId;
        this.mismatchPercentage = mismatchPercentage;
        this.status = status;
        this.screenshotUrl = screenshotUrl;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getScreenshotId() {
        return screenshotId;
    }

    public void setScreenshotId(String screenshotId) {
        this.screenshotId = screenshotId;
    }

    public double getMismatchPercentage() {
        return mismatchPercentage;
    }

    public void setMismatchPercentage(double mismatchPercentage) {
        this.mismatchPercentage = mismatchPercentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getScreenshotUrl() {
        return screenshotUrl;
    }

    public void setScreenshotUrl(String screenshotUrl) {
        this.screenshotUrl = screenshotUrl;
    }
}
