package com.co.lab.hex.exceptions.business;

import com.co.lab.hex.exceptions.DomainException;
import com.co.lab.hex.exceptions.Reason;
import com.co.lab.hex.exceptions.WithErrors;
import com.co.lab.hex.exceptions.error.ErrorMessage;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class CustomBusinessException extends RuntimeException implements DomainException, WithErrors {

    private final String code;
    private final String title;
    private final Reason reason;
    private final transient List<ErrorMessage> errors;

    public CustomBusinessException(BusinessErrorMessages em, List<ErrorMessage> errors) {
        super(em.getMessage());
        this.code = em.getCode();
        this.title = em.getTitle();
        this.reason = em.getReason();
        this.errors = errors == null ? Collections.emptyList() : errors;
    }

    public CustomBusinessException(BusinessErrorMessages em) {
        this(em, Collections.emptyList());
    }

    /** Ctor libre para casos dinámicos (no catalogados). */
    public CustomBusinessException(
            String code,
            String message,
            String title,
            Reason reason,
            List<ErrorMessage> errors
    ) {
        super(message);
        this.code = code;
        this.title = title;
        this.reason = reason;
        this.errors = errors == null ? Collections.emptyList() : errors;
    }
}