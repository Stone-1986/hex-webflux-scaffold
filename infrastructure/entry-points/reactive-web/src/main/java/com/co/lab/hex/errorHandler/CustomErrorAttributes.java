package com.co.lab.hex.errorHandler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebInputException;

import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class CustomErrorAttributes extends DefaultErrorAttributes {

    private static final boolean BLOCKHOUND_ON =
            ClassUtils.isPresent("reactor.blockhound.BlockingOperationError",
                    CustomErrorAttributes.class.getClassLoader());
    private static final Class<?> BLOCKHOUND_CLS =
            BLOCKHOUND_ON
                    ? ClassUtils.resolveClassName("reactor.blockhound.BlockingOperationError",
                    CustomErrorAttributes.class.getClassLoader())
                    : null;

    private final ProblemDetailsMapper mapper;

    public CustomErrorAttributes(ProblemDetailsMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Throwable error = unwrapBlockHoundIfNeeded(getError(request));
        String cid = resolveCid(request);

        Map<String, Object> body = mapper.toProblemBody(error, request.path(), cid);

        int status = (int) body.getOrDefault("status", 500);
        String msg = "Handled error. status={}, type={}, code={}, path={}, cid={}";
        if (status >= 500) {
            log.error(msg, body.get("status"), body.get("type"), body.get("code"), body.get("instance"), cid, error);
        } else {
            log.warn(msg, body.get("status"), body.get("type"), body.get("code"), body.get("instance"), cid);
        }
        return body;
    }

    private Throwable unwrapBlockHoundIfNeeded(Throwable error) {
        if (!BLOCKHOUND_ON || error == null) return error;
        if (BLOCKHOUND_CLS.isInstance(error)) {
            Throwable cause = error.getCause();
            if (cause instanceof ConstraintViolationException || cause instanceof WebExchangeBindException) return cause;
            return new ServerWebInputException("Validation error");
        }
        return error;
    }

    private String resolveCid(ServerRequest request) {
        final String HDR = CorrelationIdFilter.HDR;
        return Optional.ofNullable(request.headers().firstHeader(HDR))
                .or(() -> request.attribute(HDR).map(Object::toString))
                .or(() -> Optional.ofNullable(request.exchange().getAttribute(HDR)))
                .or(() -> Optional.ofNullable(request.exchange().getResponse().getHeaders().getFirst(HDR)))
                .orElse(null);
    }
}
