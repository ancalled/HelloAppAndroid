package net.microcosmus.helloapp.domain;

public class DiscountApplyResult {

    public static enum Status {
        OK, NO_USER_FOUND, NO_DISCOUNT_FOUND, NO_CONFIRMER_FOUND, CONFIRMER_IS_NOT_OF_THIS_COMPANY, COULD_NOT_APPLY
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
