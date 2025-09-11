package io.github.lambdatest.models;

import java.util.List;

public class PdfResult {
    private String pdfName;
    private int pageCount;
    private List<PdfPage> pages;

    public PdfResult() {}

    public PdfResult(String pdfName, int pageCount, List<PdfPage> pages) {
        this.pdfName = pdfName;
        this.pageCount = pageCount;
        this.pages = pages;
    }

    public String getPdfName() {
        return pdfName;
    }

    public void setPdfName(String pdfName) {
        this.pdfName = pdfName;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public List<PdfPage> getPages() {
        return pages;
    }

    public void setPages(List<PdfPage> pages) {
        this.pages = pages;
    }
}
