package io.github.lambdatest.models;


public class BuildData {
    private String buildId;
    private String buildUrl;
    private Boolean baseline;
    private String name;

    //Getter Setter for attributes
    public String getBuildId() {return buildId;}
    public String getBuildUrl() {return buildUrl;}
    public Boolean getBaseline() {return baseline;}
    public String getName() {return name;}

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }
    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }
    public void setBaseline(Boolean baseline) {
        this.baseline = baseline;
    }
    public void setName(String buildName) {this.name = buildName;}

}
