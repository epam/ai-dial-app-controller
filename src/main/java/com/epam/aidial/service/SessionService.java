package com.epam.aidial.service;

import com.epam.aidial.kubernetes.KubernetesClient;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.epam.aidial.util.NamingUtils.sessionName;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final KubernetesService kubernetesService;
    private final ConfigService configService;

    @Value("${app.deploy-namespace}")
    private final String namespace;

    @Value("${app.session-setup-timeout-sec}")
    private final int timeout;

    public Mono<String> create(String name, String image, Map<String, String> env) {
        KubernetesClient kubernetesClient = kubernetesService.deployClient();
        String sessionName = sessionName(name);

        V1Pod pod = configService.sessionPod(sessionName, image, env);
        return kubernetesClient.createPod(namespace, pod, timeout).map(v1Pod -> podUrl(pod));
    }

    private String podUrl(V1Pod pod) {
        return String.format("http://%s:8080", pod.getStatus().getPodIP());
    }

    public Mono<Boolean> delete(String name) {
        KubernetesClient kubernetesClient = kubernetesService.deployClient();
        String sessionName = sessionName(name);
        return kubernetesClient.deletePod(namespace, sessionName);
    }
}