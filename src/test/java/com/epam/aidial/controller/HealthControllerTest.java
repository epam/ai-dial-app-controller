package com.epam.aidial.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
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