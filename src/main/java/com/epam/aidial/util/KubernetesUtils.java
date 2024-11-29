package com.epam.aidial.util;

import com.epam.aidial.kubernetes.knative.V1Condition;
import com.epam.aidial.kubernetes.knative.V1Service;
import com.epam.aidial.kubernetes.knative.V1ServiceStatus;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobCondition;
import io.kubernetes.client.openapi.models.V1JobStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class KubernetesUtils {
    public boolean extractJobCompletionStatus(V1Job job) {
        String name = job.getMetadata().getName();
        V1JobStatus status = job.getStatus();
        if (status != null && status.getConditions() != null) {
            for (V1JobCondition condition : status.getConditions()) {
                // From documentation (https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.30/#jobstatus-v1-batch):
                // When a Job fails, one of the conditions will have type "Failed" and status true.
                // When a Job is suspended, one of the conditions will have type "Suspended" and status true;
                // when the Job is resumed, the status of this condition will become false.
                // When a Job is completed, one of the conditions will have type "Complete" and status true.
                if ("True".equals(condition.getStatus())) {
                    if ("Complete".equals(condition.getType())) {
                        return true;
                    }

                    if ("Failed".equals(condition.getType())) {
                        throw new IllegalStateException("Job %s has failed: %s".formatted(name, condition.getMessage()));
                    }
                }

                if (condition.getMessage() == null) {
                    log.info("Job: {}, status: {}", name, condition.getType());
                } else {
                    log.info("Job: {}, status: {}, reason: {}, message: {}",
                            name, condition.getType(), condition.getReason(), condition.getMessage());
                }
            }
        }

        return false;
    }

    public String extractServiceUrl(V1Service service) {
        String name = service.getMetadata().getName();
        V1ServiceStatus status = service.getStatus();
        if (status != null && status.getConditions() != null) {
            for (V1Condition condition : status.getConditions()) {
                if ("Ready".equals(condition.getType())) {
                    if ("True".equals(condition.getStatus())) {
                        if (StringUtils.isBlank(status.getUrl())) {
                            throw new IllegalStateException("Empty service URL.");
                        }

                        return status.getUrl();
                    }

                    if ("False".equals(condition.getStatus())) {
                        throw new IllegalStateException("Failed to setup service %s: %s".formatted(name, condition.getMessage()));
                    }
                }

                if (condition.getMessage() == null) {
                    log.info("Service: {}, status: {}", name, condition.getType());
                } else {
                    log.info("Service: {}, status: {}, reason: {}, message: {}",
                            name, condition.getType(), condition.getReason(), condition.getMessage());
                }
            }
        }

        return null;
    }

    public Pair<String, String> extractFailedContainer(V1PodList podList) {
        for (V1Pod pod : podList.getItems()) {
            if ("Failed".equals(pod.getStatus().getPhase())) {
                String podName = pod.getMetadata().getName();
                String containerName = findFailedContainerName(pod.getStatus())
                        .orElseThrow(() -> new IllegalStateException(
                                "Couldn't find a terminated container in pod %s".formatted(podName)));
                return Pair.of(podName, containerName);
            }
        }

        return null;
    }

    private Optional<String> findFailedContainerName(V1PodStatus podStatus) {
        return Stream.of(
                        podStatus.getInitContainerStatuses(),
                        podStatus.getContainerStatuses(),
                        podStatus.getEphemeralContainerStatuses())
                .filter(Objects::nonNull)
                .map(Collection::stream)
                .reduce(Stream::concat)
                .orElseGet(Stream::empty)
                .filter(containerStatus -> containerStatus.getState().getTerminated() != null
                        && containerStatus.getState().getTerminated().getExitCode() != 0)
                .map(V1ContainerStatus::getName)
                .findFirst();
    }

    public Mono<Boolean> skipIfNotFound(Mono<Void> operation, Boolean previous) {
        return operation
                .thenReturn(Boolean.TRUE)
                .onErrorResume(e -> e instanceof ApiException apiException && apiException.getCode() == 404
                        ? Mono.just(Boolean.FALSE)
                        : Mono.error(e))
                .map(deleted -> previous || deleted);
    }

    public ApiClient createClient(String configPath, @Nullable String context) throws IOException {
        try (Reader reader = Files.newBufferedReader(Path.of(configPath))) {
            KubeConfig config = KubeConfig.loadKubeConfig(reader);
            if (StringUtils.isNotBlank(context)) {
                config.setContext(context);
            }

            return ClientBuilder.kubeconfig(config).build();
        }
    }
}
