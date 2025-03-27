package com.epam.aidial.service;

import com.epam.aidial.kubernetes.KubernetesClient;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

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
        V1Service svc = configService.sessionSvc(sessionName);

        Mono<V1Service> svcTask = kubernetesClient.createService(namespace, svc);
        Mono<V1Pod> podTask = kubernetesClient.createPod(namespace, pod, timeout);

        return Mono.zip(podTask, svcTask).map(Tuple2::getT2).map(this::serviceUrl);
    }

    private String serviceUrl(V1Service service) {
        V1ObjectMeta metadata = service.getMetadata();
        int port = service.getSpec().getPorts().getFirst().getPort();
        return String.format("http://%s.%s.svc.cluster.local:%s", metadata.getName(), metadata.getNamespace(), port);
    }

    public Mono<Boolean> delete(String name) {
        KubernetesClient kubernetesClient = kubernetesService.deployClient();
        String sessionName = sessionName(name);

        Mono<Boolean> podTask = kubernetesClient.deletePod(namespace, sessionName);
        Mono<Boolean> svcTask = kubernetesClient.deleteService(namespace, sessionName);

        return Mono.zip(podTask, svcTask).map(objects -> objects.getT1() || objects.getT2());
    }
}