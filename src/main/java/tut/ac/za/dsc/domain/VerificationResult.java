package tut.ac.za.dsc.domain;

/** Outcome of checking a card, with the words shown to the person checking it. */
public enum VerificationResult {
    VALID("Valid", "Valid student card", "Compare the photo with the person presenting the card.", "pass"),
    EXPIRED("Expired", "Card has expired", "Ask the student to renew the card at the student card office.", "warn"),
    PENDING("Not yet approved", "Card not yet approved", "The registration has not been approved yet. Access cannot be granted with this card.", "warn"),
    SUSPENDED("Suspended", "Card is suspended", "Do not allow access. Refer the student to the student card office.", "fail"),
    REVOKED("Revoked", "Card has been revoked", "Do not allow access. Refer the student to the student card office.", "fail"),
    SUPERSEDED("Old card (re-issued)", "Old card: a newer one was issued", "This QR code belongs to a card that was replaced. Ask for the student's current card.", "fail"),
    TAMPERED("QR code not genuine", "QR code is not genuine", "The code was not issued by this system. Do not allow access.", "fail"),
    NOT_FOUND("No such student", "No student found", "Check the number and try again, or ask for another form of identification.", "fail");

    private final String shortLabel;
    private final String headline;
    private final String advice;
    private final String tone;

    VerificationResult(String shortLabel, String headline, String advice, String tone) {
        this.shortLabel = shortLabel;
        this.headline = headline;
        this.advice = advice;
        this.tone = tone;
    }

    public String getShortLabel() { return shortLabel; }
    public String getHeadline() { return headline; }
    public String getAdvice() { return advice; }
    /** pass, warn or fail: the colour of the result banner. */
    public String getTone() { return tone; }

    public static VerificationResult of(CardStatus status) {
        switch (status) {
            case ACTIVE: return VALID;
            case EXPIRED: return EXPIRED;
            case PENDING: return PENDING;
            case SUSPENDED: return SUSPENDED;
            default: return REVOKED;
        }
    }
}
