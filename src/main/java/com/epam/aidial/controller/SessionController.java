package com.epam.aidial.controller;

import com.epam.aidial.dto.CreateSessionRequestDto;
import com.epam.aidial.dto.CreateSessionResponseDto;
import com.epam.aidial.dto.DeleteSessionResponseDto;
import com.epam.aidial.service.HeartbeatService;
import com.epam.aidial.service.SessionService;
import com.epam.aidial.util.SseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/v1/session")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;
    private final HeartbeatService heartbeatService;

    @PostMapping(value = "{name}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> create(
            @PathVariable("name") String name,
            @RequestBody CreateSessionRequestDto request) {
        String image = Objects.requireNonNull(request.image(), "missing image");
        Map<String, String> env = Objects.requireNonNullElse(request.env(), Map.of());
        Mono<CreateSessionResponseDto> result = sessionService.create(name, image, env)
                .doOnError(e -> log.error("Failed to create session: {}. Error: {}", name, e.getMessage()))
                .map(CreateSessionResponseDto::new);

        return heartbeatService.setupHeartbeats(SseUtils.mapToSseEvent(result));
    }

    @DeleteMapping(value = "{name}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> delete(@PathVariable("name") String name) {
        Mono<DeleteSessionResponseDto> result = sessionService.delete(name)
                .doOnError(e -> log.error("Failed to delete session: {}. Error: {}", name, e.getMessage()))
                .map(DeleteSessionResponseDto::new);

        return heartbeatService.setupHeartbeats(SseUtils.mapToSseEvent(result));
    }
}
