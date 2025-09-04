package com.co.lab.hex.exceptions;

/**
 * Motivos agnósticos del protocolo/transporte.
 * La infraestructura decidirá cómo mapearlos (HTTP, gRPC, etc.).
 */
public enum Reason {
    NOT_FOUND,
    INVALID_INPUT,
    CONFLICT,
    UNPROCESSABLE,
    SERVICE_UNAVAILABLE,
    INTERNAL_ERROR
}
