package com.co.lab.hex.web.task;

import com.co.lab.hex.web.task.dto.CreateTaskRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Validated
public class TaskHandler {

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(CreateTaskRequest.class)
                .doOnNext(req -> { /* side effects if needed */ })
                .flatMap(req -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(req))
                ;
    }

    public Mono<ServerResponse> ping(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).bodyValue("pong");
    }
}
