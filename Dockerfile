FROM gradle:8.10-jdk21-alpine AS cache

WORKDIR /home/gradle/src
ENV GRADLE_USER_HOME="/cache"
COPY build.gradle settings.gradle ./
# just pull dependencies for cache
RUN gradle --no-daemon build --stacktrace

FROM gradle:8.10-jdk21-alpine AS builder

COPY --from=cache /cache /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src

WORKDIR /home/gradle/src
RUN gradle --no-daemon build --stacktrace -PdisableCompression=true -x test

RUN mkdir /build && tar -xf /home/gradle/src/build/distributions/ai-dial-app-controller*.tar --strip-components=1 -C /build

FROM eclipse-temurin:21-jdk-alpine

ENV OTEL_TRACES_EXPORTER="none"
ENV OTEL_METRICS_EXPORTER="none"
ENV OTEL_LOGS_EXPORTER="none"

WORKDIR /app

RUN adduser -u 1001 --disabled-password --gecos "" appuser

COPY --from=builder --chown=appuser:appuser /build/ .
RUN chown -R appuser:appuser /app

USER appuser

HEALTHCHECK --start-period=30s --interval=1m --timeout=3s \
  CMD wget --no-verbose --spider --tries=1 http://localhost:8080/health || exit 1

EXPOSE 8080 9464

ENTRYPOINT ["/app/bin/ai-dial-app-controller"]
