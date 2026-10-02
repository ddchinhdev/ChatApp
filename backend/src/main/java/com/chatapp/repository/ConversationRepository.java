package com.chatapp.repository;

import com.chatapp.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @EntityGraph(attributePaths = {"lastMessage", "lastMessage.sender"})
    Optional<Conversation> findByDirectConversationKey(String directConversationKey);

    @EntityGraph(attributePaths = {"lastMessage", "lastMessage.sender"})
    @Query("""
            select c from Conversation c
            where exists (select cm.id from ConversationMember cm
                          where cm.conversation = c and cm.user.id = :userId)
            order by c.lastActivityAt desc, c.id desc
            """)
    List<Conversation> findAllForUser(@Param("userId") Long userId);

    @Override
    @EntityGraph(attributePaths = {"lastMessage", "lastMessage.sender"})
    Optional<Conversation> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id")
    Optional<Conversation> findByIdForUpdate(@Param("id") Long id);
}
