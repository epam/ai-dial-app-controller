package com.epam.aidial.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class HealthController {
    @GetMapping("/health")
    public Mono<Void> health() {
        log.trace("Health check requested");
        return Mono.empty();
    }
}
