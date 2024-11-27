package com.epam.aidial.service;

import com.epam.aidial.util.SseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class HeartbeatService {
    @Value("${app.heartbeat-period-sec}")
    private final int heartbeatPeriodSec;

    public Flux<ServerSentEvent<Object>> setupHeartbeats(Mono<ServerSentEvent<Object>> data) {
        // Share to avoid double invocation
        Mono<ServerSentEvent<Object>> shared = data.share();
        Flux<ServerSentEvent<Object>> heartbeats = Flux.interval(
                Duration.ZERO,
                Duration.ofSeconds(heartbeatPeriodSec))
                .map(ignore -> SseUtils.heartbeat())
                .takeUntilOther(shared);

        return Flux.concat(heartbeats, shared);
    }
}
