package io.github.lambdatest.models;

import com.google.gson.annotations.SerializedName;

public class BuildResponse {
    @SerializedName("data")
    private BuildData data;

    public BuildData getData() {
        return data;
    }

    public void setData(BuildData data) {
        this.data = data;
    }
}
