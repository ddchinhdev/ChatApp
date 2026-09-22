package com.chatapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_group", nullable = false)
    private Boolean group = false;

    @Column(length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (group == null) group = false;
    }

    public Long getId() { return id; }
    public Boolean getGroup() { return group; }
    public String getName() { return name; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setGroup(Boolean group) { this.group = group; }
    public void setName(String name) { this.name = name; }
}
