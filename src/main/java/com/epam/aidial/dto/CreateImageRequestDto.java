package com.epam.aidial.dto;

import org.jetbrains.annotations.Nullable;

public record CreateImageRequestDto(
        String sources,
        @Nullable String runtime) {
}
