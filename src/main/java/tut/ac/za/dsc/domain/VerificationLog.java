package tut.ac.za.dsc.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One check of a card: a scan at a gate, a QR link opened with a phone camera, or a typed student number. */
@Entity
@Table(name = "verification_log")
public class VerificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String studentNumber;

    @Column(length = 100)
    private String studentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private VerificationResult result;

    /** SCANNER, PUBLIC_LINK or MANUAL. */
    @Column(nullable = false, length = 12)
    private String method;

    @Column(length = 60)
    private String checkedBy;

    @Column(length = 80)
    private String location;

    @Column(length = 64)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime checkedAt;

    @PrePersist
    void onCreate() {
        if (checkedAt == null) checkedAt = LocalDateTime.now();
    }

    public boolean isPassed() { return result == VerificationResult.VALID; }
    public String getResultLabel() { return result == null ? "" : result.getShortLabel(); }
    public String getCheckedAtText() { return Formats.dayTime(checkedAt); }

    public String getMethodLabel() {
        if ("SCANNER".equals(method)) return "Scanner";
        if ("PUBLIC_LINK".equals(method)) return "Phone camera link";
        if ("MANUAL".equals(method)) return "Typed number";
        return method;
    }

    public Long getId() { return id; }
    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public VerificationResult getResult() { return result; }
    public void setResult(VerificationResult result) { this.result = result; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getCheckedBy() { return checkedBy; }
    public void setCheckedBy(String checkedBy) { this.checkedBy = checkedBy; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
}
