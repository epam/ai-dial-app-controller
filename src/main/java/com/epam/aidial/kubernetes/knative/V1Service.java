package com.epam.aidial.kubernetes.knative;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Data;
import lombok.Getter;

@Data
public class V1Service implements KubernetesObject {
    @Getter(onMethod = @__(@Override))
    String apiVersion;
    @Getter(onMethod = @__(@Override))
    String kind;
    @Getter(onMethod = @__(@Override))
    V1ObjectMeta metadata;
    V1ServiceSpec spec;
    V1ServiceStatus status;
}
