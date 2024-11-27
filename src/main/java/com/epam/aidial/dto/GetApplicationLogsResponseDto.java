package com.epam.aidial.dto;

import java.util.List;

public record GetApplicationLogsResponseDto(List<LogEntry> logs) {
    public record LogEntry(String instance, String content) {
    }
}
