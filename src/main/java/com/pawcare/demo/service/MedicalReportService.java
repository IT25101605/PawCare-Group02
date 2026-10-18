package com.pawcare.demo.service;

import com.pawcare.demo.dto.MedicalRecordDTO;
import com.pawcare.demo.entity.Pet;
import com.pawcare.demo.entity.Vaccination;
import com.pawcare.demo.exception.ResourceNotFoundException;
import com.pawcare.demo.repository.PetRepository;
import com.pawcare.demo.repository.VaccinationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class MedicalReportService {
    private static final int LINES_PER_PAGE = 48;
    private final MedicalRecordService records;
    private final PetRepository pets;
    private final VaccinationRepository vaccinations;

    public MedicalReportService(MedicalRecordService records, PetRepository pets, VaccinationRepository vaccinations) {
        this.records = records;
        this.pets = pets;
        this.vaccinations = vaccinations;
    }

    public byte[] generate(Long petId) {
        records.assertCanViewPet(petId);
        Pet p = pets.findById(petId).orElseThrow(() -> new ResourceNotFoundException("Pet", "id", petId));
        List<MedicalRecordDTO> history = records.getListByPet(petId);
        List<Vaccination> vaccineHistory = vaccinations.findByPetId(petId).stream()
                .sorted((a, b) -> {
                    int byDate = b.getDateAdministered().compareTo(a.getDateAdministered());
                    return byDate != 0 ? byDate : Long.compare(b.getId(), a.getId());
                }).toList();

        List<String> lines = new ArrayList<>();
        lines.add("PAWCARE - PET HEALTH REPORT");
        lines.add("Generated: " + LocalDate.now());
        lines.add("");
        lines.add("PET INFORMATION");
        lines.add("Pet: " + p.getName());
        lines.add("Owner: " + p.getOwner().getFullName());
        lines.add("Species: " + p.getSpecies().getName() + "   Breed: " + (p.getBreed() != null ? p.getBreed().getName() : "-"));
        lines.add("Gender: " + p.getGender() + "   DOB: " + (p.getDateOfBirth() != null ? p.getDateOfBirth() : "-"));
        lines.add("Current Weight: " + (p.getWeight() != null ? p.getWeight() + " kg" : "-"));
        lines.add("");

        lines.add("MEDICAL HISTORY");
        if (history.isEmpty()) {
            lines.add("No medical records available.");
        } else {
            for (MedicalRecordDTO r : history) {
                lines.add(r.getRecordDate() + " | " + safe(r.getRecordType()) + " | " + safe(r.getDiagnosis()));
                lines.add("Veterinarian: " + safe(r.getVeterinarianName()));
                lines.add("Treatment: " + safe(r.getTreatment()));
                if (r.getPrescription() != null && !r.getPrescription().isBlank()) lines.add("Prescription: " + safe(r.getPrescription()));
                if (r.getWeightAtVisit() != null) lines.add("Weight at Visit: " + r.getWeightAtVisit() + " kg");
                if (r.getFollowUpDate() != null) lines.add("Follow-up: " + r.getFollowUpDate());
                if (r.getAppointmentId() != null) lines.add("Appointment: #" + r.getAppointmentId());
                lines.add("");
            }
        }

        lines.add("VACCINATION HISTORY");
        if (vaccineHistory.isEmpty()) {
            lines.add("No vaccination records available.");
        } else {
            for (Vaccination v : vaccineHistory) {
                lines.add(v.getDateAdministered() + " | " + safe(v.getVaccineName()));
                if (v.getNextDueDate() != null) lines.add("Next due: " + v.getNextDueDate());
                if (v.getAdministeredBy() != null) lines.add("Administered by: " + safe(v.getAdministeredBy().getFullName()));
                lines.add("");
            }
        }

        return simplePdf(lines);
    }

    private String safe(String s) {
        return s == null ? "-" : s.replaceAll("[\\r\\n]+", " ");
    }

    private byte[] simplePdf(List<String> raw) {
        List<String> lines = new ArrayList<>();
        for (String line : raw) wrap(line, 92, lines);
        if (lines.isEmpty()) lines.add("");

        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += LINES_PER_PAGE) {
            pages.add(new ArrayList<>(lines.subList(i, Math.min(i + LINES_PER_PAGE, lines.size()))));
        }

        int pageCount = pages.size();
        int firstPageObj = 3;
        int firstContentObj = firstPageObj + pageCount;
        int fontObj = firstContentObj + pageCount;
        int objectCount = fontObj;

        List<byte[]> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) kids.append(firstPageObj + i).append(" 0 R ");
        objects.add(("<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>").getBytes(StandardCharsets.ISO_8859_1));

        for (int i = 0; i < pageCount; i++) {
            int contentObj = firstContentObj + i;
            String page = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] "
                    + "/Resources << /Font << /F1 " + fontObj + " 0 R >> >> /Contents " + contentObj + " 0 R >>";
            objects.add(page.getBytes(StandardCharsets.ISO_8859_1));
        }

        for (List<String> pageLines : pages) {
            StringBuilder content = new StringBuilder("BT\n/F1 10 Tf\n50 790 Td\n14 TL\n");
            for (String line : pageLines) content.append('(').append(pdfEscape(line)).append(") Tj\nT*\n");
            content.append("ET\n");
            byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);
            ByteArrayOutputStream contentObject = new ByteArrayOutputStream();
            try {
                contentObject.write(("<< /Length " + stream.length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
                contentObject.write(stream);
                contentObject.write("endstream".getBytes(StandardCharsets.ISO_8859_1));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            objects.add(contentObject.toByteArray());
        }

        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>".getBytes(StandardCharsets.ISO_8859_1));
        if (objects.size() != objectCount) throw new IllegalStateException("PDF object count mismatch");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);
            for (int i = 0; i < objects.size(); i++) {
                offsets.add(out.size());
                out.write(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
                out.write(objects.get(i));
                out.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
            }
            int xref = out.size();
            out.write(("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n").getBytes(StandardCharsets.ISO_8859_1));
            for (int i = 1; i < offsets.size(); i++) {
                out.write(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.ISO_8859_1));
            }
            out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF")
                    .getBytes(StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }

    private void wrap(String text, int width, List<String> out) {
        if (text == null) { out.add(""); return; }
        String s = text;
        while (s.length() > width) {
            int cut = s.lastIndexOf(' ', width);
            if (cut < 1) cut = width;
            out.add(s.substring(0, cut));
            s = s.substring(cut).trim();
        }
        out.add(s);
    }

    private String pdfEscape(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replaceAll("[^\\x20-\\x7E]", "?");
    }
}
