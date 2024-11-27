package com.epam.aidial.service;

import com.epam.aidial.kubernetes.knative.V1Service;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "app.deploy-namespace=" + DeployServiceTest.TEST_NAMESPACE
})
@Import(DeployService.class)
class DeployServiceTest {
    private static final Map<String, String> TEST_ENV = Map.of("test-env-name", "test-env-value");
    private static final String TEST_NAME = "test-name";
    private static final String TEST_URL = "url";

    static final String TEST_NAMESPACE = "test-namespace";

    @Autowired
    private DeployService deployService;

    @MockBean
    private KubernetesService kubernetesService;

    @MockBean
    private ConfigService templateService;

    @Captor
    private ArgumentCaptor<Object> appServiceConfigCaptor;

    @Captor
    private ArgumentCaptor<Object> createServiceCaptor;

    @Captor
    private ArgumentCaptor<String> deleteServiceCaptor;

    @Test
    @SuppressWarnings("unchecked")
    void testDeploy() throws IOException {
        // Arrange
        V1Service testService = new V1Service();
        testService.setMetadata(new V1ObjectMeta().name(TEST_NAME));
        when(templateService.appServiceConfig(
                (String) appServiceConfigCaptor.capture(),
                (Map<String, String>) appServiceConfigCaptor.capture()))
                .thenReturn(testService);
        when(kubernetesService.createKnativeService(
                (String) createServiceCaptor.capture(),
                (V1Service) createServiceCaptor.capture()))
                .thenReturn(Mono.just(TEST_URL));

        // Act
        Mono<String> actual = deployService.deploy(TEST_NAME, TEST_ENV);

        // Assert
        StepVerifier.create(actual)
                .expectNext(TEST_URL)
                .verifyComplete();

        assertThat(appServiceConfigCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAME, TEST_ENV));
        assertThat(createServiceCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAMESPACE, testService));
    }

    @Test
    void testUndeploy() {
        // Arrange
        when(kubernetesService.deleteKnativeService(
                deleteServiceCaptor.capture(),
                deleteServiceCaptor.capture()))
                .thenReturn(Mono.empty());

        // Act
        Mono<Boolean> actual = deployService.undeploy(TEST_NAME);

        // Assert
        StepVerifier.create(actual)
                .expectNext(true)
                .verifyComplete();

        assertThat(deleteServiceCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAMESPACE, "app-ctrl-app-test-name"));
    }

    @Test
    void testUndeployReturnsFalse() {
        // Arrange
        when(kubernetesService.deleteKnativeService(
                deleteServiceCaptor.capture(),
                deleteServiceCaptor.capture()))
                .thenReturn(Mono.error(new ApiException(404, "Not found")));

        // Act
        Mono<Boolean> actual = deployService.undeploy(TEST_NAME);

        // Assert
        StepVerifier.create(actual)
                .expectNext(false)
                .verifyComplete();

        assertThat(deleteServiceCaptor.getAllValues())
                .isEqualTo(List.of(TEST_NAMESPACE, "app-ctrl-app-test-name"));
    }
}