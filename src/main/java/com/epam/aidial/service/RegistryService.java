package com.epam.aidial.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryService {
    private static final String MANIFEST_URL_TEMPLATE = "%s://%s/v2/%s/manifests/%s";
    private final OkHttpClient okHttpClient;

    @Value("${app.docker-registry}")
    private final String dockerRegistry;

    @Value("${app.docker-registry-protocol}")
    private final URI dockerRegistryProtocol;

    @Value("${app.image-name-format}")
    private final String imageFormat;

    @Value("${app.image-label}")
    private final String imageLabel;

    public Mono<String> getDigest(String image) {
        return getDigest("application/vnd.oci.image.manifest.v1+json", image)
                .switchIfEmpty(getDigest("application/vnd.docker.distribution.manifest.v2+json", image));
    }

    private Mono<String> getDigest(String manifestVersion, String name) {
        return Mono.create(sink -> {
            String imageName = imageName(name);
            log.info("Retrieving digest for {} from manifest {}", imageName, manifestVersion);
            String url = MANIFEST_URL_TEMPLATE.formatted(
                    dockerRegistryProtocol, dockerRegistry, imageName, imageLabel);
            Request request = new Request.Builder()
                    .head()
                    .url(url)
                    .header("Accept", manifestVersion)
                    .build();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    sink.error(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    if (response.code() == 404) {
                        sink.success();
                    } else if (response.isSuccessful()) {
                        String digest = response.header("Docker-Content-Digest");
                        if (StringUtils.isBlank(digest)) {
                            sink.error(new IllegalStateException(
                                    "Missing digest in manifest %s response".formatted(manifestVersion)));
                        } else {
                            log.info("Retrieved {} digest for image {} and label {}: {}",
                                    manifestVersion, imageName, imageLabel, digest);
                            sink.success(digest);
                        }
                    } else {
                        sink.error(new ResponseStatusException(response.code(), response.message(), null));
                    }
                }
            });
        });
    }

    public Mono<Boolean> deleteManifest(String name, String digest) {
        return Mono.create(sink -> {
            String imageName = imageName(name);
            log.info("Deleting {} manifest", imageName);
            String url = MANIFEST_URL_TEMPLATE.formatted(
                    dockerRegistryProtocol, dockerRegistry, imageName, digest);
            Request request = new Request.Builder()
                    .delete()
                    .url(url)
                    .build();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    sink.error(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    if (response.code() == 404) {
                        sink.success(false);
                    } else if (response.isSuccessful()) {
                        log.info("Deleted image {} with digest {}", imageName, digest);
                        sink.success(true);
                    } else {
                        sink.error(new ResponseStatusException(response.code(), response.message(), null));
                    }
                }
            });
        });
    }

    public String fullImageName(String name) {
        return "%s/%s:%s".formatted(dockerRegistry, imageName(name), imageLabel);
    }

    private String imageName(String name) {
        return imageFormat.formatted(name);
    }
}
