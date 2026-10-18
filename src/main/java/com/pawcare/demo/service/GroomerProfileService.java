package com.pawcare.demo.service;

import com.pawcare.demo.dto.GroomerProfileDTO;
import com.pawcare.demo.dto.ReviewDTO;
import com.pawcare.demo.entity.StakeholderProfile;
import com.pawcare.demo.entity.User;
import com.pawcare.demo.repository.ReviewRepository;
import com.pawcare.demo.repository.StakeholderProfileRepository;
import com.pawcare.demo.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GroomerProfileService {
    private final UserRepository users;
    private final StakeholderProfileRepository profiles;
    private final ReviewRepository reviews;
    private final ReviewService reviewService;

    public GroomerProfileService(UserRepository users,
                                 StakeholderProfileRepository profiles,
                                 ReviewRepository reviews,
                                 ReviewService reviewService) {
        this.users = users;
        this.profiles = profiles;
        this.reviews = reviews;
        this.reviewService = reviewService;
    }

    public List<GroomerProfileDTO> getActiveGroomers() {
        return users.findByRoleName("GROOMER").stream()
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()))
                .map(this::toDTO)
                .sorted(Comparator.comparingDouble(GroomerProfileDTO::getAverageRating).reversed()
                        .thenComparing(GroomerProfileDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public java.util.Map<String,Object> getDetails(Long id) {
        requireGroomer(id);
        return details(profiles.findById(id).orElseGet(StakeholderProfile::new));
    }

    @Transactional
    public java.util.Map<String,Object> updateDetails(Long id, com.pawcare.demo.dto.GroomerDetailsRequest request) {
        User user=requireGroomer(id);
        StakeholderProfile profile=profiles.findById(id).orElseGet(()->{
            StakeholderProfile created=new StakeholderProfile();created.setUser(user);return created;
        });
        profile.setOrganization(clean(request.organization()));
        profile.setQualification(clean(request.qualification()));
        profile.setSpecialty(clean(request.specialty()));
        profile.setExperienceYears(request.experienceYears());
        return details(profiles.saveAndFlush(profile));
    }

    private User requireGroomer(Long id) {
        User user=users.findById(id).orElseThrow(()->new com.pawcare.demo.exception.ResourceNotFoundException("User","id",id));
        if(!"GROOMER".equals(user.getRole().getName()))throw new com.pawcare.demo.exception.BadRequestException("Professional grooming details belong to groomer accounts only");
        return user;
    }

    private String clean(String value) {return value==null||value.isBlank()?null:value.trim();}

    private java.util.Map<String,Object> details(StakeholderProfile profile) {
        var data=new java.util.LinkedHashMap<String,Object>();
        data.put("organization",profile.getOrganization());data.put("qualification",profile.getQualification());
        data.put("specialty",profile.getSpecialty());data.put("experienceYears",profile.getExperienceYears());return data;
    }

    private GroomerProfileDTO toDTO(User user) {
        StakeholderProfile profile = profiles.findById(user.getId()).orElse(null);
        Double storedAverage = reviews.averageGroomerRating(user.getId());
        double average = Math.round((storedAverage == null ? 0.0 : storedAverage) * 10.0) / 10.0;
        List<ReviewDTO> recent = reviews.publicGroomerReviews(user.getId(), PageRequest.of(0, 3))
                .stream().map(reviewService::toDTO).toList();
        return GroomerProfileDTO.builder()
                .id(user.getId())
                .name(user.getFullName())
                .profileImage(user.getProfileImage())
                .organization(profile == null ? null : profile.getOrganization())
                .specialty(profile == null ? null : profile.getSpecialty())
                .qualification(profile == null ? null : profile.getQualification())
                .experienceYears(profile == null ? null : profile.getExperienceYears())
                .averageRating(average)
                .totalReviews(reviews.countGroomerReviews(user.getId()))
                .recentReviews(recent)
                .build();
    }
}
