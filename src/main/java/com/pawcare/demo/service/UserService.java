package com.pawcare.demo.service;

import com.pawcare.demo.dto.UserDTO;
import com.pawcare.demo.entity.Notification;
import com.pawcare.demo.entity.Role;
import com.pawcare.demo.entity.User;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.RoleRepository;
import com.pawcare.demo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notifications;
    private final ImageStorageService imageStorage;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       NotificationService notifications, ImageStorageService imageStorage) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.notifications = notifications;
        this.imageStorage = imageStorage;
    }

    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(newest(pageable)).map(this::toDTO);
    }

    public Page<UserDTO> searchUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable).map(this::toDTO);
    }

    public Page<UserDTO> getUsersByRole(String roleName, Pageable pageable) {
        return userRepository.findByRoleName(roleName, newest(pageable)).map(this::toDTO);
    }

    private Pageable newest(Pageable p){return PageRequest.of(p.getPageNumber(),p.getPageSize(),Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")));}

    public UserDTO getUserById(Long id) {
        return toDTO(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id)));
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        boolean wasEnabled = Boolean.TRUE.equals(user.getEnabled());

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getAddress() != null) user.setAddress(dto.getAddress());
        if (dto.getCity() != null) user.setCity(dto.getCity());
        if (dto.getState() != null) user.setState(dto.getState());
        if (dto.getZipCode() != null) user.setZipCode(dto.getZipCode());

        if ((dto.getRole() != null || dto.getEnabled() != null) && !SecurityService.hasRole("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException("Only administrators can change roles or approval status");
        }
        if (dto.getRole() != null && !dto.getRole().equals(user.getRole().getName())) {
            throw new BadRequestException("Role changes require a new role-specific registration");
        }
        if (dto.getRole() != null) {
            Role role = roleRepository.findByName(dto.getRole())
                    .orElseThrow(() -> new BadRequestException("Invalid role"));
            user.setRole(role);
        }

        if (dto.getEnabled() != null) user.setEnabled(dto.getEnabled());

        user = userRepository.save(user);
        if (!wasEnabled && Boolean.TRUE.equals(user.getEnabled())) {
            notifications.create(user.getId(), "Account approved",
                    "Your PawCare professional account has been approved. You can now sign in.",
                    Notification.NotificationType.ACCOUNT, Notification.Priority.SUCCESS,
                    "/pages/login.html", user.getId(), "USER", "account-approved-" + user.getId());
        }
        return toDTO(user);
    }


    @Transactional
    public UserDTO updateProfileImage(Long id, org.springframework.web.multipart.MultipartFile file) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        if (!java.util.Set.of("PET_OWNER","VETERINARIAN","GROOMER").contains(user.getRole().getName()))
            throw new BadRequestException("Profile photo upload is available for pet owners, veterinarians and groomers");
        String newImage = imageStorage.save(file);
        if (newImage == null) throw new BadRequestException("Choose a JPEG or PNG image");
        String oldImage = user.getProfileImage();
        user.setProfileImage(newImage);
        User saved = userRepository.save(user);
        if (oldImage != null && !oldImage.equals(newImage)) imageStorage.remove(oldImage);
        return toDTO(saved);
    }

    @Transactional
    public UserDTO removeProfileImage(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        if (!java.util.Set.of("PET_OWNER","VETERINARIAN","GROOMER").contains(user.getRole().getName()))
            throw new BadRequestException("Profile photo upload is available for pet owners, veterinarians and groomers");
        String oldImage = user.getProfileImage();
        user.setProfileImage(null);
        User saved = userRepository.save(user);
        imageStorage.remove(oldImage);
        return toDTO(saved);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        userRepository.deleteById(id);
    }

    public UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId()).firstName(user.getFirstName()).lastName(user.getLastName())
                .email(user.getEmail()).phone(user.getPhone()).address(user.getAddress())
                .city(user.getCity()).state(user.getState()).zipCode(user.getZipCode())
                .profileImage(user.getProfileImage()).enabled(user.getEnabled())
                .emailVerified(user.getEmailVerified()).role(user.getRole().getName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
