package tut.ac.za.dsc.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** The card photo, kept apart from {@link Student} so student lists never load images. */
@Entity
@Table(name = "student_photo")
public class StudentPhoto {

    @Id
    private Long studentId;

    @Column(nullable = false)
    private byte[] content;

    @Column(nullable = false, length = 40)
    private String contentType;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected StudentPhoto() {
    }

    public StudentPhoto(Long studentId, byte[] content, String contentType) {
        this.studentId = studentId;
        this.content = content;
        this.contentType = contentType;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getStudentId() { return studentId; }
    public byte[] getContent() { return content; }
    public String getContentType() { return contentType; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void replace(byte[] content, String contentType) {
        this.content = content;
        this.contentType = contentType;
        this.updatedAt = LocalDateTime.now();
    }
}
