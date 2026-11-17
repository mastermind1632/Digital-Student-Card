package tut.ac.za.dsc.domain;

/** Lifecycle of a digital student card. EXPIRED is never stored; it is worked out from the expiry date. */
public enum CardStatus {
    PENDING("Awaiting approval"),
    ACTIVE("Valid"),
    EXPIRED("Expired"),
    SUSPENDED("Suspended"),
    REVOKED("Revoked");

    private final String label;

    CardStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
