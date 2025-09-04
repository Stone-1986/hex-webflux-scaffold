package com.co.lab.hex.errorHandler;

import com.co.lab.hex.exceptions.DomainException;
import com.co.lab.hex.exceptions.Reason;
import com.co.lab.hex.exceptions.WithErrors;
import com.co.lab.hex.exceptions.error.ErrorMessage;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.codec.CodecException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.*;

import java.time.Instant;
import java.util.*;

@Component
public class ProblemDetailsMapper {

    private static final String KEY_TYPE = "type";
    private static final String KEY_TITLE = "title";
    private static final String KEY_STATUS = "status";
    private static final String KEY_DETAIL = "detail";
    private static final String KEY_INSTANCE = "instance";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_CORRELATION_ID = "correlationId";
    private static final String KEY_CODE = "code";
    private static final String KEY_ERRORS = "errors";
    private static final String KEY_FIELD = "field";
    private static final String KEY_OBJECT = "object";
    private static final String KEY_MESSAGE = "message";

    public Map<String, Object> toProblemBody(Throwable ex, String path, String correlationId) {
        int status = httpStatus(ex);
        HttpStatus hs = HttpStatus.valueOf(status);

        String type = "about:blank";
        String title = titleFor(ex, hs);
        String detail = sanitizedDetailFor(ex, status);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(KEY_TYPE, type);
        body.put(KEY_TITLE, title);
        body.put(KEY_STATUS, status);
        body.put(KEY_DETAIL, detail);
        body.put(KEY_INSTANCE, path);
        body.put(KEY_TIMESTAMP, Instant.now().toString());
        body.put(KEY_CORRELATION_ID, correlationId);

        if (ex instanceof DomainException de) {
            body.put(KEY_CODE, de.getCode());
        }

        if (ex instanceof WebExchangeBindException bind) {
            body.put(KEY_CODE, "VALIDATION_ERROR");
            var fieldErrors = bind.getFieldErrors().stream()
                    .map(f -> Map.<String, Object>of(
                            KEY_FIELD, f.getField(),
                            KEY_MESSAGE, Optional.ofNullable(f.getDefaultMessage()).orElse("Invalid value")))
                    .toList();
            var globalErrors = bind.getGlobalErrors().stream()
                    .map(g -> Map.<String, Object>of(
                            KEY_OBJECT, g.getObjectName(),
                            KEY_MESSAGE, Optional.ofNullable(g.getDefaultMessage()).orElse("Invalid value")))
                    .toList();
            var combined = new ArrayList<Map<String, Object>>(fieldErrors.size() + globalErrors.size());
            combined.addAll(fieldErrors);
            combined.addAll(globalErrors);
            if (!combined.isEmpty()) body.put(KEY_ERRORS, combined);

        } else if (ex instanceof ConstraintViolationException cve) {
            body.put(KEY_CODE, "VALIDATION_ERROR");
            var violations = cve.getConstraintViolations().stream()
                    .map(v -> Map.<String, Object>of(
                            KEY_FIELD, String.valueOf(v.getPropertyPath()),
                            KEY_MESSAGE, v.getMessage()))
                    .toList();
            if (!violations.isEmpty()) body.put(KEY_ERRORS, violations);

        } else if (ex instanceof WithErrors withErrors) {
            var errors = mapDomainErrors(withErrors.getErrors());
            if (!errors.isEmpty()) body.put(KEY_ERRORS, errors);

        } else if (ex instanceof ServerWebInputException) {
            body.put(KEY_CODE, "INVALID_INPUT");
        }

        return body;
    }

    public int httpStatus(Throwable ex) {
        if (ex instanceof DomainException de) {
            return httpStatusFromReason(de.getReason());
        }
        if (ex instanceof WebClientResponseException wcre) {
            return wcre.getStatusCode().value();
        }
        if (ex instanceof ConstraintViolationException
                || ex instanceof ServerWebInputException
                || ex instanceof CodecException
        ) {
            return 400;
        }
        if (ex instanceof MethodNotAllowedException) return 405;
        if (ex instanceof UnsupportedMediaTypeStatusException) return 415;
        if (ex instanceof ResponseStatusException rse) return rse.getStatusCode().value();

        if (ex instanceof java.util.concurrent.TimeoutException
                || ex instanceof io.netty.handler.timeout.ReadTimeoutException
                || ex instanceof io.netty.handler.timeout.WriteTimeoutException) {
            return 504;
        }
        if (ex instanceof CallNotPermittedException) {
            return 503;
        }
        return 500;
    }

