package com.pawcare.demo.medical.specification;

import com.pawcare.demo.entity.MedicalRecord;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.Locale;

/** Specification Pattern: reusable, composable JPA search predicates. */
public final class MedicalRecordSpecifications {
    private MedicalRecordSpecifications() { }
    public static Specification<MedicalRecord> all() { return (r,q,c) -> c.conjunction(); }
    public static Specification<MedicalRecord> forVeterinarian(Long id) {
        return (r,q,c) -> c.equal(r.get("veterinarian").get("id"), id);
    }
    public static Specification<MedicalRecord> forOwner(Long id) {
        return (r,q,c) -> c.equal(r.get("pet").get("owner").get("id"), id);
    }
    public static Specification<MedicalRecord> forPet(Long id) {
        return (r,q,c) -> c.equal(r.get("pet").get("id"), id);
    }
    public static Specification<MedicalRecord> forAppointment(Long id) {
        return (r,q,c) -> c.equal(r.get("appointment").get("id"), id);
    }
    public static Specification<MedicalRecord> recordType(String type) {
        return (r,q,c) -> c.equal(r.get("recordType"), type);
    }
    public static Specification<MedicalRecord> fromDate(LocalDate date) {
        return (r,q,c) -> c.greaterThanOrEqualTo(r.get("recordDate"), date);
    }
    public static Specification<MedicalRecord> toDate(LocalDate date) {
        return (r,q,c) -> c.lessThanOrEqualTo(r.get("recordDate"), date);
    }
    private static String contains(String query) { return "%" + query.toLowerCase(Locale.ROOT) + "%"; }
    public static Specification<MedicalRecord> ownerName(String name) {
        return (r,q,c) -> c.like(c.lower(c.concat(c.concat(r.get("pet").get("owner").get("firstName")," "),r.get("pet").get("owner").get("lastName"))), contains(name));
    }
    public static Specification<MedicalRecord> veterinarianName(String name) {
        return (r,q,c) -> c.like(c.lower(c.concat(c.concat(r.get("veterinarian").get("firstName")," "),r.get("veterinarian").get("lastName"))), contains(name));
    }
    public static Specification<MedicalRecord> diagnosis(String term) {
        return (r,q,c) -> c.like(c.lower(r.get("diagnosis")), contains(term));
    }
    public static Specification<MedicalRecord> treatment(String term) {
        return (r,q,c) -> c.like(c.lower(r.get("treatment")), contains(term));
    }
    public static Specification<MedicalRecord> keyword(String term) {
        return (r,q,c) -> c.or(
            c.like(c.lower(r.get("pet").get("name")),contains(term)),
            c.like(c.lower(r.get("diagnosis")),contains(term)),
            c.like(c.lower(r.get("treatment")),contains(term)),
            c.like(c.lower(r.get("notes")),contains(term)));
    }
}
