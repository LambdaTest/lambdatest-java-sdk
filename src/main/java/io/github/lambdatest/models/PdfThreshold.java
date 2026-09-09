package io.github.lambdatest.models;

public class PdfThreshold {
    // boxed so an unset side stays absent from the request and falls back server-side; 0 is a real value
    private Double approval;
    private Double rejection;

    public PdfThreshold() {}

    public PdfThreshold(Double approval, Double rejection) {
        this.approval = approval;
        this.rejection = rejection;
    }

    public static PdfThreshold approval(double approval) {
        return new PdfThreshold(approval, null);
    }

    public static PdfThreshold rejection(double rejection) {
        return new PdfThreshold(null, rejection);
    }

    public Double getApproval() {
        return approval;
    }

    public void setApproval(Double approval) {
        this.approval = approval;
    }

    public Double getRejection() {
        return rejection;
    }

    public void setRejection(Double rejection) {
        this.rejection = rejection;
    }
}
