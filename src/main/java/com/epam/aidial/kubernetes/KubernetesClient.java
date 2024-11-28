package com.epam.aidial.kubernetes;

import com.epam.aidial.kubernetes.knative.V1Service;
import com.epam.aidial.util.KubernetesUtils;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.ModelMapper;
import io.kubernetes.client.util.Watch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class KubernetesClient {
    private static final String SERVICES = "services";
    private static final TypeToken<Watch.Response<V1Job>> JOB_TYPE_TOKEN = new TypeToken<>() {
    };
    private static final TypeToken<Watch.Response<V1Service>> SERVICE_TYPE_TOKEN = new TypeToken<>() {
    };
    private static final String FOREGROUND_POLICY = "Foreground";

    private final ApiClient apiClient;

    public Mono<Void> createSecret(String namespace, V1Secret secret) {
        return Mono.create(sink -> {
            V1ObjectMeta metadata = secret.getMetadata();
            String name = metadata.getName();

            CoreV1Api coreApi = new CoreV1Api(apiClient);
            log.info("Creating a secret {}", name);
            try {
                coreApi.createNamespacedSecret(namespace, secret)
                        .executeAsync(new NoProgressApiCallback<>() {
                            @Override
                            public void onFailure(ApiException e, int i, Map<String, List<String>> map) {
                                sink.error(e);
                            }

                            @Override
                            public void onSuccess(V1Secret state, int i, Map<String, List<String>> map) {
                                log.info("Secret {} has been successfully created", name);
                                sink.success();
                            }
                        });
            } catch (ApiException e) {
                sink.error(e);
            }
        });
    }

    public Mono<Void> deleteSecret(String namespace, String name) {
        return Mono.create(sink -> {
            CoreV1Api coreApi = new CoreV1Api(apiClient);
            log.info("Deleting a secret {}", name);
            try {
                coreApi.deleteNamespacedSecret(name, namespace)
                        .executeAsync(new NoProgressApiCallback<>() {
                            @Override
                            public void onFailure(ApiException e, int i, Map<String, List<String>> map) {
                                sink.error(e);
                            }

                            @Override
                            public void onSuccess(V1Status state, int i, Map<String, List<String>> map) {
                                log.info("Secret {} has been deleted", name);
                                sink.success();
                            }
                        });
            } catch (ApiException e) {
                sink.error(e);
            }
        });
    }

    public Mono<Void> createJob(String namespace, V1Job job, int imageBuildTimeoutSec) {
        // Currently there is no asynchronous Watch api
        return Mono.<Void>fromCallable(() -> {
            String name = job.getMetadata().getName();

            BatchV1Api batchApi = new BatchV1Api(apiClient);
            Call call = batchApi.listNamespacedJob(namespace)
                    .watch(true)
                    .timeoutSeconds(imageBuildTimeoutSec)
                    .buildCall(null);

            try (Watch<V1Job> watch = Watch.createWatch(batchApi.getApiClient(), call, JOB_TYPE_TOKEN.getType())) {
                log.info("Creating a job {}", name);
                batchApi.createNamespacedJob(namespace, job)
                        .execute();

                log.info("Waiting for job {} to complete", name);
                for (Watch.Response<V1Job> item : watch) {
                    V1Job jobState = item.object;
                    if (jobState != null && name.equals(jobState.getMetadata().getName())
                            && KubernetesUtils.extractJobCompletionStatus(jobState)) {
                        log.info("Job {} has completed successfully", name);
                        return null;
                    }
                }
            }

            throw new IllegalStateException("Subscription to job %s events expired".formatted(name));
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<V1PodList> getJobPods(String namespace, String name) {
        return getPods(namespace, "job-name=" + name);
    }

    public Mono<V1PodList> getKnativeServicePods(String namespace, String name) {
        return getPods(namespace, "serving.knative.dev/service=" + name);
    }

    private Mono<V1PodList> getPods(String namespace, String label) {
        return Mono.create(sink -> {
            CoreV1Api coreV1Api = new CoreV1Api(apiClient);
            log.info("Querying pods with label {}", label);
            try {
                coreV1Api.listNamespacedPod(namespace)
                        .labelSelector(label)
                        .executeAsync(new NoProgressApiCallback<>() {
                            @Override
                            public void onFailure(ApiException e, int i, Map<String, List<String>> map) {
                                sink.error(e);
                            }

                            @Override
                            public void onSuccess(V1PodList state, int i, Map<String, List<String>> map) {
                                log.info("Received pod list with label {}", label);
                                sink.success(state);
                            }
                        });
            } catch (ApiException e) {
                sink.error(e);
            }
        });
    }

    public Mono<String> getContainerLog(String namespace, String pod, String container) {
        return Mono.create(sink -> {
            CoreV1Api coreV1Api = new CoreV1Api(apiClient);
            log.info("Retrieving pod {} container {} logs", pod, container);
            try {
                coreV1Api.readNamespacedPodLog(pod, namespace)
                        .container(container)
                        .executeAsync(new NoProgressApiCallback<>() {
                            @Override
                            public void onFailure(ApiException e, int i, Map<String, List<String>> map) {
                                sink.error(e);
                            }

                            @Override
                            public void onSuccess(String logs, int i, Map<String, List<String>> map) {
                                log.info("Retrieved pod {} container {} logs", pod, container);
                                sink.success(logs);
                            }
                        });
            } catch (ApiException e) {
                sink.error(e);
            }
        });
    }

    public Mono<Void> deleteJob(String namespace, String name) {
        return Mono.create(sink -> {
            BatchV1Api batchV1Api = new BatchV1Api(apiClient);
            log.info("Deleting a job {}", name);
            try {
                batchV1Api.deleteNamespacedJob(name, namespace)
                        .propagationPolicy(FOREGROUND_POLICY)
                        .executeAsync(new NoProgressApiCallback<>() {
                            @Override
                            public void onFailure(ApiException e, int i, Map<String, List<String>> map) {
                                sink.error(e);
                            }

                            @Override
                            public void onSuccess(V1Status state, int i, Map<String, List<String>> map) {
                                log.info("Job {} has been deleted", name);
                                sink.success();
                            }
                        });
            } catch (ApiException e) {
                sink.error(e);
            }
        });
    }

    public Mono<String> createKnativeService(String namespace, V1Service service, int serviceSetupTimeoutSec) {
        // Currently there is no asynchronous Watch api
        return Mono.fromCallable(() -> {
            String name = service.getMetadata().getName();
            ServiceVersion version = ServiceVersion.parse(service.getApiVersion());

            CustomObjectsApi customObjectsApi = new CustomObjectsApi(apiClient);
            Call call = customObjectsApi.listNamespacedCustomObject(version.group(), version.version(), namespace, SERVICES)
                    .watch(true)
                    .timeoutSeconds(serviceSetupTimeoutSec)
                    .buildCall(null);
            try (Watch<V1Service> watch = Watch.createWatch(
                    customObjectsApi.getApiClient(), call, SERVICE_TYPE_TOKEN.getType())) {
                log.info("Creating a service {}", name);
                customObjectsApi.createNamespacedCustomObject(version.group(), version.version(), namespace, SERVICES, service)
                        .execute();

                for (Watch.Response<V1Service> item : watch) {
                    V1Service serviceState = item.object;
                    if (serviceState != null && name.equals(serviceState.getMetadata().getName())) {
                        String url = KubernetesUtils.extractServiceUrl(serviceState);
                        if (url != null) {
                            log.info("Service {} has been set up", name);
                            return url;
                        }
                    }
                }
            }

            throw new IllegalStateException("Subscription to service %s events expired".formatted(name));
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteKnativeService(String namespace, String name, String serviceVersion) {
        return Mono.create(sink -> {
            ServiceVersion version = ServiceVersion.parse(serviceVersion);

            CustomObjectsApi customObjectsApi = new CustomObjectsApi(apiClient);
            log.info("Deleting a service {}", name);
            try {
                customObjectsApi.deleteNamespacedCustomObject(version.group(), version.version(), namespace, SERVICES, name)
                        .propagationPolicy(FOREGROUND_POLICY)
                        .executeAsync(new NoProgressApiCallback<>() {
                            @Override
                            public void onFailure(ApiException e, int i, Map<String, List<String>> map) {
                                sink.error(e);
                            }

                            @Override
                            public void onSuccess(Object state, int i, Map<String, List<String>> map) {
                                log.info("Service {} has been deleted", name);
                                sink.success();
                            }
                        });
            } catch (ApiException e) {
                sink.error(e);
            }
        });
    }

    public static void addKnativeServiceToModelMap(String serviceVersion) {
        ServiceVersion version = ServiceVersion.parse(serviceVersion);
        ModelMapper.addModelMap(version.group(), version.version(), "Service", SERVICES, true, V1Service.class);
    }

    public record ServiceVersion(String group, String version) {
        public static ServiceVersion parse(String apiVersion) {
            int splitter = apiVersion.indexOf("/");
            String group = apiVersion.substring(0, splitter);
            String version = apiVersion.substring(splitter + 1);

            return new ServiceVersion(group, version);
        }
    }
}
