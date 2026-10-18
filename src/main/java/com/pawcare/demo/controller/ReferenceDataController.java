package com.pawcare.demo.controller;

import com.pawcare.demo.entity.Breed;
import com.pawcare.demo.entity.Species;
import com.pawcare.demo.repository.BreedRepository;
import com.pawcare.demo.repository.SpeciesRepository;
import com.pawcare.demo.service.GroomerProfileService;
import com.pawcare.demo.service.VeterinarianProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @org.springframework.transaction.annotation.Transactional(readOnly=true)
@RequestMapping("/api")
@Tag(name = "Reference Data", description = "Species, breeds and care professionals")
public class ReferenceDataController {

    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final GroomerProfileService groomerProfiles;
    private final VeterinarianProfileService veterinarianProfiles;

    public ReferenceDataController(SpeciesRepository speciesRepository, BreedRepository breedRepository,
                                   GroomerProfileService groomerProfiles,
                                   VeterinarianProfileService veterinarianProfiles) {
        this.speciesRepository = speciesRepository;
        this.breedRepository = breedRepository;
        this.groomerProfiles = groomerProfiles;
        this.veterinarianProfiles = veterinarianProfiles;
    }

    @GetMapping("/veterinarians")
    public ResponseEntity<List<com.pawcare.demo.dto.VeterinarianProfileDTO>> veterinarians() {
        return ResponseEntity.ok(veterinarianProfiles.getActiveVeterinarians());
    }

    @GetMapping("/groomers")
    public ResponseEntity<List<com.pawcare.demo.dto.GroomerProfileDTO>> groomers() {
        return ResponseEntity.ok(groomerProfiles.getActiveGroomers());
    }

    @GetMapping("/species")
    public ResponseEntity<List<Species>> getAllSpecies() {
        return ResponseEntity.ok(speciesRepository.findAll());
    }

    @GetMapping("/breeds")
    public ResponseEntity<List<java.util.Map<String,Object>>> getAllBreeds() {
        return ResponseEntity.ok(breedRepository.findAll().stream().map(b -> java.util.Map.<String,Object>of("id",b.getId(),"name",b.getName(),"speciesId",b.getSpecies().getId())).toList());
    }

    @GetMapping("/breeds/species/{speciesId}")
    public ResponseEntity<List<java.util.Map<String,Object>>> getBreedsBySpecies(@PathVariable Long speciesId) {
        return ResponseEntity.ok(breedRepository.findBySpeciesId(speciesId).stream().map(b -> java.util.Map.<String,Object>of("id",b.getId(),"name",b.getName(),"speciesId",speciesId)).toList());
    }
}
