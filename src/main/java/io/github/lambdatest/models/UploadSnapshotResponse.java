package io.github.lambdatest.models;


public class UploadSnapshotResponse {

    private UploadSnapshotResponseData data;
    private ErrAPIResponse error;

    // Default constructor
    public UploadSnapshotResponse() {}

    // All-args constructor
    public UploadSnapshotResponse(UploadSnapshotResponseData data, ErrAPIResponse error) {
        this.data = data;
        this.error = error;
    }

    public UploadSnapshotResponseData getData() {
        return data;
    }

    public void setData(UploadSnapshotResponseData data) {
        this.data = data;
    }

    public ErrAPIResponse getError() {
        return error;
    }

    public void setError(ErrAPIResponse error) {
        this.error = error;
    }
}