    /**
     * Traduce un {@link Reason} (agnóstico del transporte) a un código HTTP.
     *
     * <p>Este método es la única pieza que conecta la semántica del dominio
     * con la representación HTTP. El dominio lanza excepciones con un
     * {@link Reason}, y aquí se decide cuál {@code status} devolver al cliente.
     *
     * <ul>
     *   <li>{@link Reason#NOT_FOUND} → 404 (Resource Not Found)</li>
     *   <li>{@link Reason#INVALID_INPUT} → 400 (Bad Request)</li>
     *   <li>{@link Reason#CONFLICT} → 409 (Conflict)</li>
     *   <li>{@link Reason#UNPROCESSABLE} → 422 (Unprocessable Entity, regla de negocio)</li>
     *   <li>{@link Reason#SERVICE_UNAVAILABLE} → 503 (Dependencia caída / circuito abierto)</li>
     *   <li>{@link Reason#INTERNAL_ERROR} → 500 (Fallo inesperado del sistema)</li>
     * </ul>
     *
     * <p>Ventaja: el dominio no conoce HTTP. Si mañana cambiamos a gRPC o mensajes,
     * este método puede tener otra implementación de mapeo sin tocar el core de negocio.
     */
    private int httpStatusFromReason(Reason r) {
        return switch (r) {
            case NOT_FOUND           -> 404;  // Cuando el recurso solicitado no existe.
            case INVALID_INPUT       -> 400;  // Es para errores de sintaxis o estructura en la petición
            case CONFLICT            -> 409;  // Cuando la solicitud no puede completarse por un estado conflictivo en el servidor. ej. Intentar crear un paciente con un DNI que ya existe
            case UNPROCESSABLE       -> 422;  // La petición está bien formada (no es 400), pero viola reglas de negoci
            case SERVICE_UNAVAILABLE -> 503;  //Cuando tu servicio no puede procesar la solicitud porque depende de algo externo que está caído o saturado.
            case INTERNAL_ERROR      -> 500;  //significa “algo salió mal en el servidor”, y se debe devolver un mensaje genérico (no detalles internos).
        };
    }

    private String titleFor(Throwable ex, HttpStatus hs) {
        if (ex instanceof DomainException de) return de.getTitle();
        if (ex instanceof WebExchangeBindException || ex instanceof ConstraintViolationException)
            return "Validation Failed";
        if (ex instanceof ServerWebInputException) return "Invalid Input";
        return hs.getReasonPhrase();
    }

    private String sanitizedDetailFor(Throwable ex, int status) {
        if (ex instanceof ResponseStatusException rse) {
            String r = rse.getReason();
            if (StringUtils.hasText(r)) return r;
            return HttpStatus.valueOf(rse.getStatusCode().value()).getReasonPhrase();
        }
        if (status >= 500) return "An unexpected error occurred";
        String m = ex.getMessage();
        return (m == null || m.isBlank()) ? "Unexpected error" : m;
    }

    private List<Map<String, Object>> mapDomainErrors(List<?> rawErrors) {
        if (rawErrors == null || rawErrors.isEmpty()) return List.of();
        return rawErrors.stream().map(e -> {
            if (e instanceof ErrorMessage em) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put(KEY_CODE, em.code());
                m.put(KEY_MESSAGE, em.message());
                return m;
            }
            if (e instanceof Map<?, ?> map) {
                Map<String, Object> m2 = new LinkedHashMap<>();
                for (var entry : map.entrySet()) {
                    m2.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                return m2;
            }
            return Map.<String, Object>of(KEY_MESSAGE, String.valueOf(e));
        }).toList();
    }
}