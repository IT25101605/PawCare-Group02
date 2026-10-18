package com.pawcare.demo.service;

import com.pawcare.demo.pet.builder.PetRegistrationBuilder;
import com.pawcare.demo.dto.PetDTO;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class PetService {

    /*
     * Maximum sensible weights for the built-in pet categories.
     * Backend validation is authoritative; the frontend mirrors these values
     * only to give the owner immediate feedback before submitting the form.
     */
    private static final Map<String, BigDecimal> MAX_WEIGHT_KG = Map.of(
            "dog", new BigDecimal("120.00"),
            "cat", new BigDecimal("25.00"),
            "bird", new BigDecimal("5.00"),
            "rabbit", new BigDecimal("15.00"),
            "fish", new BigDecimal("50.00"),
            "hamster", new BigDecimal("1.00")
    );
    private static final BigDecimal CUSTOM_SPECIES_MAX_WEIGHT_KG = new BigDecimal("200.00");

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final NotificationService notifications;
    private final AppointmentRepository appointmentRepository;
    private final GroomingAppointmentRepository groomingAppointmentRepository;

    public PetService(PetRepository petRepository, UserRepository userRepository,
                      SpeciesRepository speciesRepository, BreedRepository breedRepository,
                      NotificationService notifications, AppointmentRepository appointmentRepository,
                      GroomingAppointmentRepository groomingAppointmentRepository) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.speciesRepository = speciesRepository;
        this.breedRepository = breedRepository;
        this.notifications = notifications;this.appointmentRepository = appointmentRepository;
        this.groomingAppointmentRepository = groomingAppointmentRepository;
    }

    public Page<PetDTO> getAllPets(Pageable pageable) {
        return petRepository.findByIsActiveTrue(newest(pageable)).map(this::toDTO);
    }

    public Page<PetDTO> getPetsByOwner(Long ownerId, Pageable pageable) {
        return petRepository.findByOwnerIdAndIsActiveTrue(ownerId, newest(pageable)).map(this::toDTO);
    }

    public List<PetDTO> getPetListByOwner(Long ownerId) {
        return petRepository.findByOwnerIdAndIsActiveTrueOrderByCreatedAtDescIdDesc(ownerId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    private Pageable newest(Pageable p){return PageRequest.of(p.getPageNumber(),p.getPageSize(),Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")));}

    public PetDTO getPetById(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", id));
        if (SecurityService.hasRole("PET_OWNER")) SecurityService.requireOwner(pet.getOwner().getId());
        if (!Boolean.TRUE.equals(pet.getIsActive())) throw new ResourceNotFoundException("Pet", "id", id);
        return toDTO(pet);
    }

    public Page<PetDTO> searchPets(String search, Pageable pageable) {
        return petRepository.searchByName(search, newest(pageable)).map(this::toDTO);
    }

    public void assertCanModify(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", id));
        SecurityService.requireOwner(pet.getOwner().getId());
        if (!Boolean.TRUE.equals(pet.getIsActive())) throw new BadRequestException("This pet was removed");
    }

    @Transactional
    public PetDTO createPet(PetDTO dto, Long ownerId) {
        normalizePet(dto);
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));
        SecurityService.requireOwner(ownerId);
        if (!owner.getRole().getName().equals("PET_OWNER")) throw new BadRequestException("Choose a pet owner");

        Species species = resolveCreateSpecies(dto);
        validateWeightForSpecies(dto.getWeight(), species);

        Pet pet = new PetRegistrationBuilder()
                .from(dto).species(species).owner(owner).build();

        if (dto.getBreedId() != null) {
            Breed breed = breedRepository.findById(dto.getBreedId())
                    .orElseThrow(() -> new ResourceNotFoundException("Breed", "id", dto.getBreedId()));
            if (!breed.getSpecies().getId().equals(pet.getSpecies().getId())) throw new BadRequestException("Breed must belong to the selected species");
            pet.setBreed(breed);
        } else if (dto.getCustomBreedName() != null && !dto.getCustomBreedName().strip().isEmpty()) {
            String customBreed = dto.getCustomBreedName().strip();
            Breed breed = breedRepository.findByNameIgnoreCaseAndSpeciesId(customBreed, species.getId())
                    .orElseGet(() -> breedRepository.save(Breed.builder().name(customBreed).species(species).description("Custom breed added by pet owner").build()));
            pet.setBreed(breed);
        }

        return toDTO(petRepository.save(pet));
    }

    @Transactional
    public PetDTO updatePet(Long id, PetDTO dto) {
        normalizePet(dto);
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", id));

        SecurityService.requireOwner(pet.getOwner().getId());
        if (!Boolean.TRUE.equals(pet.getIsActive())) throw new BadRequestException("This pet was removed");

        Species targetSpecies = pet.getSpecies();
        if (dto.getSpeciesId() != null || hasText(dto.getCustomSpeciesName())) {
            targetSpecies = resolveCreateSpecies(dto);
        }

        BigDecimal oldWeight = pet.getWeight();
        BigDecimal targetWeight = Boolean.TRUE.equals(dto.getClearWeight())
                ? null
                : (dto.getWeight() != null ? dto.getWeight() : pet.getWeight());
        validateWeightForSpecies(targetWeight, targetSpecies);

        if (Boolean.TRUE.equals(dto.getClearBreed())) pet.setBreed(null);
        if (Boolean.TRUE.equals(dto.getClearDateOfBirth())) pet.setDateOfBirth(null);
        if (Boolean.TRUE.equals(dto.getClearWeight())) pet.setWeight(null);
        if (dto.getName() != null) pet.setName(dto.getName());
        if (dto.getPhoto() != null) pet.setPhoto(dto.getPhoto());
        if (dto.getDateOfBirth() != null) pet.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getGender() != null) pet.setGender(Pet.Gender.valueOf(dto.getGender()));
        if (dto.getWeight() != null) pet.setWeight(dto.getWeight());
        if (dto.getColor() != null) pet.setColor(dto.getColor());
        if (dto.getMicrochipId() != null) pet.setMicrochipId(dto.getMicrochipId());
        if (dto.getNotes() != null) pet.setNotes(dto.getNotes());

        if (dto.getSpeciesId() != null || hasText(dto.getCustomSpeciesName())) {
            if (!targetSpecies.getId().equals(pet.getSpecies().getId())) pet.setBreed(null);
            pet.setSpecies(targetSpecies);
        }
        if (dto.getBreedId() != null) {
            Breed breed = breedRepository.findById(dto.getBreedId())
                    .orElseThrow(() -> new ResourceNotFoundException("Breed", "id", dto.getBreedId()));
            if (!breed.getSpecies().getId().equals(pet.getSpecies().getId())) throw new BadRequestException("Breed must belong to the selected species");
            pet.setBreed(breed);
        } else if (hasText(dto.getCustomBreedName())) {
            String customBreed = dto.getCustomBreedName().strip();
            Species resolvedSpecies = pet.getSpecies();
            pet.setBreed(breedRepository.findByNameIgnoreCaseAndSpeciesId(customBreed, resolvedSpecies.getId())
                    .orElseGet(() -> breedRepository.save(Breed.builder().name(customBreed)
                            .species(resolvedSpecies).description("Custom breed added by pet owner").build())));
        }

        pet = petRepository.save(pet);
        if (dto.getWeight() != null && (oldWeight == null || oldWeight.compareTo(dto.getWeight()) != 0)) {
            notifications.create(pet.getOwner().getId(), "Pet weight updated",
                    pet.getName() + "'s weight record has been updated to " + dto.getWeight().stripTrailingZeros().toPlainString() + " kg.",
                    Notification.NotificationType.PET_WEIGHT, Notification.Priority.INFO,
                    "/pages/dashboard/owner.html#pets", pet.getId(), "PET",
                    "pet-weight-" + pet.getId() + "-" + pet.getUpdatedAt());
        }
        return toDTO(pet);
    }

    @Transactional
    public void deletePet(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", id));
        SecurityService.requireOwner(pet.getOwner().getId());
        if (!Boolean.TRUE.equals(pet.getIsActive())) throw new BadRequestException("Pet is already removed");
        // Keep in-progress treatment/grooming attached to a currently active pet.
        if (appointmentRepository.findByPetId(id).stream().anyMatch(a -> a.getStatus()==Appointment.AppointmentStatus.IN_PROGRESS)
                || groomingAppointmentRepository.findByPetId(id).stream().anyMatch(g -> g.getStatus()==GroomingAppointment.GroomingStatus.IN_PROGRESS))
            throw new BadRequestException("Finish the ongoing veterinary or grooming visit before removing this pet");
        // Linked medical history, vaccinations and bookings must be preserved.
        for (Appointment appointment : appointmentRepository.findByPetId(id)) {
            if (appointment.getStatus()==Appointment.AppointmentStatus.SCHEDULED || appointment.getStatus()==Appointment.AppointmentStatus.CONFIRMED) {
                appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);
                appointmentRepository.save(appointment);
            }
        }
        for (GroomingAppointment visit : groomingAppointmentRepository.findByPetId(id)) {
            if (visit.getStatus()==GroomingAppointment.GroomingStatus.SCHEDULED || visit.getStatus()==GroomingAppointment.GroomingStatus.CONFIRMED) {
                visit.setStatus(GroomingAppointment.GroomingStatus.CANCELLED);
                groomingAppointmentRepository.save(visit);
            }
        }
        pet.setIsActive(false);
        pet.setPassportEnabled(false);
        petRepository.save(pet);
    }

    private Species resolveCreateSpecies(PetDTO dto) {
        if (dto.getSpeciesId() != null) {
            return speciesRepository.findById(dto.getSpeciesId())
                    .orElseThrow(() -> new ResourceNotFoundException("Species", "id", dto.getSpeciesId()));
        }
        String custom = dto.getCustomSpeciesName() == null ? "" : dto.getCustomSpeciesName().strip();
        if (custom.isEmpty()) throw new BadRequestException("Species is required");
        return speciesRepository.findByNameIgnoreCase(custom)
                .orElseGet(() -> speciesRepository.save(Species.builder().name(custom).description("Custom species added by pet owner").build()));
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }

    private void normalizePet(PetDTO dto) {
        if (dto.getName() != null) dto.setName(dto.getName().strip().replaceAll("\\s+", " "));
        if (dto.getColor() != null) dto.setColor(dto.getColor().strip());
        if (dto.getCustomSpeciesName() != null) dto.setCustomSpeciesName(dto.getCustomSpeciesName().strip());
        if (dto.getCustomBreedName() != null) dto.setCustomBreedName(dto.getCustomBreedName().strip());
        if (dto.getName() != null) {
            if (dto.getName().length() < 2 || dto.getName().length() > 50)
                throw new BadRequestException("Pet name must be between 2 and 50 characters");
            if (!dto.getName().matches("^[\\p{L}]+(?: [\\p{L}]+)*$"))
                throw new BadRequestException("Pet name may contain letters and spaces only");
        }
        if (dto.getColor() != null && !dto.getColor().isEmpty() && !dto.getColor().matches("[\\p{L} ]+"))
            throw new BadRequestException("Color must contain letters and spaces only");
        if (dto.getWeight() != null && dto.getWeight().compareTo(BigDecimal.ZERO) <= 0)
            throw new BadRequestException("Weight must be greater than 0");
        if (dto.getDateOfBirth() != null && dto.getDateOfBirth().isAfter(java.time.LocalDate.now()))
            throw new BadRequestException("Birth date cannot be in the future");
    }

    private void validateWeightForSpecies(BigDecimal weight, Species species) {
        if (weight == null) return;
        if (weight.compareTo(BigDecimal.ZERO) <= 0) throw new BadRequestException("Weight must be greater than 0");
        String speciesName = species.getName() == null ? "" : species.getName().strip();
        BigDecimal max = MAX_WEIGHT_KG.getOrDefault(speciesName.toLowerCase(Locale.ROOT), CUSTOM_SPECIES_MAX_WEIGHT_KG);
        if (weight.compareTo(max) > 0) {
            String displayMax = max.stripTrailingZeros().toPlainString();
            throw new BadRequestException("Weight for " + speciesName + " must be at most " + displayMax + " kg");
        }
    }

    private PetDTO toDTO(Pet pet) {
        return PetDTO.builder()
                .id(pet.getId()).name(pet.getName())
                .speciesId(pet.getSpecies().getId()).speciesName(pet.getSpecies().getName())
                .breedId(pet.getBreed() != null ? pet.getBreed().getId() : null)
                .breedName(pet.getBreed() != null ? pet.getBreed().getName() : null)
                .dateOfBirth(pet.getDateOfBirth())
                .gender(pet.getGender() != null ? pet.getGender().name() : null)
                .weight(pet.getWeight()).color(pet.getColor())
                .microchipId(pet.getMicrochipId()).photo(pet.getPhoto()).notes(pet.getNotes())
                .ownerId(pet.getOwner().getId())
                .ownerName(pet.getOwner().getFirstName() + " " + pet.getOwner().getLastName())
                .isActive(pet.getIsActive())
                .build();
    }
}
