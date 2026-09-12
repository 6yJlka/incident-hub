package ru.donskikh.incidenthub.team.web;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateTeamResponse(
        @Schema(description = "Generated team identifier", example = "12") Long teamId,
        @Schema(description = "Normalized team code", example = "PLATFORM") String code,
        @Schema(description = "Whether the team is active", example = "true") boolean active
) {
}
