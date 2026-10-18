package com.pawcare.demo.dto;

import jakarta.validation.constraints.*;

public record ReviewRequestDTO(
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank @Size(min = 2, max = 2000) String review
) {}
