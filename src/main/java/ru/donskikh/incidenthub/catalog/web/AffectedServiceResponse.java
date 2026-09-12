package ru.donskikh.incidenthub.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceTier;

public record AffectedServiceResponse(
        @Schema(description = "Affected service identifier", example = "44") Long id,
        @Schema(description = "Affected service code", example = "CUSTOMER_PORTAL") String code,
        @Schema(description = "Affected service name", example = "Customer Portal") String name,
        @Schema(description = "Business criticality tier", example = "TIER_2") ServiceTier tier,
        @Schema(description = "Whether the affected service is active", example = "true") boolean active,
        @Schema(description = "Owner team identifier", example = "15") Long ownerTeamId,
        @Schema(description = "Owner team name", example = "Customer Operations") String ownerTeamName,
        @Schema(description = "Minimum number of dependency edges from the failed service", example = "2") int depth,
        @Schema(description = "Type of the edge used on the selected minimum-depth path", example = "SYNC")
        DependencyType dependencyType
) {
}
