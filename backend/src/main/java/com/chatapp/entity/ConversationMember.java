package com.chatapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conversation_member",
                columnNames = {"conversation_id", "user_id"}))
public class ConversationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public User getUser() { return user; }
    public LocalDateTime getJoinedAt() { return joinedAt; }

    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public void setUser(User user) { this.user = user; }
}
