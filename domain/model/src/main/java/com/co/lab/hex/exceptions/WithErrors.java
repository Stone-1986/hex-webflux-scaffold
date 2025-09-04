package com.co.lab.hex.exceptions;

import com.co.lab.hex.exceptions.error.ErrorMessage;

import java.util.List;

public interface WithErrors {
    // Lista opcional de errores detallados
    List<ErrorMessage> getErrors();
}
