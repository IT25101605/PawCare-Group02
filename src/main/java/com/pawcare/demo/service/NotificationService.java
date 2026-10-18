package com.pawcare.demo.service;

import com.pawcare.demo.dto.NotificationDTO;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public Page<NotificationDTO> getByUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toDTO);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationDTO create(Long userId, String title, String message,
                                  Notification.NotificationType type, String link) {
        return create(userId,title,message,type,Notification.Priority.INFO,link,null,null,null);
    }

    @Transactional
    public NotificationDTO create(Long userId, String title, String message,
                                  Notification.NotificationType type, Notification.Priority priority,
                                  String link, Long relatedEntityId, String relatedEntityType, String dedupeKey) {
        if (dedupeKey != null && notificationRepository.existsByDedupeKey(dedupeKey)) return null;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Notification n = Notification.builder()
                .user(user).title(title).message(message)
                .notificationType(type == null ? Notification.NotificationType.INFO : type)
                .priority(priority == null ? Notification.Priority.INFO : priority)
                .isRead(false).link(link).relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType).dedupeKey(dedupeKey)
                .build();

        return toDTO(notificationRepository.save(n));
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        Notification n = notificationRepository.findByIdAndUserId(id,userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged());
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications.getContent());
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Notification n = notificationRepository.findByIdAndUserId(id,userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        notificationRepository.delete(n);
    }

    public NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId()).userId(n.getUser().getId())
                .title(n.getTitle()).message(n.getMessage())
                .notificationType(n.getNotificationType().name())
                .priority(n.getPriority() == null ? Notification.Priority.INFO.name() : n.getPriority().name())
                .isRead(n.getIsRead()).link(n.getLink())
                .relatedEntityId(n.getRelatedEntityId()).relatedEntityType(n.getRelatedEntityType())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
