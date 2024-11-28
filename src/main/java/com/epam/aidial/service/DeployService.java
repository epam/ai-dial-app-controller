package com.epam.aidial.service;

import com.epam.aidial.dto.GetApplicationLogsResponseDto;
import com.epam.aidial.kubernetes.KubernetesClient;
import com.epam.aidial.util.KubernetesUtils;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.epam.aidial.util.NamingUtils.appName;

@Service
@RequiredArgsConstructor
public class DeployService {
    private final KubernetesService kubernetesService;
    private final ConfigService templateService;

    @Value("${app.deploy-namespace}")
    private final String namespace;

    @Value("${app.service-container}")
    private final String serviceContainer;

    @Value("${app.service-setup-timeout-sec}")
    private final int serviceSetupTimeoutSec;

    public Mono<String> deploy(String name, Map<String, String> env) {
        KubernetesClient kubernetesClient = kubernetesService.deployClient();
        return Mono.fromCallable(() -> templateService.appServiceConfig(name, env))
                .flatMap(service -> kubernetesClient.createKnativeService(namespace, service, serviceSetupTimeoutSec));
    }

    public Mono<Boolean> undeploy(String name) {
        KubernetesClient kubernetesClient = kubernetesService.deployClient();
        return KubernetesUtils.skipIfNotFound(
                kubernetesClient.deleteKnativeService(
                        namespace, appName(name), kubernetesService.getKnativeServiceVersion()),
                Boolean.FALSE);
    }

    public Mono<List<GetApplicationLogsResponseDto.LogEntry>> logs(String name) {
        KubernetesClient kubernetesClient = kubernetesService.deployClient();
        return kubernetesClient.getKnativeServicePods(namespace, appName(name))
                .flatMapIterable(V1PodList::getItems)
                .flatMap(pod -> {
                    if (!isContainerReady(pod.getStatus(), serviceContainer)) {
                        return Mono.empty();
                    }

                    String podName = pod.getMetadata().getName();
                    return kubernetesClient.getContainerLog(namespace, podName, serviceContainer)
                            .map(text -> new GetApplicationLogsResponseDto.LogEntry(podName, text));
                })
                .collectList();
    }

    private static boolean isContainerReady(V1PodStatus podStatus, String container) {
        V1ContainerStatus containerStatus = podStatus.getContainerStatuses().stream()
                .filter(status -> container.equals(status.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Container %s is missing in service pod".formatted(container)));
        return containerStatus.getState().getWaiting() == null;
    }
}
