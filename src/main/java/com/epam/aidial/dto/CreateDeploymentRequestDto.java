package com.epam.aidial.dto;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record CreateDeploymentRequestDto(
        @Nullable Map<String, String> env,
        @Nullable String image,
        @Nullable Integer initialScale,
        @Nullable Integer minScale,
        @Nullable Integer maxScale) {
}
