package com.pawcare.demo.repository;

import com.pawcare.demo.entity.ConversationMessage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    List<ConversationMessage> findByPetIdAndSenderIdInAndRecipientIdInOrderByCreatedAtDescIdDesc(
            Long petId, Collection<Long> senders, Collection<Long> recipients);

    List<ConversationMessage> findBySenderIdOrRecipientIdOrderByCreatedAtDescIdDesc(Long senderId, Long recipientId);

    List<ConversationMessage> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);

    Optional<ConversationMessage> findTopByConversationIdOrderByCreatedAtDescIdDesc(Long conversationId);

    long countByConversationIdAndRecipientIdAndReadAtIsNull(Long conversationId, Long recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ConversationMessage m SET m.readAt = CURRENT_TIMESTAMP WHERE m.conversation.id=:conversationId AND m.recipient.id=:recipientId AND m.readAt IS NULL")
    int markConversationRead(@Param("conversationId") Long conversationId, @Param("recipientId") Long recipientId);

    @Query("""
        SELECT m FROM ConversationMessage m
        WHERE m.conversation IS NULL
          AND m.pet.id=:petId
          AND ((m.sender.id=:ownerId AND m.recipient.id=:vetId) OR (m.sender.id=:vetId AND m.recipient.id=:ownerId))
        ORDER BY m.createdAt ASC, m.id ASC
        """)
    List<ConversationMessage> findLegacyThread(@Param("petId") Long petId,
                                               @Param("ownerId") Long ownerId,
                                               @Param("vetId") Long vetId);
}
