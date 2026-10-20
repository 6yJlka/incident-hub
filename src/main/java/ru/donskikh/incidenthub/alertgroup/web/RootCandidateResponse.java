package ru.donskikh.incidenthub.alertgroup.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record RootCandidateResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long serviceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) double score,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) String serviceCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) String serviceName) {}
