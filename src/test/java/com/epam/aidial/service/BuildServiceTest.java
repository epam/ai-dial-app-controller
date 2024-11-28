package com.epam.aidial.service;

import com.epam.aidial.kubernetes.KubernetesClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "app.build-namespace=" + BuildServiceTest.TEST_NAMESPACE,
        "app.docker-registry=" + BuildServiceTest.TEST_REGISTRY,
        "app.max-error-log-lines=5",
        "app.max-error-log-chars=15",
        "app.image-build-timeout-sec=5"
})
@Import(BuildService.class)
class BuildServiceTest {
    private static final String TEST_DIGEST = "test-digest";
    private static final String TEST_NAME = "test-name";
    private static final String TEST_IMAGE = "test-image";
    private static final String TEST_SOURCES = "test-sources";
    private static final String TEST_RUNTIME = "test-runtime";
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_JWT = "test-jwt";
    private static final V1Secret TEST_SECRET = new V1Secret().metadata(new V1ObjectMeta().name(TEST_NAME));
    private static final V1Job TEST_JOB = new V1Job().metadata(new V1ObjectMeta().name(TEST_NAME));

    static final String TEST_NAMESPACE = "test-namespace";
    static final String TEST_REGISTRY = "test-registry";

    @Autowired
    private BuildService buildService;

    @MockitoBean
    private KubernetesClient kubernetesClient;

    @MockitoBean
    private KubernetesService kubernetesService;

    @MockitoBean
    private ConfigService templateService;

    @MockitoBean
    private RegistryService registryService;

    @Captor
    private ArgumentCaptor<String> secretConfigCaptor;

    @Captor
    private ArgumentCaptor<String> jobConfigCaptor;

    @Captor
    private ArgumentCaptor<Object> createSecretCaptor;

    @Captor
    private ArgumentCaptor<Object> createJobCaptor;

    @Captor
    private ArgumentCaptor<String> deleteSecretCaptor;

    @Captor
    private ArgumentCaptor<String> deleteJobCaptor;

    @Captor
    private ArgumentCaptor<String> fullImageNameCaptor;

    @Captor
    private ArgumentCaptor<String> getDigestCaptor;

    @Captor
    private ArgumentCaptor<String> deleteManifestCaptor;

    @Test
    void testBuild() {
        // Arrange
        when(kubernetesService.buildClient()).thenReturn(kubernetesClient);
        when(templateService.dialAuthSecretConfig(
                secretConfigCaptor.capture(),
                secretConfigCaptor.capture(),
                secretConfigCaptor.capture()))
                .thenReturn(TEST_SECRET);
        when(kubernetesClient.createSecret(
                (String) createSecretCaptor.capture(),
                (V1Secret) createSecretCaptor.capture()))
                .thenReturn(Mono.empty());
        when(templateService.buildJobConfig(
                jobConfigCaptor.capture(),
                jobConfigCaptor.capture(),
                jobConfigCaptor.capture()))
                .thenReturn(TEST_JOB);
        when(kubernetesClient.createJob(
                (String) createJobCaptor.capture(),
                (V1Job) createJobCaptor.capture(),
                anyInt()))
                .thenReturn(Mono.empty());
        when(registryService.fullImageName(
                fullImageNameCaptor.capture()))
                .thenReturn(TEST_IMAGE);

        BuildService.BuildParameters buildParameters =
                new BuildService.BuildParameters(TEST_NAME, TEST_SOURCES, TEST_API_KEY, TEST_JWT, TEST_RUNTIME);

        // Act
        Mono<String> actual = buildService.build(buildParameters);

        // Assert
        StepVerifier.create(actual)
                .expectNext(TEST_IMAGE)
                .verifyComplete();

        assertThat(secretConfigCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAME, TEST_API_KEY, TEST_JWT));
        assertThat(createSecretCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAMESPACE, TEST_SECRET));
        assertThat(jobConfigCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAME, TEST_SOURCES, TEST_RUNTIME));
        assertThat(createJobCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAMESPACE, TEST_JOB));
        assertThat(fullImageNameCaptor.getValue())
                .isEqualTo(TEST_NAME);
    }

    @Test
    void testClean() {
        // Arrange
        when(kubernetesService.buildClient()).thenReturn(kubernetesClient);
        when(kubernetesClient.deleteJob(
                deleteJobCaptor.capture(),
                deleteJobCaptor.capture()))
                .thenReturn(Mono.empty());
        when(kubernetesClient.deleteSecret(
                deleteSecretCaptor.capture(),
                deleteSecretCaptor.capture()))
                .thenReturn(Mono.empty());
        when(registryService.getDigest(
                getDigestCaptor.capture()))
                .thenReturn(Mono.just(TEST_DIGEST));
        when(registryService.deleteManifest(
                deleteManifestCaptor.capture(),
                deleteManifestCaptor.capture()))
                .thenReturn(Mono.just(true));

        // Act
        Mono<Boolean> actual = buildService.clean(TEST_NAME);

        // Assert
        StepVerifier.create(actual)
                .expectNext(true)
                .verifyComplete();

        assertThat(deleteJobCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAMESPACE, "app-ctrl-build-test-name"));
        assertThat(deleteSecretCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAMESPACE, "app-ctrl-dial-auth-test-name"));
        assertThat(getDigestCaptor.getValue())
                .isEqualTo(TEST_NAME);
        assertThat(deleteManifestCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAME, TEST_DIGEST));
    }

    @Test
    void testCleanReturnsFalse() {
        // Arrange
        when(kubernetesService.buildClient()).thenReturn(kubernetesClient);
        when(kubernetesClient.deleteJob(
                deleteJobCaptor.capture(),
                deleteJobCaptor.capture()))
                .thenReturn(Mono.error(new ApiException(404, "Not found")));
        when(kubernetesClient.deleteSecret(
                deleteSecretCaptor.capture(),
                deleteSecretCaptor.capture()))
                .thenReturn(Mono.error(new ApiException(404, "Not found")));
        when(registryService.getDigest(
                getDigestCaptor.capture()))
                .thenReturn(Mono.empty());

        // Act
        Mono<Boolean> actual = buildService.clean(TEST_NAME);

        // Assert
        StepVerifier.create(actual)
                .expectNext(false)
                .verifyComplete();

        assertThat(deleteJobCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAMESPACE, "app-ctrl-build-test-name"));
        assertThat(deleteSecretCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAMESPACE, "app-ctrl-dial-auth-test-name"));
        assertThat(getDigestCaptor.getValue())
                .isEqualTo(TEST_NAME);
    }
}