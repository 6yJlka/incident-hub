package ru.donskikh.incidenthub.team.web;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @Schema(description = "Unique team code; stored trimmed and uppercase", example = "PLATFORM")
        @NotBlank @Size(max = 50) String code,
        @Schema(description = "Human-readable team name", example = "Platform Engineering")
        @NotBlank @Size(max = 150) String name
) {
}
