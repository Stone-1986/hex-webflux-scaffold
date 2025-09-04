package com.co.lab.hex.errorHandler;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Configuration
public class GlobalErrorWebExceptionHandlerConfig {

    @Bean
    public WebExceptionHandler problemHandler(ErrorAttributes errorAttributes,
                                              ApplicationContext applicationContext,
                                              ObjectProvider<ViewResolver> viewResolversProvider,
                                              ServerCodecConfigurer codecs) {

        var resourceProps = new WebProperties.Resources();

        return new AbstractErrorWebExceptionHandler(errorAttributes, resourceProps, applicationContext) {
            {
                List<ViewResolver> viewResolvers = viewResolversProvider.orderedStream().toList();
                setViewResolvers(viewResolvers);
                setMessageReaders(codecs.getReaders());
                setMessageWriters(codecs.getWriters());
            }

            @Override
            protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes ea) {
                return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
            }

            private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
                Map<String, Object> props = getErrorAttributes(request, ErrorAttributeOptions.defaults());
                int status = (int) props.getOrDefault("status", 500);
                return ServerResponse.status(status)
                        .contentType(MediaType.valueOf("application/problem+json"))
                        .bodyValue(props);
            }
        };
    }
}
