package com.chatapp.repository;

import com.chatapp.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @EntityGraph(attributePaths = {"sender", "conversation"})
    Optional<Message> findBySenderIdAndClientMessageId(Long senderId, String clientMessageId);

    @Override
    @EntityGraph(attributePaths = {"sender", "conversation"})
    Optional<Message> findById(Long id);

    @Query("""
            select count(m) from Message m
            where m.conversation.id = :conversationId
              and m.sender.id <> :userId
              and m.id > :lastReadMessageId
            """)
    long countUnread(@Param("conversationId") Long conversationId,
                     @Param("userId") Long userId,
                     @Param("lastReadMessageId") Long lastReadMessageId);

    @EntityGraph(attributePaths = "sender")
    @Query("""
            select m from Message m
            where m.conversation.id = :conversationId
              and (:cursorTime is null or m.createdAt < :cursorTime
                   or (m.createdAt = :cursorTime and m.id < :cursorId))
            order by m.createdAt desc, m.id desc
            """)
    List<Message> findHistory(@Param("conversationId") Long conversationId,
                              @Param("cursorTime") LocalDateTime cursorTime,
                              @Param("cursorId") Long cursorId,
                              Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "conversation"})
    @Query("""
            select m from Message m
            where m.id > :afterMessageId
              and exists (select cm.id from ConversationMember cm
                          where cm.conversation = m.conversation and cm.user.id = :userId)
            order by m.id asc
            """)
    List<Message> findMessagesForSync(@Param("userId") Long userId,
                                      @Param("afterMessageId") Long afterMessageId,
                                      Pageable pageable);
}
