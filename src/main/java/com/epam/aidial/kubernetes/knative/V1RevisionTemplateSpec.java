package com.epam.aidial.kubernetes.knative;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Data;

@Data
public class V1RevisionTemplateSpec {
    V1ObjectMeta metadata;
    V1RevisionSpec spec;
}
