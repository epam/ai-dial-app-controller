package com.epam.aidial.dto;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record CreateSessionRequestDto(String image, @Nullable Map<String, String> env) {
}