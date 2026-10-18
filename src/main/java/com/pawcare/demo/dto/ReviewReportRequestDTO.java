package com.pawcare.demo.dto;

import jakarta.validation.constraints.*;

public record ReviewReportRequestDTO(
        @NotBlank @Size(min = 2, max = 500) String reason
) {}
