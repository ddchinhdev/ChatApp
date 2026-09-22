package com.chatapp.repository;

import com.chatapp.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {
}
