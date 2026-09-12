FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew \
    && mkdir -p src/main/java/ru/donskikh/incidenthub \
    && printf '%s\n' \
        'package ru.donskikh.incidenthub;' \
        'public class IncidentHubApplication { public static void main(String[] args) {} }' \
        > src/main/java/ru/donskikh/incidenthub/IncidentHubApplication.java \
    && ./gradlew bootJar --no-daemon \
    && rm -rf build src

COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S incidenthub && adduser -S -G incidenthub incidenthub

WORKDIR /app
COPY --from=builder --chown=incidenthub:incidenthub \
    /workspace/build/libs/*.jar app.jar

USER incidenthub
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
    CMD wget --quiet --output-document=/dev/null http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
