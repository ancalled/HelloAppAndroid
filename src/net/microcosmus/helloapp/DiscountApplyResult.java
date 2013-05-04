package net.microcosmus.helloapp;

public class DiscountApplyResult {

    public static enum Status {
        OK, ERRORS
    }

    private Long id;

    private Status status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
