package com.epam.aidial.kubernetes.knative;

import lombok.Data;

@Data
public class V1Addressable {
    String name;
    String url;
    String CACerts;
    String audience;
}
