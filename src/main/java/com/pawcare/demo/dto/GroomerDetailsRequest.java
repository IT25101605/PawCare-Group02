package com.pawcare.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Editable professional facts for the signed-in groomer or an administrator. */
public record GroomerDetailsRequest(
        @Size(max=150) String organization,
        @Size(max=200) String qualification,
        @Size(max=150) String specialty,
        @Min(0) @Max(60) Integer experienceYears) {}
