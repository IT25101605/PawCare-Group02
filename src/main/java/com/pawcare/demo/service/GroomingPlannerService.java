package com.pawcare.demo.service;

import com.pawcare.demo.dto.GroomingPlanRequest;
import com.pawcare.demo.entity.GroomingService;
import com.pawcare.demo.exception.ResourceNotFoundException;
import com.pawcare.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/** An explainable catalogue matcher, not a medical assessment or an AI diagnosis. */
@Service
@Transactional(readOnly = true)
public class GroomingPlannerService {
    private final PetRepository pets;
    private final GroomingServiceRepository services;

    public GroomingPlannerService(PetRepository pets, GroomingServiceRepository services) {
        this.pets = pets;
        this.services = services;
    }

    public record Option(Long id, String name, BigDecimal price, int durationMinutes, String reason) {}
    public record Plan(String petName, List<Option> services, BigDecimal totalPrice,
                       int totalMinutes, int visitLimitMinutes, List<String> explanations, String comfortNote) {}

    public Plan recommend(GroomingPlanRequest request) {
        var pet = pets.findById(request.petId()).orElseThrow(() -> new ResourceNotFoundException("Pet", "id", request.petId()));
        SecurityService.requireOwner(pet.getOwner().getId());
        int limit = request.comfort() == GroomingPlanRequest.Comfort.CALM
                ? request.maxMinutes() : Math.min(request.maxMinutes(), 45);
        String comfortNote = switch (request.comfort()) {
            case CALM -> "Owner reports that this pet is comfortable with grooming.";
            case FIRST_VISIT -> "First grooming visit: introduce tools slowly and check comfort before starting.";
            case NERVOUS -> "Owner reports a nervous pet: discuss handling and breaks before starting.";
        };
        List<String> reasons = new ArrayList<>();
        reasons.add("Uses current catalogue prices and durations; booking availability is checked separately.");
        if (request.comfort() != GroomingPlanRequest.Comfort.CALM)
            reasons.add("Your comfort preference limits this suggested visit to 45 minutes or your shorter time limit.");
        String species = pet.getSpecies().getName().toLowerCase(Locale.ROOT);
        if (!List.of("dog", "cat").contains(species)) {
            reasons.add("Automatic matching is available for dogs and cats. Ask a groomer about services for this species.");
            return new Plan(pet.getName(), List.of(), BigDecimal.ZERO, 0, limit, reasons, comfortNote);
        }
        // Deliberately match only known routine catalogue services. Treatments and unclassified
        // services require a human discussion; descriptions are not interpreted as medical advice.
        List<String> priorities = switch (request.goal()) {
            case FRESH_COAT -> List.of("basic bath", "de-shedding treatment");
            case QUICK_TIDY -> List.of("hygiene trim", "nail trimming");
            case FULL_STYLE -> List.of("full grooming");
        };
        List<GroomingService> catalogue = services.findByIsActiveTrue().stream()
                .filter(s -> s.getPrice() != null && s.getPrice().signum() >= 0
                        && s.getDurationMinutes() != null && s.getDurationMinutes() >= 15)
                .sorted(Comparator.comparing(GroomingService::getPrice).thenComparing(GroomingService::getId)).toList();
        List<Option> selected = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int minutes = 0;
        // Fresh-coat options are alternatives; never stack two baths or repeat a full package.
        for (String name : priorities) {
            for (var service : catalogue) {
                if (!service.getName().equalsIgnoreCase(name)) continue;
                if (total.add(service.getPrice()).compareTo(request.budget()) > 0
                        || minutes + service.getDurationMinutes() > limit) continue;
                selected.add(new Option(service.getId(), service.getName(), service.getPrice(), service.getDurationMinutes(),
                        "Matches your " + request.goal().name().toLowerCase(Locale.ROOT).replace('_', ' ') + " priority and fits your remaining budget and time."));
                total = total.add(service.getPrice());
                minutes += service.getDurationMinutes();
                break;
            }
            if (!selected.isEmpty() && request.goal() != GroomingPlanRequest.Goal.QUICK_TIDY) break;
        }
        if (selected.isEmpty()) reasons.add("No matching service fits these limits. Increase the budget or time, choose another priority, or discuss a shorter visit with your groomer.");
        else reasons.add("Only services matching your priority are included; your remaining budget is not automatically spent.");
        reasons.add("Confirm species, coat suitability and handling needs with your groomer before the visit.");
        return new Plan(pet.getName(), selected, total, minutes, limit, reasons, comfortNote);
    }
}
