package tut.ac.za.dsc.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Who did what, and when. */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String actor;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(length = 60)
    private String target;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(String actor, String action, String target, String details) {
        this.actor = cut(actor == null ? "system" : actor, 60);
        this.action = cut(action, 60);
        this.target = cut(target, 60);
        this.details = cut(details, 1000);
        this.occurredAt = LocalDateTime.now();
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    public String getOccurredAtText() { return Formats.dayTime(occurredAt); }

    /** True when the target looks like a student number, so pages can link to the student. */
    public boolean isStudentTarget() { return target != null && target.matches("\\d{9}"); }

    public Long getId() { return id; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getTarget() { return target; }
    public String getDetails() { return details; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
