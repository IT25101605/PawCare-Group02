package com.pawcare.demo.service;

import com.pawcare.demo.dto.PetPassportDTO;
import com.pawcare.demo.dto.PublicPetPassportDTO;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.BadRequestException;
import com.pawcare.demo.exception.ResourceNotFoundException;
import com.pawcare.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PetPassportService {
    private final PetRepository pets;
    private final VaccinationRepository vaccinations;
    private final UserRepository users;
    private final StakeholderProfileRepository profiles;

    public PetPassportService(PetRepository pets, VaccinationRepository vaccinations,
                              UserRepository users, StakeholderProfileRepository profiles) {
        this.pets = pets; this.vaccinations = vaccinations; this.users = users; this.profiles = profiles;
    }

    @Transactional
    public PetPassportDTO getSettings(Long petId) {
        Pet pet = ownedPet(petId);
        ensureToken(pet);
        return toSettings(pet);
    }

    @Transactional
    public PetPassportDTO updateSettings(Long petId, PetPassportDTO dto) {
        Pet pet = ownedPet(petId);
        ensureToken(pet);
        if (dto.getEmergencyVetId() != null) {
            User vet = users.findById(dto.getEmergencyVetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Veterinarian", "id", dto.getEmergencyVetId()));
            if (vet.getRole() == null || !"VETERINARIAN".equals(vet.getRole().getName()) || !Boolean.TRUE.equals(vet.getEnabled())) {
                throw new BadRequestException("Choose an active veterinarian for the emergency contact");
            }
        }
        pet.setPassportEnabled(bool(dto.getEnabled(), false));
        pet.setPassportShowPhoto(bool(dto.getShowPhoto(), true));
        pet.setPassportShowOwnerContact(bool(dto.getShowOwnerContact(), false));
        pet.setPassportShowAllergies(bool(dto.getShowAllergies(), true));
        pet.setPassportShowMedicalWarnings(bool(dto.getShowMedicalWarnings(), true));
        pet.setPassportShowVaccinations(bool(dto.getShowVaccinations(), true));
        pet.setPassportShowEmergencyVet(bool(dto.getShowEmergencyVet(), true));
        pet.setPassportAllergies(clean(dto.getAllergies()));
        pet.setPassportMedicalWarnings(clean(dto.getMedicalWarnings()));
        pet.setPassportEmergencyVetId(dto.getEmergencyVetId());
        return toSettings(pets.save(pet));
    }

    @Transactional(readOnly = true)
    public PublicPetPassportDTO getPublic(String token) {
        Pet pet = pets.findByPassportToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Pet passport", "token", token));
        if (!Boolean.TRUE.equals(pet.getPassportEnabled()) || !Boolean.TRUE.equals(pet.getIsActive())) {
            throw new ResourceNotFoundException("Pet passport", "token", token);
        }
        User owner = pet.getOwner();
        PublicPetPassportDTO.OwnerContact ownerContact = null;
        if (Boolean.TRUE.equals(pet.getPassportShowOwnerContact())) {
            ownerContact = PublicPetPassportDTO.OwnerContact.builder()
                    .name(owner.getFullName()).phone(owner.getPhone()).email(owner.getEmail()).build();
        }

        PublicPetPassportDTO.EmergencyVet emergencyVet = null;
        if (Boolean.TRUE.equals(pet.getPassportShowEmergencyVet()) && pet.getPassportEmergencyVetId() != null) {
            User vet = users.findById(pet.getPassportEmergencyVetId()).orElse(null);
            if (vet != null && vet.getRole() != null && "VETERINARIAN".equals(vet.getRole().getName()) && Boolean.TRUE.equals(vet.getEnabled())) {
                StakeholderProfile profile = profiles.findById(vet.getId()).orElse(null);
                emergencyVet = PublicPetPassportDTO.EmergencyVet.builder()
                        .name(vet.getFullName()).phone(vet.getPhone())
                        .organization(profile == null ? null : profile.getOrganization())
                        .qualification(profile == null ? null : profile.getQualification())
                        .specialty(profile == null ? null : profile.getSpecialty()).build();
            }
        }

        List<PublicPetPassportDTO.VaccinationItem> vaccineItems = List.of();
        if (Boolean.TRUE.equals(pet.getPassportShowVaccinations())) {
            vaccineItems = vaccinations.findByPetId(pet.getId()).stream()
                    .sorted(Comparator.comparing(Vaccination::getDateAdministered, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Vaccination::getId, Comparator.reverseOrder()))
                    .limit(12)
                    .map(v -> PublicPetPassportDTO.VaccinationItem.builder()
                            .vaccineName(v.getVaccineName()).dateAdministered(v.getDateAdministered())
                            .nextDueDate(v.getNextDueDate()).build()).toList();
        }

        return PublicPetPassportDTO.builder()
                .petName(pet.getName())
                .photo(Boolean.TRUE.equals(pet.getPassportShowPhoto()) ? pet.getPhoto() : null)
                .species(pet.getSpecies() == null ? null : pet.getSpecies().getName())
                .breed(pet.getBreed() == null ? null : pet.getBreed().getName())
                .gender(pet.getGender() == null ? null : pet.getGender().name())
                .color(pet.getColor()).microchipId(null)
                .allergies(Boolean.TRUE.equals(pet.getPassportShowAllergies()) ? pet.getPassportAllergies() : null)
                .medicalWarnings(Boolean.TRUE.equals(pet.getPassportShowMedicalWarnings()) ? pet.getPassportMedicalWarnings() : null)
                .ownerContact(ownerContact).emergencyVet(emergencyVet).vaccinations(vaccineItems).build();
    }

    private Pet ownedPet(Long petId) {
        Pet pet = pets.findById(petId).orElseThrow(() -> new ResourceNotFoundException("Pet", "id", petId));
        SecurityService.requireOwner(pet.getOwner().getId());
        if (!Boolean.TRUE.equals(pet.getIsActive())) throw new ResourceNotFoundException("Pet", "id", petId);
        return pet;
    }
    private void ensureToken(Pet pet) {
        if (pet.getPassportToken() == null || pet.getPassportToken().isBlank()) {
            pet.setPassportToken(UUID.randomUUID().toString()); pets.save(pet);
        }
    }
    private PetPassportDTO toSettings(Pet p) {
        return PetPassportDTO.builder().petId(p.getId()).petName(p.getName()).passportToken(p.getPassportToken())
                .enabled(Boolean.TRUE.equals(p.getPassportEnabled()))
                .showPhoto(!Boolean.FALSE.equals(p.getPassportShowPhoto()))
                .showOwnerContact(Boolean.TRUE.equals(p.getPassportShowOwnerContact()))
                .showAllergies(!Boolean.FALSE.equals(p.getPassportShowAllergies()))
                .showMedicalWarnings(!Boolean.FALSE.equals(p.getPassportShowMedicalWarnings()))
                .showVaccinations(!Boolean.FALSE.equals(p.getPassportShowVaccinations()))
                .showEmergencyVet(!Boolean.FALSE.equals(p.getPassportShowEmergencyVet()))
                .allergies(p.getPassportAllergies()).medicalWarnings(p.getPassportMedicalWarnings())
                .emergencyVetId(p.getPassportEmergencyVetId()).build();
    }
    private static Boolean bool(Boolean value, boolean fallback) { return value == null ? fallback : value; }
    private static String clean(String value) {
        if (value == null) return null;
        String v = value.strip();
        return v.isEmpty() ? null : v;
    }
}
