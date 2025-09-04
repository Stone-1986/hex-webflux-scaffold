package com.co.lab.hex.exceptions.error;

/** Helpers estáticos para construir mensajes de error. */
public final class Errors {
    private Errors() {}

    public static ErrorMessage e(String code, String message) {
        return new ErrorMessage(code, message);
    }
}
