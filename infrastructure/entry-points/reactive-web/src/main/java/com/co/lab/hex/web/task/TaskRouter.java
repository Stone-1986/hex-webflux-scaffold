package com.co.lab.hex.web.task;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class TaskRouter {
    @Bean
    RouterFunction<ServerResponse> routes(TaskHandler h) {
        return RouterFunctions.route()
                .GET("/ping", h::ping)
                .POST("/api/tasks", h::create)
                .build();
    }
}
