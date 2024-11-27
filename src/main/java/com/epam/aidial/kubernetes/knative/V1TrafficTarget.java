package com.epam.aidial.kubernetes.knative;

import lombok.Data;

@Data
public class V1TrafficTarget {
    String tag;
    String revisionName;
    String configurationName;
    Boolean latestRevision;
    Long percent;
    String url;
}
