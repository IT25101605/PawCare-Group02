package com.pawcare.demo.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PetDTO {
    private Long id;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Pet name is required")
    @Size(min=2,max=50, message="Pet name must be between 2 and 50 characters")
    @Pattern(regexp="^ *[\\p{L}]+(?: +[\\p{L}]+)* *$", message="Pet name may contain letters and spaces only")
    private String name;

    @Positive
    private Long speciesId;
    private String speciesName;
    @Size(max=100)
    private String customSpeciesName;

    @Positive
    private Long breedId;
    private String breedName;
    @Size(max=100)
    private String customBreedName;

    @PastOrPresent(message="Birth date cannot be in the future")
    private LocalDate dateOfBirth;
    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Gender is required")
    @Pattern(regexp="MALE|FEMALE|UNKNOWN",message="Choose a valid gender")
    private String gender;
    @DecimalMin("0.01") @DecimalMax("999.99") @Digits(integer=3,fraction=2)
    private BigDecimal weight;
    @Size(max=50, message="Color must be 50 characters or fewer")
    @Pattern(regexp="^$|^(?=.*[\\p{L}])[\\p{L} ]+$", message="Color must contain letters and spaces only")
    private String color;
    @Size(max=50)
    private String microchipId;
    @Pattern(regexp="/uploads/[a-f0-9-]+\\.png",message="Upload a valid pet photo")
    private String photo;
    @Size(max=2000)
    private String notes;
    private Long ownerId;
    private String ownerName;
    private Boolean isActive;
    private Boolean clearBreed;
    private Boolean clearDateOfBirth;
    private Boolean clearWeight;
}
