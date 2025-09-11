package io.github.lambdatest.models;

import java.util.List;

public class FormattedResults {
    private String status;
    private ResultData data;

    public FormattedResults() {}

    public FormattedResults(String status, ResultData data) {
        this.status = status;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ResultData getData() {
        return data;
    }

    public void setData(ResultData data) {
        this.data = data;
    }

    public static class ResultData {
        private String buildId;
        private String buildName;
        private String projectName;
        private String buildStatus;
        private String buildUrl;
        private List<PdfResult> pdfs;

        public ResultData(String buildId, String buildName, String projectName, String buildStatus, List<PdfResult> pdfs) {
            this.buildId = buildId;
            this.buildName = buildName;
            this.projectName = projectName;
            this.buildStatus = buildStatus;
            this.pdfs = pdfs;
        }

        public ResultData(String buildId, String projectName) {
            this.buildId = buildId;
            this.projectName = projectName;
        }

        public String getBuildId() {
            return buildId;
        }

        public void setBuildId(String buildId) {
            this.buildId = buildId;
        }

        public String getBuildName() {
            return buildName;
        }

        public String getBuildUrl() {
            return buildUrl;
        }

        public void setBuildUrl(String buildUrl) {
            this.buildUrl = buildUrl;
        }

        public void setBuildName(String buildName) {
            this.buildName = buildName;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getBuildStatus() {
            return buildStatus;
        }

        public void setBuildStatus(String buildStatus) {
            this.buildStatus = buildStatus;
        }

        public List<PdfResult> getPdfs() {
            return pdfs;
        }

        public void setPdfs(List<PdfResult> pdfs) {
            this.pdfs = pdfs;
        }
    }
}
