package ru.donskikh.incidenthub.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.donskikh.incidenthub.catalog.ServiceTier;

public record CreateBusinessServiceRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        @NotBlank String description,
        @NotNull @Positive Long ownerTeamId,
        @NotNull ServiceTier tier
) {
}
