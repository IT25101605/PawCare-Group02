package com.pawcare.demo.service;

import com.pawcare.demo.dto.VeterinarianProfileDTO;
import com.pawcare.demo.entity.StakeholderProfile;
import com.pawcare.demo.entity.User;
import com.pawcare.demo.repository.StakeholderProfileRepository;
import com.pawcare.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VeterinarianProfileService {
    private final UserRepository users;
    private final StakeholderProfileRepository profiles;

    public VeterinarianProfileService(UserRepository users, StakeholderProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    public List<VeterinarianProfileDTO> getActiveVeterinarians() {
        return users.findByRoleName("VETERINARIAN").stream()
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()))
                .map(this::toDTO)
                .sorted(Comparator.comparing(VeterinarianProfileDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private VeterinarianProfileDTO toDTO(User user) {
        StakeholderProfile profile = profiles.findById(user.getId()).orElse(null);
        return VeterinarianProfileDTO.builder()
                .id(user.getId())
                .name(user.getFullName())
                .profileImage(user.getProfileImage())
                .organization(profile == null ? null : profile.getOrganization())
                .qualification(profile == null ? null : profile.getQualification())
                .experienceYears(profile == null ? null : profile.getExperienceYears())
                .specialty(profile == null ? null : profile.getSpecialty())
                .build();
    }
}
