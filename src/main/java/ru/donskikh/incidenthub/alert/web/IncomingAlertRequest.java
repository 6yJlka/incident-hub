package ru.donskikh.incidenthub.alert.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.donskikh.incidenthub.alert.AlertSignalType;
import ru.donskikh.incidenthub.alert.AlertSeverity;

import java.time.Instant;

public record IncomingAlertRequest(
        @Schema(description = "Monitoring system; part of the idempotency key", example = "prometheus")
        @NotBlank @Size(max = 100) String source,
        @Schema(description = "Event identifier within the source", example = "database-latency-123")
        @NotBlank @Size(max = 255) String externalId,
        @Schema(description = "Business service code", example = "CORE_DATABASE")
        @NotBlank @Size(max = 50) String serviceCode,
        @NotNull AlertSignalType signalType,
        @NotNull AlertSeverity severity,
        @Schema(description = "Time when the signal occurred", example = "2026-10-18T12:00:00Z")
        @NotNull Instant occurredAt,
        @Schema(description = "Short description of the observed signal", example = "Database p95 latency exceeds threshold")
        @NotBlank @Size(max = 2000) String description
) {
}
