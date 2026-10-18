package com.pawcare.demo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record GroomingServiceRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 5000) String description,
        @NotNull @Min(1) @Max(480) Integer durationMinutes,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull Boolean isActive,
        Boolean removeImage) {}
