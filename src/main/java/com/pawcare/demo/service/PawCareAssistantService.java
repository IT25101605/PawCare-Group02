package com.pawcare.demo.service;

import com.pawcare.demo.entity.*;
import com.pawcare.demo.repository.*;
import com.pawcare.demo.security.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class PawCareAssistantService {
    private final PetRepository pets;
    private final VaccinationRepository vaccinations;
    private final AppointmentRepository appointments;
    private final GroomingAppointmentRepository grooming;
    private final MedicalRecordRepository medicalRecords;

    public PawCareAssistantService(PetRepository pets, VaccinationRepository vaccinations,
                                   AppointmentRepository appointments, GroomingAppointmentRepository grooming,
                                   MedicalRecordRepository medicalRecords) {
        this.pets = pets; this.vaccinations = vaccinations; this.appointments = appointments;
        this.grooming = grooming; this.medicalRecords = medicalRecords;
    }

    public Map<String,Object> answer(String question, CustomUserDetails user, Long requestedPetId) {
        String q = question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
        Pet contextPet = resolveContextPet(user, requestedPetId, q);
        String answer;
        String category = "GENERAL";

        // Context-aware PawCare data is deliberately factual: it reports records, never diagnoses.
        if (contextPet != null && asksVaccinationRecord(q)) {
            category = "PET_RECORD";
            answer = vaccinationAnswer(contextPet, q);
        } else if (contextPet != null && asksGroomingRecord(q)) {
            category = "PET_RECORD";
            answer = groomingAnswer(contextPet);
        } else if (contextPet != null && asksAppointmentRecord(q)) {
            category = "PET_RECORD";
            answer = appointmentAnswer(contextPet, q);
        } else if (contextPet != null && asksWeightRecord(q)) {
            category = "PET_RECORD";
            answer = weightAnswer(contextPet);
        } else if (contextPet != null && (q.contains("allerg") || q.contains("warning"))) {
            category = "PET_RECORD";
            answer = emergencyNotesAnswer(contextPet, q);
        } else if (contextPet != null && asksPetSummary(q)) {
            category = "PET_RECORD";
            answer = petSummary(contextPet);
        } else if (q.isBlank()) {
            answer = "Ask me about PawCare navigation, appointments, grooming, vaccinations, medical terms, shipping, or general pet-care guidance.";
        } else if (isHealthConcern(q)) {
            category = "SAFETY";
            answer = "I can explain general pet-care information, but I can't diagnose symptoms from a chat. Please contact a veterinarian for assessment; if your pet appears seriously unwell or the situation feels urgent, seek veterinary care promptly.";
        } else if (q.contains("shipping") || q.contains("delivery charge")) {
            category = "PAWCARE";
            answer = "For online-payment orders, shipping is Rs. 350 when the delivery city is Kurunegala (case-insensitive) or the postal code is 60000. Other online-payment delivery locations are Rs. 500. Pay-at-shop cash keeps the existing no-online-shipping behavior.";
        } else if (q.contains("passport") || q.contains("qr")) {
            category = "PAWCARE";
            answer = "Pet owners can open My Pets and choose Passport on a pet card. There you can enable the Digital Pet Passport, choose exactly what is public, select an emergency veterinarian, and show or save the pet's QR code.";
        } else if (q.contains("add") && q.contains("pet")) {
            category = "PAWCARE";
            answer = "Open your Pet Owner Dashboard, choose Add Pet, complete the pet profile, then save it. PawCare validates the pet name, species/breed, date of birth, photo and species-appropriate weight.";
        } else if ((q.contains("book") || q.contains("appointment")) && (q.contains("vet") || q.contains("veterinarian"))) {
            category = "PAWCARE";
            answer = "In the Pet Owner Dashboard, open Appointments, select your pet and veterinarian, choose a date, then pick an available time slot and enter the reason for the visit.";
        } else if (q.contains("groom")) {
            category = "PAWCARE";
            answer = "Open Grooming, select your pet and service, choose the groomer you prefer, then select an available date and time. That booking is shown only to the selected groomer.";
        } else if (q.contains("password")) {
            category = "PAWCARE";
            answer = "Use My Account to change your password. PawCare also asks for your current password before sensitive actions such as pet edits/deletes and owner appointment cancellations.";
        } else if (q.contains("booster")) {
            category = "MEDICAL_TERM";
            answer = "A booster vaccination is an additional vaccine dose given after an earlier dose to help maintain immune protection. Your veterinarian can advise the correct schedule for your pet.";
        } else if (q.contains("otitis")) {
            category = "MEDICAL_TERM";
            answer = "Otitis refers to ear inflammation. The exact cause and treatment depend on the pet and should be assessed by a veterinarian.";
        } else if (q.contains("vaccin")) {
            category = "PET_CARE";
            answer = "Vaccinations help protect pets from preventable diseases. The appropriate vaccines and timing depend on species, age, health and local risk, so follow your veterinarian's schedule. PawCare can remind you about recorded due dates.";
        } else {
            category = "PET_CARE";
            answer = contextPet == null
                    ? "I can provide general pet-care information and explain common terms, but I can't diagnose a pet. If you're a pet owner, open Ask PawCare from one of your pet cards to ask about that pet's recorded appointments, vaccinations or weight history."
                    : "I can report " + contextPet.getName() + "'s recorded PawCare information such as vaccinations, next appointments, grooming visits and weight history. For symptoms, treatment decisions or medicine doses, a veterinarian should assess your pet.";
        }

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("answer", answer);
        result.put("category", category);
        result.put("aiGenerated", true);
        result.put("notice", "Record summaries and general guidance only — not a veterinary diagnosis.");
        if (contextPet != null) {
            result.put("contextPetId", contextPet.getId());
            result.put("contextPetName", contextPet.getName());
        }
        return result;
    }

    private Pet resolveContextPet(CustomUserDetails user, Long requestedPetId, String q) {
        if (user == null || !"PET_OWNER".equals(user.getRole())) return null;
        List<Pet> owned = pets.findByOwnerIdAndIsActiveTrue(user.getId());
        if (requestedPetId != null) {
            return owned.stream().filter(p -> p.getId().equals(requestedPetId)).findFirst().orElse(null);
        }
        for (Pet pet : owned) {
            if (pet.getName() != null && q.contains(pet.getName().toLowerCase(Locale.ROOT))) return pet;
        }
        return owned.size() == 1 ? owned.get(0) : null;
    }

    private String vaccinationAnswer(Pet pet, String q) {
        List<Vaccination> rows = new ArrayList<>(vaccinations.findByPetId(pet.getId()));
        rows.sort(Comparator.comparing(Vaccination::getDateAdministered, Comparator.nullsLast(Comparator.reverseOrder())));
        if (rows.isEmpty()) return "There are no vaccination records saved for " + pet.getName() + " yet.";
        if (q.contains("next") || q.contains("due") || q.contains("upcoming")) {
            LocalDate today = LocalDate.now();
            return rows.stream().filter(v -> v.getNextDueDate() != null && !v.getNextDueDate().isBefore(today))
                    .min(Comparator.comparing(Vaccination::getNextDueDate))
                    .map(v -> pet.getName() + "'s next recorded vaccination due date is " + fmt(v.getNextDueDate()) + " for " + v.getVaccineName() + ".")
                    .orElse("No upcoming vaccination due date is currently recorded for " + pet.getName() + ".");
        }
        Vaccination last = rows.get(0);
        String due = last.getNextDueDate() == null ? "" : " The recorded next due date is " + fmt(last.getNextDueDate()) + ".";
        return pet.getName() + "'s latest recorded vaccination is " + last.getVaccineName() + " on " + fmt(last.getDateAdministered()) + "." + due;
    }

    private String appointmentAnswer(Pet pet, String q) {
        LocalDate today = LocalDate.now(); LocalTime now = LocalTime.now();
        Optional<Appointment> nextVet = appointments.findByPetId(pet.getId()).stream()
                .filter(a -> activeVet(a.getStatus()) && future(a.getAppointmentDate(), a.getAppointmentTime(), today, now))
                .min(Comparator.comparing(Appointment::getAppointmentDate).thenComparing(Appointment::getAppointmentTime));
        Optional<GroomingAppointment> nextGroom = grooming.findByPetId(pet.getId()).stream()
                .filter(a -> activeGroom(a.getStatus()) && future(a.getAppointmentDate(), a.getAppointmentTime(), today, now))
                .min(Comparator.comparing(GroomingAppointment::getAppointmentDate).thenComparing(GroomingAppointment::getAppointmentTime));
        if (q.contains("groom")) return groomingAnswer(pet);
        if (nextVet.isEmpty() && nextGroom.isEmpty()) return "There are no upcoming appointments recorded for " + pet.getName() + ".";
        if (nextVet.isPresent() && (nextGroom.isEmpty() || compare(nextVet.get(), nextGroom.get()) <= 0)) {
            Appointment a = nextVet.get();
            return pet.getName() + "'s next appointment is a veterinary visit with " + a.getVeterinarian().getFullName() + " on " + fmt(a.getAppointmentDate()) + " at " + a.getAppointmentTime() + ".";
        }
        GroomingAppointment g = nextGroom.get();
        String provider = g.getGroomer() == null ? "the grooming team" : g.getGroomer().getFullName();
        return pet.getName() + "'s next appointment is grooming with " + provider + " on " + fmt(g.getAppointmentDate()) + " at " + g.getAppointmentTime() + ".";
    }

    private String groomingAnswer(Pet pet) {
        LocalDate today = LocalDate.now(); LocalTime now = LocalTime.now();
        return grooming.findByPetId(pet.getId()).stream()
                .filter(g -> activeGroom(g.getStatus()) && future(g.getAppointmentDate(), g.getAppointmentTime(), today, now))
                .min(Comparator.comparing(GroomingAppointment::getAppointmentDate).thenComparing(GroomingAppointment::getAppointmentTime))
                .map(g -> pet.getName() + "'s next grooming appointment is on " + fmt(g.getAppointmentDate()) + " at " + g.getAppointmentTime() + " with " + (g.getGroomer() == null ? "the grooming team" : g.getGroomer().getFullName()) + ".")
                .orElse("There is no upcoming grooming appointment recorded for " + pet.getName() + ".");
    }

    private String weightAnswer(Pet pet) {
        List<MedicalRecord> rows = medicalRecords.findByPetIdOrderByRecordDateDescIdDesc(pet.getId()).stream()
                .filter(r -> r.getWeightAtVisit() != null).limit(5).toList();
        List<String> parts = new ArrayList<>();
        if (pet.getWeight() != null) parts.add("current profile: " + kg(pet.getWeight()) + " kg");
        for (MedicalRecord r : rows) parts.add(fmt(r.getRecordDate()) + ": " + kg(r.getWeightAtVisit()) + " kg");
        if (parts.isEmpty()) return "No weight history is currently recorded for " + pet.getName() + ".";
        return pet.getName() + "'s recorded weight history — " + String.join("; ", parts) + ".";
    }

    private String emergencyNotesAnswer(Pet pet, String q) {
        if (q.contains("allerg")) return textOrNone(pet.getName(), "allergies", pet.getPassportAllergies());
        return textOrNone(pet.getName(), "medical warnings", pet.getPassportMedicalWarnings());
    }

    private String petSummary(Pet pet) {
        String species = pet.getSpecies() == null ? "pet" : pet.getSpecies().getName();
        String breed = pet.getBreed() == null ? "" : ", " + pet.getBreed().getName();
        String weight = pet.getWeight() == null ? "" : ", " + kg(pet.getWeight()) + " kg";
        return pet.getName() + " is recorded in PawCare as " + species + breed + weight + ". You can ask me about vaccinations, upcoming appointments, grooming or weight history for this pet.";
    }

    private static boolean asksVaccinationRecord(String q) {
        boolean subject = q.contains("vaccin") || q.contains("booster") || q.contains("immun");
        return subject && (q.contains("last") || q.contains("latest") || q.contains("next") || q.contains("due") || q.contains("when") || q.contains("record") || q.contains("history") || q.contains("received") || q.contains("had"));
    }
    private static boolean asksGroomingRecord(String q) {
        return q.contains("groom") && (q.contains("next") || q.contains("appointment") || q.contains("when") || q.contains("upcoming") || q.contains("what time") || q.contains("what date"));
    }
    private static boolean asksAppointmentRecord(String q) {
        boolean subject = q.contains("appointment") || q.contains("visit") || q.contains("next vet");
        return subject && (q.contains("next") || q.contains("when") || q.contains("upcoming") || q.contains("what time") || q.contains("what date") || q.contains("scheduled") || q.contains("do i have"));
    }
    private static boolean asksWeightRecord(String q) {
        boolean subject = q.contains("weight") || q.contains("weigh");
        return subject && (q.contains("history") || q.contains("record") || q.contains("recent") || q.contains("current") || q.contains("last") || q.contains("show") || q.contains("how much does"));
    }
    private static boolean asksPetSummary(String q) { return q.contains("tell me about") || q.contains("profile") || q.contains("what do you know"); }
    private static boolean isHealthConcern(String q) { return q.contains("vomit") || q.contains("bleed") || q.contains("breath") || q.contains("seiz") || q.contains("collapse") || q.contains("sick") || q.contains("pain") || q.contains("diarr") || q.contains("not eating"); }
    private static boolean activeVet(Appointment.AppointmentStatus s) { return s != Appointment.AppointmentStatus.CANCELLED && s != Appointment.AppointmentStatus.NO_SHOW && s != Appointment.AppointmentStatus.COMPLETED; }
    private static boolean activeGroom(GroomingAppointment.GroomingStatus s) { return s != GroomingAppointment.GroomingStatus.CANCELLED && s != GroomingAppointment.GroomingStatus.COMPLETED; }
    private static boolean future(LocalDate d, LocalTime t, LocalDate today, LocalTime now) { return d != null && (d.isAfter(today) || (d.isEqual(today) && t != null && !t.isBefore(now))); }
    private static int compare(Appointment a, GroomingAppointment g) { int d=a.getAppointmentDate().compareTo(g.getAppointmentDate()); return d!=0?d:a.getAppointmentTime().compareTo(g.getAppointmentTime()); }
    private static String fmt(LocalDate d) { return d == null ? "an unspecified date" : d.format(DateTimeFormatter.ofPattern("dd MMM yyyy")); }
    private static String kg(BigDecimal n) { return n.stripTrailingZeros().toPlainString(); }
    private static String textOrNone(String pet, String label, String value) { return value == null || value.isBlank() ? "No " + label + " are currently recorded for " + pet + "." : pet + "'s recorded " + label + ": " + value.strip(); }
}
