package com.co.lab.hex.errorHandler;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {
    public static final String HDR = "X-Correlation-Id";

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange,
                                      @NonNull WebFilterChain chain) {
        String cid = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(HDR))
                .orElseGet(() -> UUID.randomUUID().toString());

        var mutatedRequest = exchange.getRequest().mutate().header(HDR, cid).build();
        exchange.getResponse().getHeaders().set(HDR, cid);

        var mutatedExchange = exchange.mutate().request(mutatedRequest).build();
        mutatedExchange.getAttributes().put(HDR, cid);

        return chain.filter(mutatedExchange).contextWrite(ctx -> ctx.put(HDR, cid));
    }
}