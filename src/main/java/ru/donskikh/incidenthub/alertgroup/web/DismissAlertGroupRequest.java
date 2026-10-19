package ru.donskikh.incidenthub.alertgroup.web;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record DismissAlertGroupRequest(
        @NotNull @PositiveOrZero Long expectedVersion,
        @Size(max = 2000) String reason
) {}
