package com.toollix.common.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action")
    private String action;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "details", columnDefinition = "text")
    private String details;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public AuditLog() {}

    public AuditLog(String action, String performedBy, String details) {
        this.action = action;
        this.performedBy = performedBy;
        this.details = details;
    }

    public Long getId() { return id; }
}
