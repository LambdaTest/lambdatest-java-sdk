package io.github.lambdatest.models;

public class PdfThreshold {
    // boxed so an unset side stays absent from the request and falls back server-side; 0 is a real value
    private Double approval;
    private Double rejection;

    public PdfThreshold() {}

    // Number so callers can pass int, long, float or double literals alike
    public PdfThreshold(Number approval, Number rejection) {
        this.approval = toDouble(approval);
        this.rejection = toDouble(rejection);
    }

    public static PdfThreshold approval(Number approval) {
        return new PdfThreshold(approval, null);
    }

    public static PdfThreshold rejection(Number rejection) {
        return new PdfThreshold(null, rejection);
    }

    public Double getApproval() {
        return approval;
    }

    public void setApproval(Number approval) {
        this.approval = toDouble(approval);
    }

    public Double getRejection() {
        return rejection;
    }

    public void setRejection(Number rejection) {
        this.rejection = toDouble(rejection);
    }

    static Double toDouble(Number value) {
        return value == null ? null : value.doubleValue();
    }
}
