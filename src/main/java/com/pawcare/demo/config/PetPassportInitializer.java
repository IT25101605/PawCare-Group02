package com.pawcare.demo.config;

import com.pawcare.demo.entity.Pet;
import com.pawcare.demo.repository.PetRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class PetPassportInitializer implements ApplicationRunner {
    private final PetRepository pets;

    public PetPassportInitializer(PetRepository pets) { this.pets = pets; }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean changed = false;
        for (Pet pet : pets.findAll()) {
            if (pet.getPassportToken() == null || pet.getPassportToken().isBlank()) {
                pet.setPassportToken(UUID.randomUUID().toString()); changed = true;
            }
            if (pet.getPassportEnabled() == null) { pet.setPassportEnabled(false); changed = true; }
            if (pet.getPassportShowPhoto() == null) { pet.setPassportShowPhoto(true); changed = true; }
            if (pet.getPassportShowOwnerContact() == null) { pet.setPassportShowOwnerContact(false); changed = true; }
            if (pet.getPassportShowAllergies() == null) { pet.setPassportShowAllergies(true); changed = true; }
            if (pet.getPassportShowMedicalWarnings() == null) { pet.setPassportShowMedicalWarnings(true); changed = true; }
            if (pet.getPassportShowVaccinations() == null) { pet.setPassportShowVaccinations(true); changed = true; }
            if (pet.getPassportShowEmergencyVet() == null) { pet.setPassportShowEmergencyVet(true); changed = true; }
        }
        if (changed) pets.flush();
    }
}
