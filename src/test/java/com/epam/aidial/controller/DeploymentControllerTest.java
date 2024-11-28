package com.epam.aidial.controller;

import com.epam.aidial.dto.CreateDeploymentRequestDto;
import com.epam.aidial.dto.CreateDeploymentResponseDto;
import com.epam.aidial.dto.DeleteDeploymentResponseDto;
import com.epam.aidial.service.DeployService;
import com.epam.aidial.service.HeartbeatService;
import com.epam.aidial.util.SseUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(DeploymentController.class)
class DeploymentControllerTest {
    private static final String TEST_NAME = "test-name";
    private static final String TEST_URL = "test-url";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DeployService deployService;

    @MockitoBean
    private HeartbeatService heartbeatService;

    @Captor
    private ArgumentCaptor<Object> deployCaptor;

    @Captor
    private ArgumentCaptor<String> undeployCaptor;

    @Captor
    private ArgumentCaptor<Mono<ServerSentEvent<Object>>> setupHeartbeatsCaptor;

    @Test
    @SuppressWarnings("unchecked")
    void testDeploymentCreate() {
        // Arrange
        when(deployService.deploy(
                (String) deployCaptor.capture(),
                (Map<String, String>) deployCaptor.capture()))
                .thenReturn(Mono.just(TEST_URL));
        CreateDeploymentResponseDto response = new CreateDeploymentResponseDto(TEST_URL);
        ServerSentEvent<Object> result = SseUtils.result(response);
        when(heartbeatService.setupHeartbeats(
                setupHeartbeatsCaptor.capture()))
                .thenReturn(Mono.just(result).flux());
        Map<String, String> env = Map.of("test-env-name", "test-env-value");

        // Act
        Flux<CreateDeploymentResponseDto> actual = webTestClient.post()
                .uri("/v1/deployment/" + TEST_NAME)
                .body(BodyInserters.fromValue(new CreateDeploymentRequestDto(env)))
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(CreateDeploymentResponseDto.class)
                .getResponseBody();

        // Assert
        StepVerifier.create(actual)
                .expectNext(response)
                .verifyComplete();

        StepVerifier.create(setupHeartbeatsCaptor.getValue())
                .expectNext(result);

        assertThat(deployCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAME, env));
    }

    @Test
    void testDeploymentDelete() {
        // Arrange
        when(deployService.undeploy(
                undeployCaptor.capture()))
                .thenReturn(Mono.just(true));
        DeleteDeploymentResponseDto response = new DeleteDeploymentResponseDto(true);
        ServerSentEvent<Object> result = SseUtils.result(response);
        when(heartbeatService.setupHeartbeats(
                setupHeartbeatsCaptor.capture()))
                .thenReturn(Mono.just(result).flux());

        // Act
        Flux<DeleteDeploymentResponseDto> actual = webTestClient.delete()
                .uri("/v1/deployment/" + TEST_NAME)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(DeleteDeploymentResponseDto.class)
                .getResponseBody();

        // Assert
        StepVerifier.create(actual)
                .expectNext(response)
                .verifyComplete();

        StepVerifier.create(setupHeartbeatsCaptor.getValue())
                .expectNext(result);

        verify(deployService).undeploy(TEST_NAME);
    }
}