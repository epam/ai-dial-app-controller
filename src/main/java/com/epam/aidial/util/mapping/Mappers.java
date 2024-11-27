package com.epam.aidial.util.mapping;

import com.epam.aidial.kubernetes.knative.V1RevisionSpec;
import com.epam.aidial.kubernetes.knative.V1RevisionTemplateSpec;
import com.epam.aidial.kubernetes.knative.V1Service;
import com.epam.aidial.kubernetes.knative.V1ServiceSpec;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EnvFromSource;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Secret;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class Mappers {
    public static final NamedItemMapper<V1Container> CONTAINER_NAME = new NamedItemMapper<>(
            V1Container::new,
            V1Container::getName,
            V1Container::setName);

    public static final NamedItemMapper<V1EnvVar> ENV_VAR_NAME = new NamedItemMapper<>(
            V1EnvVar::new,
            V1EnvVar::getName,
            V1EnvVar::setName);

    public static final FieldMapper<V1Secret, V1ObjectMeta> SECRET_METADATA_FIELD = new FieldMapper<>(
            V1ObjectMeta::new,
            V1Secret::getMetadata,
            V1Secret::setMetadata);

    public static final FieldMapper<V1Job, V1ObjectMeta> JOB_METADATA_FIELD = new FieldMapper<>(
            V1ObjectMeta::new,
            V1Job::getMetadata,
            V1Job::setMetadata);

    public static final FieldMapper<V1Job, V1JobSpec> JOB_SPEC_FIELD = new FieldMapper<>(
            V1JobSpec::new,
            V1Job::getSpec,
            V1Job::setSpec);

    public static final FieldMapper<V1JobSpec, V1PodTemplateSpec> JOB_TEMPLATE_FIELD = new FieldMapper<>(
            V1PodTemplateSpec::new,
            V1JobSpec::getTemplate,
            V1JobSpec::setTemplate);

    public static final FieldMapper<V1PodTemplateSpec, V1PodSpec> JOB_TEMPLATE_SPEC_FIELD = new FieldMapper<>(
            V1PodSpec::new,
            V1PodTemplateSpec::getSpec,
            V1PodTemplateSpec::setSpec);

    public static final FieldMapper<V1PodSpec, List<V1Container>> POD_INIT_CONTAINERS_FIELD = new FieldMapper<>(
            ArrayList::new,
            V1PodSpec::getInitContainers,
            V1PodSpec::setInitContainers);

    public static final FieldMapper<V1PodSpec, List<V1Container>> POD_CONTAINERS_FIELD = new FieldMapper<>(
            ArrayList::new,
            V1PodSpec::getContainers,
            V1PodSpec::setContainers);

    public static final FieldMapper<V1Service, V1ObjectMeta> SERVICE_METADATA_FIELD = new FieldMapper<>(
            V1ObjectMeta::new,
            V1Service::getMetadata,
            V1Service::setMetadata);

    public static final FieldMapper<V1Service, V1ServiceSpec> SERVICE_SPEC_FIELD = new FieldMapper<>(
            V1ServiceSpec::new,
            V1Service::getSpec,
            V1Service::setSpec);

    public static final FieldMapper<V1ServiceSpec, V1RevisionTemplateSpec> SERVICE_TEMPLATE_FIELD = new FieldMapper<>(
            V1RevisionTemplateSpec::new,
            V1ServiceSpec::getTemplate,
            V1ServiceSpec::setTemplate);

    public static final FieldMapper<V1RevisionTemplateSpec, V1RevisionSpec> SERVICE_TEMPLATE_SPEC_FIELD = new FieldMapper<>(
            V1RevisionSpec::new,
            V1RevisionTemplateSpec::getSpec,
            V1RevisionTemplateSpec::setSpec);

    public static final FieldMapper<V1RevisionSpec, List<V1Container>> TEMPLATE_CONTAINERS_FIELD = new FieldMapper<>(
            ArrayList::new,
            V1RevisionSpec::getContainers,
            V1RevisionSpec::setContainers);

    public static final FieldMapper<V1Container, List<V1EnvVar>> CONTAINER_ENV_FIELD = new FieldMapper<>(
            ArrayList::new,
            V1Container::getEnv,
            V1Container::setEnv);

    public static final FieldMapper<V1Container, List<V1EnvFromSource>> CONTAINER_ENV_FROM_FIELD = new FieldMapper<>(
            ArrayList::new,
            V1Container::getEnvFrom,
            V1Container::setEnvFrom);

    public static final FieldMapper<V1Container, List<String>> CONTAINER_ARGS_FIELD = new FieldMapper<>(
            ArrayList::new,
            V1Container::getArgs,
            V1Container::setArgs);
}