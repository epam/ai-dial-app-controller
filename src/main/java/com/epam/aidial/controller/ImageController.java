package com.epam.aidial.controller;

import com.epam.aidial.dto.CreateImageRequestDto;
import com.epam.aidial.dto.CreateImageResponseDto;
import com.epam.aidial.dto.DeleteImageResponseDto;
import com.epam.aidial.service.BuildService;
import com.epam.aidial.service.HeartbeatService;
import com.epam.aidial.util.SseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/v1/image")
@RequiredArgsConstructor
public class ImageController {
    private static final String AUTHORIZATION_PREFIX = "Bearer ";

    private final BuildService buildService;
    private final HeartbeatService heartbeatService;

    @Value("${app.default-runtime}")
    private final String pythonDefaultRuntime;

    @PostMapping(value = "{name}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> create(
            @Nullable
            @RequestHeader("api-key")
            String apiKey,
            @Nullable
            @RequestHeader("Authorization")
            String authorization,
            @PathVariable("name")
            String name,
            @RequestBody
            CreateImageRequestDto request) {
        String jwt = StringUtils.startsWithIgnoreCase(authorization, AUTHORIZATION_PREFIX)
                ? authorization.substring(AUTHORIZATION_PREFIX.length()).trim()
                : null;

        String runtime = Objects.requireNonNullElse(request.runtime(), pythonDefaultRuntime);
        BuildService.BuildParameters buildParameters = new BuildService.BuildParameters(
                name, request.sources(), apiKey, jwt, runtime);
        Mono<CreateImageResponseDto> result = buildService.build(buildParameters)
                .doOnError(e -> log.error("Failed to create image {}", name, e))
                .map(CreateImageResponseDto::new);

        return heartbeatService.setupHeartbeats(SseUtils.mapToSseEvent(result));
    }

    @DeleteMapping(value = "{name}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> delete(@PathVariable("name") String name) {
        Mono<DeleteImageResponseDto> result = buildService.clean(name)
                .doOnError(e -> log.error("Failed to delete image {}", name, e))
                .map(DeleteImageResponseDto::new);

        return heartbeatService.setupHeartbeats(SseUtils.mapToSseEvent(result));
    }
}
