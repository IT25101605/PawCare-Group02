package com.pawcare.demo.service;

import com.pawcare.demo.dto.*;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import com.pawcare.demo.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class SecureMessagingService {
    private final ConversationMessageRepository messages;
    private final MessageConversationRepository conversations;
    private final PetRepository pets;
    private final UserRepository users;
    private final AppointmentRepository appointments;
    private final DocumentStorageService storage;
    private final NotificationService notifications;

    public SecureMessagingService(ConversationMessageRepository messages,
                                  MessageConversationRepository conversations,
                                  PetRepository pets,
                                  UserRepository users,
                                  AppointmentRepository appointments,
                                  DocumentStorageService storage,
                                  NotificationService notifications) {
        this.messages = messages;
        this.conversations = conversations;
        this.pets = pets;
        this.users = users;
        this.appointments = appointments;
        this.storage = storage;
        this.notifications = notifications;
    }

    private CustomUserDetails me() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof CustomUserDetails user)) throw new AccessDeniedException("Authentication required");
        return user;
    }

    private User currentUser() {
        return users.findById(me().getId()).orElseThrow(() -> new ResourceNotFoundException("User", "id", me().getId()));
    }

    private ParticipantPair authorizePair(Pet pet, User other, Long appointmentId) {
        User sender = currentUser();
        User owner = pet.getOwner();
        User vet;

        if (SecurityService.hasRole("PET_OWNER")) {
            if (!owner.getId().equals(sender.getId()) || !"VETERINARIAN".equals(other.getRole().getName())) {
                throw new AccessDeniedException("Conversation is not authorized");
            }
            vet = other;
        } else if (SecurityService.hasRole("VETERINARIAN")) {
            if (!"PET_OWNER".equals(other.getRole().getName()) || !owner.getId().equals(other.getId())) {
                throw new AccessDeniedException("Conversation is not authorized");
            }
            vet = sender;
        } else {
            throw new AccessDeniedException("Only pet owners and veterinarians may send messages");
        }

        if (!appointments.existsByPetIdAndVeterinarianId(pet.getId(), vet.getId())) {
            throw new AccessDeniedException("Messaging is available only for an authorized veterinarian-pet relationship");
        }

        Appointment appointment = null;
        if (appointmentId != null) {
            appointment = appointments.findById(appointmentId)
                    .orElseThrow(() -> new BadRequestException("Appointment not found"));
            if (!appointment.getPet().getId().equals(pet.getId())
                    || !appointment.getOwner().getId().equals(owner.getId())
                    || !appointment.getVeterinarian().getId().equals(vet.getId())) {
                throw new AccessDeniedException("Appointment is not part of this conversation");
            }
        }
        return new ParticipantPair(owner, vet, appointment);
    }

    private void requireConversationAccess(MessageConversation conversation) {
        Long uid = me().getId();
        if (!conversation.getOwner().getId().equals(uid) && !conversation.getVeterinarian().getId().equals(uid)) {
            throw new AccessDeniedException("This conversation belongs to another user");
        }
    }

    @Transactional
    protected MessageConversation findOrCreateConversation(Pet pet, ParticipantPair pair) {
        MessageConversation conversation = conversations
                .findByPetIdAndOwnerIdAndVeterinarianId(pet.getId(), pair.owner().getId(), pair.vet().getId())
                .orElseGet(() -> conversations.save(MessageConversation.builder()
                        .pet(pet).owner(pair.owner()).veterinarian(pair.vet())
                        .latestAppointment(pair.appointment()).latestMessageAt(LocalDateTime.now()).build()));

        if (pair.appointment() != null) conversation.setLatestAppointment(pair.appointment());

        List<ConversationMessage> legacy = messages.findLegacyThread(pet.getId(), pair.owner().getId(), pair.vet().getId());
        if (!legacy.isEmpty()) {
            for (ConversationMessage message : legacy) message.setConversation(conversation);
            messages.saveAll(legacy);
            ConversationMessage latest = legacy.get(legacy.size() - 1);
            if (latest.getCreatedAt() != null && (conversation.getLatestMessageAt() == null
                    || latest.getCreatedAt().isAfter(conversation.getLatestMessageAt()))) {
                conversation.setLatestMessageAt(latest.getCreatedAt());
            }
            if (latest.getAppointment() != null) conversation.setLatestAppointment(latest.getAppointment());
        }
        return conversations.save(conversation);
    }

    public List<ConversationMessageDTO> myMessages() {
        Long uid = me().getId();
        return messages.findBySenderIdOrRecipientIdOrderByCreatedAtDescIdDesc(uid, uid).stream().map(this::dto).toList();
    }

    public List<ConversationSummaryDTO> conversationList(String search) {
        Long uid = me().getId();
        List<MessageConversation> list = conversations.findForParticipant(uid);
        String q = search == null ? "" : search.strip().toLowerCase(Locale.ROOT);
        return list.stream().map(c -> summary(c, uid))
                .filter(c -> q.isBlank() || contains(c.getPetName(), q) || contains(c.getOtherUserName(), q)
                        || contains(c.getLatestMessage(), q))
                .toList();
    }

    public List<ConversationMessageDTO> thread(Long conversationId) {
        MessageConversation conversation = conversations.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        requireConversationAccess(conversation);
        return messages.findByConversationIdOrderByCreatedAtAscIdAsc(conversationId).stream().map(this::dto).toList();
    }

    @Transactional
    public List<ConversationMessageDTO> thread(Long petId, Long otherUserId) {
        Pet pet = pets.findById(petId).orElseThrow(() -> new ResourceNotFoundException("Pet", "id", petId));
        User other = users.findById(otherUserId).orElseThrow(() -> new ResourceNotFoundException("User", "id", otherUserId));
        ParticipantPair pair = authorizePair(pet, other, null);
        MessageConversation conversation = findOrCreateConversation(pet, pair);
        return thread(conversation.getId());
    }

    @Transactional
    public ConversationMessageDTO send(Long petId, Long recipientId, Long appointmentId, String text, MultipartFile file) {
        if ((text == null || text.isBlank()) && (file == null || file.isEmpty())) {
            throw new BadRequestException("Enter a message or attach a file");
        }
        if (text != null && text.length() > 2000) throw new BadRequestException("Message must be 2000 characters or fewer");

        Pet pet = pets.findById(petId).orElseThrow(() -> new ResourceNotFoundException("Pet", "id", petId));
        User recipient = users.findById(recipientId).orElseThrow(() -> new ResourceNotFoundException("User", "id", recipientId));
        ParticipantPair pair = authorizePair(pet, recipient, appointmentId);
        User sender = currentUser();
        MessageConversation conversation = findOrCreateConversation(pet, pair);

        ConversationMessage.ConversationMessageBuilder builder = ConversationMessage.builder()
                .conversation(conversation).pet(pet).sender(sender).recipient(recipient)
                .message(text == null ? "" : text.strip());
        if (pair.appointment() != null) builder.appointment(pair.appointment());
        var stored = storage.save(file);
        if (stored != null) builder.attachmentName(stored.originalName()).attachmentUrl(stored.url()).attachmentType(stored.contentType());

        ConversationMessage saved = messages.save(builder.build());
        conversation.setLatestMessageAt(saved.getCreatedAt() == null ? LocalDateTime.now() : saved.getCreatedAt());
        if (pair.appointment() != null) conversation.setLatestAppointment(pair.appointment());
        conversations.save(conversation);

        String preview = saved.getMessage() == null || saved.getMessage().isBlank()
                ? "Sent an attachment: " + Optional.ofNullable(saved.getAttachmentName()).orElse("file")
                : saved.getMessage();
        if (preview.length() > 140) preview = preview.substring(0, 137) + "...";
        String link = "PET_OWNER".equals(recipient.getRole().getName())
                ? "/pages/dashboard/owner.html#messages" : "/pages/dashboard/vet.html#messages";
        notifications.create(recipient.getId(), "New message about " + pet.getName(),
                sender.getFullName() + ": " + preview,
                Notification.NotificationType.INFO, Notification.Priority.INFO, link,
                conversation.getId(), "MESSAGE_CONVERSATION", "message-" + saved.getId());
        return dto(saved);
    }

    public FileData attachment(Long id) {
        ConversationMessage message = messages.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", id));
        if (message.getConversation() != null) requireConversationAccess(message.getConversation());
        else {
            Long uid = me().getId();
            if (!message.getSender().getId().equals(uid) && !message.getRecipient().getId().equals(uid)) {
                throw new AccessDeniedException("Not your message");
            }
        }
        if (message.getAttachmentUrl() == null) throw new ResourceNotFoundException("Attachment", "messageId", id);
        return new FileData(message.getAttachmentName(), message.getAttachmentType(), storage.read(message.getAttachmentUrl()));
    }

    public record FileData(String name, String contentType, byte[] bytes) {}

    @Transactional
    public void markRead(Long id) {
        ConversationMessage message = messages.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", id));
        if (!message.getRecipient().getId().equals(me().getId())) {
            throw new AccessDeniedException("Not your message");
        }
        if (message.getReadAt() == null) message.setReadAt(LocalDateTime.now());
        messages.save(message);
    }

    @Transactional
    public void markConversationRead(Long conversationId) {
        MessageConversation conversation = conversations.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        requireConversationAccess(conversation);
        messages.markConversationRead(conversationId, me().getId());
    }

    private ConversationSummaryDTO summary(MessageConversation conversation, Long uid) {
        boolean isOwner = conversation.getOwner().getId().equals(uid);
        User other = isOwner ? conversation.getVeterinarian() : conversation.getOwner();
        ConversationMessage latest = messages.findTopByConversationIdOrderByCreatedAtDescIdDesc(conversation.getId()).orElse(null);
        long unread = messages.countByConversationIdAndRecipientIdAndReadAtIsNull(conversation.getId(), uid);
        Appointment appointment = conversation.getLatestAppointment();
        String latestText = latest == null ? "No messages yet" : latest.getMessage();
        if ((latestText == null || latestText.isBlank()) && latest != null && latest.getAttachmentName() != null) latestText = "Attachment: " + latest.getAttachmentName();
        return ConversationSummaryDTO.builder()
                .id(conversation.getId()).petId(conversation.getPet().getId()).petName(conversation.getPet().getName())
                .otherUserId(other.getId()).otherUserName(other.getFullName()).otherUserRole(other.getRole().getName())
                .appointmentId(appointment == null ? null : appointment.getId())
                .appointmentDate(appointment == null ? null : appointment.getAppointmentDate().toString())
                .latestMessage(latestText).latestMessageAt(conversation.getLatestMessageAt())
                .unreadCount(unread).latestAttachmentName(latest == null ? null : latest.getAttachmentName()).build();
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private ConversationMessageDTO dto(ConversationMessage message) {
        return ConversationMessageDTO.builder()
                .id(message.getId()).conversationId(message.getConversation() == null ? null : message.getConversation().getId())
                .petId(message.getPet().getId()).petName(message.getPet().getName())
                .appointmentId(message.getAppointment() != null ? message.getAppointment().getId() : null)
                .senderId(message.getSender().getId()).senderName(message.getSender().getFullName())
                .recipientId(message.getRecipient().getId()).recipientName(message.getRecipient().getFullName())
                .message(message.getMessage()).attachmentName(message.getAttachmentName())
                .attachmentUrl(message.getAttachmentUrl() != null ? "/api/messages/" + message.getId() + "/attachment" : null)
                .attachmentType(message.getAttachmentType()).createdAt(message.getCreatedAt()).readAt(message.getReadAt()).build();
    }

    private record ParticipantPair(User owner, User vet, Appointment appointment) {}
}
