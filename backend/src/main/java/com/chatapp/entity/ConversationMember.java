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

    @Column(name = "last_delivered_message_id")
    private Long lastDeliveredMessageId;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_role", nullable = false, length = 20)
    private GroupRole groupRole = GroupRole.MEMBER;

    @PrePersist
    void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public User getUser() { return user; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public Long getLastDeliveredMessageId() { return lastDeliveredMessageId; }
    public Long getLastReadMessageId() { return lastReadMessageId; }
    public GroupRole getGroupRole() { return groupRole; }

    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public void setUser(User user) { this.user = user; }
    public void setLastDeliveredMessageId(Long value) { this.lastDeliveredMessageId = value; }
    public void setLastReadMessageId(Long value) { this.lastReadMessageId = value; }
    public void setGroupRole(GroupRole groupRole) { this.groupRole = groupRole; }
}
