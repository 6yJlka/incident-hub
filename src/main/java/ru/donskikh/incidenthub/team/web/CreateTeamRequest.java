package ru.donskikh.incidenthub.team.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name
) {
}
