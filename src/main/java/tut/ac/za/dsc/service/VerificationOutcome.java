package tut.ac.za.dsc.service;

import tut.ac.za.dsc.domain.Student;
import tut.ac.za.dsc.domain.VerificationResult;

/** The answer to "is this card genuine and valid right now?". Student is null when it is unknown or forged. */
public class VerificationOutcome {

    private final VerificationResult result;
    private final Student student;

    public VerificationOutcome(VerificationResult result, Student student) {
        this.result = result;
        this.student = student;
    }

    public VerificationResult getResult() { return result; }
    public Student getStudent() { return student; }
    public boolean isValid() { return result == VerificationResult.VALID; }
    public String getHeadline() { return result.getHeadline(); }
    public String getAdvice() { return result.getAdvice(); }
    public String getTone() { return result.getTone(); }
}
