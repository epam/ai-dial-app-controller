package com.epam.aidial.config;

import com.epam.aidial.util.KubernetesUtils;
import io.kubernetes.client.openapi.ApiClient;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class WebFluxConfig {
    @Bean
    public ApiClient buildKubeClient(
            @Value("${app.kube-config}") String configPath,
            @Value("${app.build-context:#{null}}") String context) throws IOException {
        return KubernetesUtils.createClient(configPath, context);
    }

    @Bean
    public ApiClient deployKubeClient(
            @Value("${app.kube-config}") String configPath,
            @Value("${app.deploy-context:#{null}}") String context) throws IOException {
        return KubernetesUtils.createClient(configPath, context);
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .build();
    }
}
