package com.epam.aidial.kubernetes.knative;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class V1ServiceStatus {
    Long observedGeneration;
    V1Condition[] conditions;
    Map<String, String> annotations = new HashMap<>();
    String latestReadyRevisionName;
    String latestCreatedRevisionName;
    String url;
    V1Addressable address;
    V1TrafficTarget[] traffic;
}
