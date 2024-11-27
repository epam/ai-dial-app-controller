package com.epam.aidial.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class WebFluxConfig {
    @Bean
    public ApiClient apiClient() throws IOException {
        ApiClient client = ClientBuilder.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);

        return client;
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .build();
    }
}
