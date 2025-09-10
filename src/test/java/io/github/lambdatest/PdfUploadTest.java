package io.github.lambdatest;

import io.github.lambdatest.models.FormattedResults;

/**
 * Test to verify PDF upload functionality with fetchResults
 */
public class PdfUploadTest {
    
    public static void main(String[] args) {
        System.out.println("=== PDF Upload Test ===\n");
        
        try {
            // Test configuration with fetchResults enabled
            SmartUIConfig config = new SmartUIConfig()
                .withProjectToken("621263#01K4MEW3ESFKRSXDT8J2J2E08Y#pdf-cli")
                .withBuildName("PDF-Test-Build-" + System.currentTimeMillis())
                .withFetchResult(false);
            
            SmartUIPdf pdfUploader = new SmartUIPdf(config);
            
            // Test with a sample PDF path (replace with actual PDF path for testing)
            String pdfPath = "src/test/java/io/github/lambdatest/pdfs"; // Upload all PDFs in directory
            
            System.out.println("1. Uploading PDF: " + pdfPath);
            System.out.println("2. FetchResults enabled: " + config.getFetchResults());
            
            FormattedResults results = pdfUploader.uploadPDF(pdfPath);
            
            if (results != null) {
                System.out.println("\n3. PDF Upload and Analysis Results:");
                System.out.println("   Status: " + results.getStatus());
                System.out.println("   Build ID: " + results.getData().getBuildId());
                System.out.println("   Build Name: " + results.getData().getBuildName());
                System.out.println("   Project Name: " + results.getData().getProjectName());
                System.out.println("   Build Status: " + results.getData().getBuildStatus());
                System.out.println("   Total PDFs: " + results.getData().getPdfs().size());
                
                // Display detailed PDF information
                for (io.github.lambdatest.models.PdfResult pdfResult : results.getData().getPdfs()) {
                    System.out.println("\n   PDF: " + pdfResult.getPdfName());
                    System.out.println("   Pages: " + pdfResult.getPageCount());
                    
                    for (io.github.lambdatest.models.PdfPage page : pdfResult.getPages()) {
                        System.out.println("     Page " + page.getPageNumber() + 
                                         ": " + page.getStatus() + 
                                         " (Mismatch: " + page.getMismatchPercentage() + "%)");
                    }
                }
                
                System.out.println("\n✓ PDF upload and analysis completed successfully!");
            } else {
                System.out.println("\n⚠ No results returned (PDF may not exist or fetchResults disabled)");
            }
            
        } catch (IllegalArgumentException e) {
            System.out.println("✗ Invalid argument: " + e.getMessage());
            System.out.println("   Please ensure the PDF path exists and is valid");
        } catch (Exception e) {
            System.out.println("✗ Error during PDF upload: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
