package com.co.lab.hex.exceptions.technical;

import com.co.lab.hex.exceptions.Reason;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TechnicalErrorMessages {
    DEPENDENCY_UNAVAILABLE("PRT-001", "Insurer service unavailable", Reason.SERVICE_UNAVAILABLE, "Service Unavailable"),
    INTERNAL_ERROR("PRT-002", "Internal error", Reason.INTERNAL_ERROR, "Internal Server Error"),
    DATABASE_UNAVAILABLE("PRT-003", "Database service unavailable", Reason.SERVICE_UNAVAILABLE, "Service Unavailable"),
    FILE_GENERATION_ERROR("PRT-004", "File generation error", Reason.INTERNAL_ERROR, "Internal Server Error");

    private final String code;     // p. ej. PRT-001
    private final String message;  // mensaje por defecto (técnico)
    private final Reason reason;   // categoría agnóstica
    private final String title;    // título semántico
}