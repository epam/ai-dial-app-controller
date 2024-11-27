package com.epam.aidial.dto;

import java.util.Map;

public record CreateDeploymentRequestDto(Map<String, String> env) {
}
