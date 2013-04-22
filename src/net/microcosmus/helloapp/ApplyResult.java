package net.microcosmus.helloapp;

public class ApplyResult {

    public static enum Status {
        OK, ERRORS
    }

    private Long appliedId;

    private Status status;

    public Long getAppliedId() {
        return appliedId;
    }

    public void setAppliedId(Long appliedId) {
        this.appliedId = appliedId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
