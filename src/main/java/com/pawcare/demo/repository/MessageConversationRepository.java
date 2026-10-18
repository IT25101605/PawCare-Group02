package com.pawcare.demo.repository;

import com.pawcare.demo.entity.MessageConversation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface MessageConversationRepository extends JpaRepository<MessageConversation, Long> {
    Optional<MessageConversation> findByPetIdAndOwnerIdAndVeterinarianId(Long petId, Long ownerId, Long veterinarianId);

    @Query("""
        SELECT c FROM MessageConversation c
        WHERE c.owner.id = :userId OR c.veterinarian.id = :userId
        ORDER BY c.latestMessageAt DESC, c.id DESC
        """)
    List<MessageConversation> findForParticipant(@Param("userId") Long userId);
}
