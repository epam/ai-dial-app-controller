package com.epam.aidial.service;

import com.epam.aidial.kubernetes.knative.V1Service;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.Yaml;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("configtest")
class ConfigServiceTest {
    private static final String TEST_NAME = "test-name";
    private static final String TEST_IMAGE_NAME = "test-image-name";

    @Autowired
    private ConfigService configService;

    @MockitoBean
    private RegistryService registryService;

    @MockitoBean
    private ApiClient buildKubeClient;

    @MockitoBean
    private ApiClient deployKubeClient;

    @Captor
    private ArgumentCaptor<String> fullImageNameCaptor;

    @Test
    void testDialAuthSecretConfig() throws IOException {
        V1Secret expected = readExpected("dial-auth-secret", V1Secret.class);

        V1Secret actual = configService.dialAuthSecretConfig(TEST_NAME, "test-api-key", "test-jwt");

        assertThat(Yaml.dump(actual)).isEqualTo(Yaml.dump(expected));
    }

    @Test
    void testBuildJobConfig() throws IOException {
        // Arrange
        when(registryService.fullImageName(fullImageNameCaptor.capture()))
                .thenReturn(TEST_IMAGE_NAME);
        V1Job expected = readExpected("build-job", V1Job.class);

        // Act
        V1Job actual = configService.buildJobConfig(TEST_NAME, "test-sources", "python3.11");

        // Assert
        assertThat(Yaml.dump(actual)).isEqualTo(Yaml.dump(expected));
        assertThat(fullImageNameCaptor.getValue()).isEqualTo(TEST_NAME);
    }

    @Test
    void testAppServiceConfig() throws IOException {
        // Arrange
        when(registryService.fullImageName(fullImageNameCaptor.capture()))
                .thenReturn(TEST_IMAGE_NAME);
        V1Service expected = readExpected("app-service", V1Service.class);

        // Act
        V1Service actual = configService.appServiceConfig(TEST_NAME, Map.of("test-env-name", "test-env-value"));

        // Assert
        assertThat(Yaml.dump(actual)).isEqualTo(Yaml.dump(expected));
        assertThat(fullImageNameCaptor.getValue()).isEqualTo(TEST_NAME);
    }

    private static <T> T readExpected(String name, Class<T> clazz) throws IOException {
        return Yaml.loadAs(
                IOUtils.resourceToString("/expected-configs/" + name + ".yaml", StandardCharsets.UTF_8),
                clazz);
    }
}