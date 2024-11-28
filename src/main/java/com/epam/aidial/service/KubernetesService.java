package com.epam.aidial.service;

import com.epam.aidial.kubernetes.KubernetesClient;
import io.kubernetes.client.openapi.ApiClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class KubernetesService {
    @Qualifier("buildKubeClient")
    private final ApiClient buildClient;

    @Qualifier("deployKubeClient")
    private final ApiClient deployClient;

    @Getter
    @Value("${app.service-config.apiVersion}")
    private final String knativeServiceVersion;

    @PostConstruct
    private void initialize() {
        KubernetesClient.addKnativeServiceToModelMap(knativeServiceVersion);
    }

    public KubernetesClient buildClient() {
        return new KubernetesClient(buildClient);
    }

    public KubernetesClient deployClient() {
        return new KubernetesClient(deployClient);
    }

}
