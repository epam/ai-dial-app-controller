package com.epam.aidial.controller;

import com.epam.aidial.dto.CreateImageRequestDto;
import com.epam.aidial.dto.CreateImageResponseDto;
import com.epam.aidial.dto.DeleteImageResponseDto;
import com.epam.aidial.service.BuildService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(ImageController.class)
class ImageControllerTest {
    private static final String TEST_NAME = "test-name";
    private static final String TEST_SOURCES = "test-sources";
    private static final String TEST_RUNTIME = "test-runtime";
    private static final String TEST_IMAGE = "test-image";
    private static final String TEST_JWT = "test-api-jwt";
    private static final String TEST_API_KEY = "test-api-key";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private BuildService buildService;

    @MockitoBean
    private HeartbeatService heartbeatService;

    @Captor
    private ArgumentCaptor<BuildService.BuildParameters> buildCaptor;

    @Captor
    private ArgumentCaptor<String> cleanCaptor;

    @Captor
    private ArgumentCaptor<Mono<ServerSentEvent<Object>>> setupHeartbeatsCaptor;

    @Test
    void testImageCreate() {
        // Arrange
        when(buildService.build(buildCaptor.capture()))
                .thenReturn(Mono.just(TEST_IMAGE));
        CreateImageResponseDto response = new CreateImageResponseDto(TEST_IMAGE);
        ServerSentEvent<Object> result = SseUtils.result(response);
        when(heartbeatService.setupHeartbeats(
                setupHeartbeatsCaptor.capture()))
                .thenReturn(Mono.just(result).flux());

        // Act
        Flux<CreateImageResponseDto> actual = webTestClient.post()
                .uri("/v1/image/" + TEST_NAME)
                .header("api-key", TEST_API_KEY)
                .header("Authorization", "Bearer " + TEST_JWT)
                .body(BodyInserters.fromValue(new CreateImageRequestDto(TEST_SOURCES, TEST_RUNTIME)))
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(CreateImageResponseDto.class)
                .getResponseBody();

        // Assert
        StepVerifier.create(actual)
                .expectNext(response)
                .verifyComplete();

        StepVerifier.create(setupHeartbeatsCaptor.getValue())
                .expectNext(result);

        assertThat(buildCaptor.getValue())
                .isEqualTo(new BuildService.BuildParameters(TEST_NAME, TEST_SOURCES, TEST_API_KEY, TEST_JWT, TEST_RUNTIME));
    }

    @Test
    void testImageDelete() {
        // Arrange
        when(buildService.clean(
                cleanCaptor.capture()))
                .thenReturn(Mono.just(true));
        DeleteImageResponseDto response = new DeleteImageResponseDto(true);
        ServerSentEvent<Object> result = SseUtils.result(response);
        when(heartbeatService.setupHeartbeats(
                setupHeartbeatsCaptor.capture()))
                .thenReturn(Mono.just(result).flux());

        // Act
        Flux<DeleteImageResponseDto> actual = webTestClient.delete()
                .uri("/v1/image/" + TEST_NAME)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(DeleteImageResponseDto.class)
                .getResponseBody();

        // Assert
        StepVerifier.create(actual)
                .expectNext(response)
                .verifyComplete();

        StepVerifier.create(setupHeartbeatsCaptor.getValue())
                .expectNext(result);

        verify(buildService).clean(TEST_NAME);
    }
}