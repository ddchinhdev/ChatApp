package com.chatapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User admin;

    @Column(nullable = false, length = 50)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() {
        createdAt = LocalDateTime.now(Clock.systemUTC()).truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public User getAdmin() { return admin; }
    public String getAction() { return action; }
    public User getTargetUser() { return targetUser; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setAdmin(User admin) { this.admin = admin; }
    public void setAction(String action) { this.action = action; }
    public void setTargetUser(User targetUser) { this.targetUser = targetUser; }
    public void setDetails(String details) { this.details = details; }
}
