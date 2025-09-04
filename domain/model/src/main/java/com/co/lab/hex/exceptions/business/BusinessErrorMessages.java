package com.co.lab.hex.exceptions.business;

import com.co.lab.hex.exceptions.Reason;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessErrorMessages {
    INSURER_NOT_FOUND("PRB-001", "Insurer not found", Reason.NOT_FOUND, "Resource Not Found"),
    INSURER_NOT_ACTIVE("PRB-002", "Insurer not active", Reason.UNPROCESSABLE, "Business Rule Violation"),
    INVALID_BIRTH_DATE("PRB-003", "Invalid birth date", Reason.INVALID_INPUT, "Invalid Input"),
    DUPLICATE_PATIENT("PRB-004", "Patient already exists for DNI", Reason.CONFLICT, "Conflict"),
    PATIENT_NOT_FOUND("PRB-005", "Patient not found for DNI", Reason.NOT_FOUND, "Resource Not Found"),
    INVALID_INPUT("PRB-006", "Invalid input", Reason.INVALID_INPUT, "Invalid Input");

    private final String code;      // p. ej. PRB-001
    private final String message;   // mensaje por defecto (dominio)
    private final Reason reason;    // categoría agnóstica
    private final String title;     // título semántico
}