package com.epam.aidial.util;

import com.epam.aidial.dto.ErrorResponseDto;
import lombok.experimental.UtilityClass;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Mono;

@UtilityClass
public class SseUtils {
    private static final String RESULT = "result";
    private static final String ERROR = "error";

    public Mono<ServerSentEvent<Object>> mapToSseEvent(Mono<?> data) {
        return data.map(SseUtils::result)
                .onErrorResume(e -> Mono.just(SseUtils.error(new ErrorResponseDto(e.getMessage()))));
    }

    public ServerSentEvent<Object> heartbeat() {
        return ServerSentEvent.builder()
                .comment("heartbeat")
                .build();
    }

    public ServerSentEvent<Object> result(Object data) {
        return ServerSentEvent.builder()
                .event(SseUtils.RESULT)
                .data(data)
                .build();
    }

    public ServerSentEvent<Object> error(Object error) {
        return ServerSentEvent.builder()
                .event(SseUtils.ERROR)
                .data(error)
                .build();
    }
}
