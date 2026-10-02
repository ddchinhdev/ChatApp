package com.chatapp.repository;

import com.chatapp.entity.ConversationMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);
    @EntityGraph(attributePaths = "user")
    List<ConversationMember> findByConversationIdOrderByIdAsc(Long conversationId);

    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cm from ConversationMember cm where cm.conversation.id = :conversationId and cm.user.id = :userId")
    Optional<ConversationMember> findForUpdate(@Param("conversationId") Long conversationId,
                                               @Param("userId") Long userId);
}
