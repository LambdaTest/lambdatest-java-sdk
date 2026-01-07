package io.github.lambdatest;

import io.github.lambdatest.models.BuildScreenshotsResponse;
import io.github.lambdatest.models.UploadPDFResponse;
import io.github.lambdatest.models.FormattedResults;
import io.github.lambdatest.models.PdfResult;
import io.github.lambdatest.models.PdfPage;
import io.github.lambdatest.models.Screenshot;
import io.github.lambdatest.utils.LoggerUtil;
import io.github.lambdatest.utils.SmartUIUtil;

import javax.xml.transform.Result;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SmartUIPdf {
    private final SmartUIUtil smartUIUtils;
    public final String buildName;
    public final boolean fetchResults;
    public final String projectToken;
    public final String[] pdfNames;
    private static final Logger log = LoggerUtil.createLogger("lambdatest-java-sdk");

    public SmartUIPdf(SmartUIConfig config) {
        this.smartUIUtils = new SmartUIUtil();
        this.buildName = config.getBuildName();
        this.fetchResults = config.getFetchResults();
        this.pdfNames = config.getPdfNames();

        if (config.getProjectToken() == null || config.getProjectToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Project token is required");
        }

        this.projectToken = config.getProjectToken();
    }

    public FormattedResults uploadPDF(String path) throws Exception {
        List<File> pdfFiles = new ArrayList<>();
        
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path provided is invalid");
        }
        
        File file = new File(path);
        
        if (!file.exists()) {
            throw new IllegalArgumentException("Path does not exist: " + path);
        }
        
        if (file.isFile() && isPdfFile(file)) {
            pdfFiles.add(file);
            log.info("Found PDF file: " + file.getAbsolutePath());
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && isPdfFile(f)) {
                        pdfFiles.add(f);
                    }
                }
            }
            // Sort files alphabetically by filename to ensure consistent ordering
            Collections.sort(pdfFiles, Comparator.comparing(File::getName));
            log.info("Found " + pdfFiles.size() + " PDF files in directory: " + file.getAbsolutePath());
        }

        try {
            UploadPDFResponse response = smartUIUtils.postPDFToSmartUI(pdfFiles, this.projectToken, this.buildName, this.pdfNames);

            if (this.fetchResults) {
                BuildScreenshotsResponse screenshotsResponse = smartUIUtils.getBuildScreenshots(response.getProjectId(), response.getBuildId(), this.projectToken);
                return analyzeScreenshots(screenshotsResponse);
            }

            FormattedResults results = new FormattedResults();
            results.setStatus("success");
            results.setData(new FormattedResults.ResultData(response.getBuildId(), response.getProjectName()));

            return results;

        } catch (Exception e) {
            throw new Exception("Failure while uploading PDFs" + e);
        }
    }
    
    private boolean isPdfFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".pdf");
    }

    private FormattedResults analyzeScreenshots(BuildScreenshotsResponse response) {
        if (response.getScreenshots() == null || response.getScreenshots().isEmpty()) {
            log.warning("No screenshots found in response");
            return null;
        }

        Map<String, List<Screenshot>> pdfGroups = groupScreenshotsByPdf(response.getScreenshots());
        int pdfsWithMismatches = countPdfsWithMismatches(pdfGroups);
        int pagesWithMismatches = countPagesWithMismatches(response.getScreenshots());

        log.info("PDF Upload Results:");
        log.info("Build Name: " + response.getBuild().getName());
        log.info("Project Name: " + response.getProject().getName());
        log.info("Total PDFs: " + pdfGroups.size());
        log.info("Total Pages: " + response.getScreenshots().size());

        if (pdfsWithMismatches > 0 || pagesWithMismatches > 0) {
            log.warning(pdfsWithMismatches + " PDFs and " + pagesWithMismatches + " Pages in build " + response.getBuild().getName() + " have changes present.");
        } else {
            log.info("All PDFs match the baseline.");
        }

        for (Map.Entry<String, List<Screenshot>> entry : pdfGroups.entrySet()) {
            String pdfName = entry.getKey();
            List<Screenshot> pages = entry.getValue();
            boolean hasMismatch = pages.stream().anyMatch(page -> page.getMismatchPercentage() > 0);
            
            if (hasMismatch) {
                log.warning("📄 " + pdfName + " (" + pages.size() + " pages)");
            } else {
                log.info("📄 " + pdfName + " (" + pages.size() + " pages)");
            }

            for (Screenshot page : pages) {
                if (page.getMismatchPercentage() > 0) {
                    log.warning("  - Page " + getPageNumber(page.getScreenshotName()) + ": " + page.getStatus() + " (Mismatch: " + page.getMismatchPercentage() + "%)");
                } else {
                    log.info("  - Page " + getPageNumber(page.getScreenshotName()) + ": " + page.getStatus() + " (Mismatch: " + page.getMismatchPercentage() + "%)");
                }
            }
        }

        List<PdfResult> formattedPdfs = formatPdfsForOutput(pdfGroups);
        
        FormattedResults.ResultData data = new FormattedResults.ResultData(
            response.getBuild().getBuildId(),
            response.getBuild().getName(),
            response.getProject().getName(),
            response.getBuild().getBuildStatus(),
            formattedPdfs
        );

        return new FormattedResults("success", data);
    }

    private Map<String, List<Screenshot>> groupScreenshotsByPdf(List<Screenshot> screenshots) {
        Map<String, List<Screenshot>> pdfGroups = new HashMap<>();

        for (Screenshot screenshot : screenshots) {
            String pdfName = screenshot.getScreenshotName().split("#")[0];
            pdfGroups.computeIfAbsent(pdfName, k -> new ArrayList<>()).add(screenshot);
        }

        return pdfGroups;
    }

    private int countPdfsWithMismatches(Map<String, List<Screenshot>> pdfGroups) {
        int count = 0;

        for (List<Screenshot> pages : pdfGroups.values()) {
            if (pages.stream().anyMatch(page -> page.getMismatchPercentage() > 0)) {
                count++;
            }
        }

        return count;
    }

    private int countPagesWithMismatches(List<Screenshot> screenshots) {
        return (int) screenshots.stream().filter(screenshot -> screenshot.getMismatchPercentage() > 0).count();
    }

    private List<PdfResult> formatPdfsForOutput(Map<String, List<Screenshot>> pdfGroups) {
        List<PdfResult> results = new ArrayList<>();

        for (Map.Entry<String, List<Screenshot>> entry : pdfGroups.entrySet()) {
            String pdfName = entry.getKey();
            List<Screenshot> pages = entry.getValue();
            
            List<PdfPage> formattedPages = new ArrayList<>();
            for (Screenshot page : pages) {
                PdfPage pdfPage = new PdfPage(
                    Integer.parseInt(getPageNumber(page.getScreenshotName())),
                    page.getCapturedImageId(),
                    page.getMismatchPercentage(),
                    page.getStatus(),
                    page.getShareableLink()
                );
                formattedPages.add(pdfPage);
            }
            
            results.add(new PdfResult(pdfName, pages.size(), formattedPages));
        }

        return results;
    }

    private String getPageNumber(String screenshotName) {
        String[] parts = screenshotName.split("#");
        return parts.length > 1 ? parts[1] : "1";
    }
}
