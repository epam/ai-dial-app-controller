package com.epam.aidial.kubernetes.knative;

import lombok.Data;

@Data
public class V1ServiceSpec {
    V1RevisionTemplateSpec template;
    V1TrafficTarget[] traffic;
}
