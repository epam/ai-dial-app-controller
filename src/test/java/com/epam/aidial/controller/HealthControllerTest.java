package com.epam.aidial.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(HealthController.class)
class HealthControllerTest {
    @Autowired
    private WebTestClient webClient;

    @Test
    void testHealth() {
        webClient.get()
                .uri("/health")
                .exchange()
                .expectStatus()
                .isOk();
    }
}