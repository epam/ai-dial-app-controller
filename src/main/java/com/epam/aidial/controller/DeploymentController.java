package com.epam.aidial.controller;

import com.epam.aidial.dto.CreateDeploymentRequestDto;
import com.epam.aidial.dto.CreateDeploymentResponseDto;
import com.epam.aidial.dto.DeleteImageResponseDto;
import com.epam.aidial.dto.GetApplicationLogsResponseDto;
import com.epam.aidial.service.DeployService;
import com.epam.aidial.service.HeartbeatService;
import com.epam.aidial.util.SseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/v1/deployment")
@RequiredArgsConstructor
public class DeploymentController {
    private final DeployService deployService;
    private final HeartbeatService heartbeatService;

    @PostMapping(value = "{name}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> create(
            @PathVariable("name") String name,
            @RequestBody CreateDeploymentRequestDto request) {
        Mono<CreateDeploymentResponseDto> result = deployService.deploy(name, Objects.requireNonNullElse(request.env(), Map.of()))
                .doOnError(e -> log.error("Failed to deploy service {}", name, e))
                .map(CreateDeploymentResponseDto::new);

        return heartbeatService.setupHeartbeats(SseUtils.mapToSseEvent(result));
    }

    @DeleteMapping(value = "{name}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> delete(@PathVariable("name") String name) {
        Mono<DeleteImageResponseDto> result = deployService.undeploy(name)
                .doOnError(e -> log.error("Failed to delete service {}", name, e))
                .map(DeleteImageResponseDto::new);

        return heartbeatService.setupHeartbeats(SseUtils.mapToSseEvent(result));
    }

    @GetMapping(value = "{name}/logs")
    public Mono<GetApplicationLogsResponseDto> logs(@PathVariable("name") String name) {
        return deployService.logs(name)
                .map(GetApplicationLogsResponseDto::new)
                .doOnError(e -> log.error("Failed to retrieve logs for {}", name, e));
    }
}
