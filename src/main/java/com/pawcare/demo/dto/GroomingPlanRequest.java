package com.pawcare.demo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record GroomingPlanRequest(
        @NotNull @Positive Long petId,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000") BigDecimal budget,
        @Min(15) @Max(180) int maxMinutes,
        @NotNull Goal goal,
        @NotNull Comfort comfort) {
    public enum Goal { FRESH_COAT, QUICK_TIDY, FULL_STYLE }
    public enum Comfort { CALM, FIRST_VISIT, NERVOUS }
}
