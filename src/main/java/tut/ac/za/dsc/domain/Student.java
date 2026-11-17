package tut.ac.za.dsc.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** A registered student and the state of his or her digital card. */
@Entity
@Table(name = "student")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String studentNumber;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 13)
    private String idNumber;

    private LocalDate birthDate;

    @Column(length = 10)
    private String gender;

    @Column(length = 40)
    private String nationality;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 80)
    private String campus;

    @Column(length = 120)
    private String department;

    @Column(length = 120)
    private String qualification;

    private Integer yearOfStudy;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    /** PENDING, ACTIVE, SUSPENDED or REVOKED. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private CardStatus status;

    @Column(length = 250)
    private String statusReason;

    /** Goes up when a replacement card is issued, so QR codes on older cards stop verifying. */
    @Column(nullable = false)
    private int cardVersion = 1;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private boolean hasPhoto;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ------------------------------------------------------------ values shown on pages

    /** Status as shown on the card, taking the expiry date into account. */
    public CardStatus getEffectiveStatus() {
        if (status == CardStatus.ACTIVE && expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            return CardStatus.EXPIRED;
        }
        return status == null ? CardStatus.ACTIVE : status;
    }

    public String getStatusLabel() {
        return getEffectiveStatus().getLabel();
    }

    /** Lower-case status, used for CSS classes: active, pending, expired, suspended, revoked. */
    public String getStatusKey() {
        return getEffectiveStatus().name().toLowerCase();
    }

    public boolean isCardValid() {
        return getEffectiveStatus() == CardStatus.ACTIVE;
    }

    public String getFirstName() {
        return fullName == null ? "" : fullName.trim().split("\\s+")[0];
    }

    public String getInitials() {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] p = fullName.trim().split("\\s+");
        return (p[0].substring(0, 1) + (p.length > 1 ? p[p.length - 1].substring(0, 1) : "")).toUpperCase();
    }

    /** ID number with the middle digits hidden. */
    public String getMaskedIdNumber() {
        if (idNumber == null || idNumber.length() != 13) return idNumber;
        return idNumber.substring(0, 6) + " •••• ••" + idNumber.substring(12);
    }

    /** Student number grouped for reading aloud, e.g. 264 810 375. */
    public String getStudentNumberDisplay() {
        if (studentNumber == null || studentNumber.length() != 9) return studentNumber;
        return studentNumber.substring(0, 3) + " " + studentNumber.substring(3, 6) + " " + studentNumber.substring(6);
    }

    /** Cellphone number grouped for reading, e.g. 082 123 4567. */
    public String getPhoneDisplay() {
        if (phoneNumber == null) return null;
        if (phoneNumber.matches("0\\d{9}")) {
            return phoneNumber.substring(0, 3) + " " + phoneNumber.substring(3, 6) + " " + phoneNumber.substring(6);
        }
        if (phoneNumber.matches("\\+27\\d{9}")) {
            return "+27 " + phoneNumber.substring(3, 5) + " " + phoneNumber.substring(5, 8) + " " + phoneNumber.substring(8);
        }
        return phoneNumber;
    }

    /** Days until the card expires (negative once expired). */
    public long getDaysToExpiry() {
        return expiryDate == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public boolean isExpiringSoon() {
        return isCardValid() && getDaysToExpiry() <= 30;
    }

    public String getBirthDateText() { return Formats.longDay(birthDate); }
    public String getIssueDateText() { return Formats.day(issueDate); }
    public String getExpiryDateText() { return Formats.day(expiryDate); }
    public String getCreatedAtText() { return Formats.dayTime(createdAt); }
    public String getLastLoginText() { return Formats.dayTime(lastLoginAt); }

    // ------------------------------------------------------------ getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public Integer getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }
    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }
    public int getCardVersion() { return cardVersion; }
    public void setCardVersion(int cardVersion) { this.cardVersion = cardVersion; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public boolean isHasPhoto() { return hasPhoto; }
    public void setHasPhoto(boolean hasPhoto) { this.hasPhoto = hasPhoto; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
