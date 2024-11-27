package com.epam.aidial.kubernetes.knative;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class V1Condition {
    String type;
    String status;
    String severity;
    OffsetDateTime lastTransitionTime;
    String reason;
    String message;
}
