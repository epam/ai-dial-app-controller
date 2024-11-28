package com.epam.aidial.service;

import com.epam.aidial.kubernetes.KubernetesClient;
import com.epam.aidial.util.KubernetesUtils;
import com.epam.aidial.util.TextUtils;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.epam.aidial.util.NamingUtils.buildJobName;
import static com.epam.aidial.util.NamingUtils.dialAuthSecretName;

@Service
@RequiredArgsConstructor
public class BuildService {
    private static final String APP_VALIDATION_ERROR_PREFIX = "AppValidationException: ";
    private final KubernetesService kubernetesService;
    private final ConfigService templateService;
    private final RegistryService registryService;

    @Value("${app.build-namespace}")
    private final String namespace;

    @Value("${app.max-error-log-lines}")
    private final int maxErrorLogLines;

    @Value("${app.max-error-log-chars}")
    private final int maxErrorLogChars;

    @Value("${app.image-build-timeout-sec}")
    private final int imageBuildTimeoutSec;

    public Mono<String> build(BuildParameters params) {
        KubernetesClient kubernetesClient = kubernetesService.buildClient();
        return Mono.fromCallable(() -> templateService.dialAuthSecretConfig(params.name, params.apiKey, params.jwt))
                .flatMap(secret -> kubernetesClient.createSecret(namespace, secret))
                .then(Mono.fromCallable(() -> templateService.buildJobConfig(params.name, params.sources, params.runtime)))
                .flatMap(job -> kubernetesClient.createJob(namespace, job, imageBuildTimeoutSec))
                .onErrorResume(e -> {
                    String jobName = buildJobName(params.name);

                    return kubernetesClient.getJobPods(namespace, jobName)
                            .flatMap(this::extractErrorFromLog)
                            .flatMap(error -> Mono.error(new RuntimeException(error)))
                            .then(Mono.error(e));
                })
                .thenReturn(registryService.fullImageName(params.name));
    }

    public Mono<Boolean> clean(String name) {
        KubernetesClient kubernetesClient = kubernetesService.buildClient();
        return Mono.just(Boolean.FALSE)
                .flatMap(deleted -> KubernetesUtils.skipIfNotFound(kubernetesClient.deleteJob(namespace, buildJobName(name)), deleted))
                .flatMap(deleted -> KubernetesUtils.skipIfNotFound(kubernetesClient.deleteSecret(namespace, dialAuthSecretName(name)), deleted))
                .flatMap(deleted -> registryService.getDigest(name)
                        .flatMap(digest -> registryService.deleteManifest(name, digest))
                        .map(d -> d || deleted)
                        .defaultIfEmpty(deleted));
    }

    private Mono<String> extractErrorFromLog(V1PodList podList) {
        KubernetesClient kubernetesClient = kubernetesService.buildClient();
        return Mono.fromCallable(() -> KubernetesUtils.extractFailedContainer(podList))
                .flatMap(container -> kubernetesClient.getContainerLog(
                        namespace, container.getKey(), container.getValue()))
                .map(text -> {
                    int validationErrorIndex = text.indexOf(APP_VALIDATION_ERROR_PREFIX);
                    return validationErrorIndex == StringUtils.INDEX_NOT_FOUND
                            ? "Failed to build image. Logs: %s".formatted(
                                    TextUtils.truncateText(text, maxErrorLogLines, maxErrorLogChars))
                            : "Validation error: %s".formatted(
                                    text.substring(validationErrorIndex + APP_VALIDATION_ERROR_PREFIX.length())).trim();

                });
    }

    public record BuildParameters(String name, String sources, String apiKey, String jwt, String runtime) {
    }
}
