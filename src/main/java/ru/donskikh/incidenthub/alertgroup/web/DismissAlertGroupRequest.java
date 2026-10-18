package ru.donskikh.incidenthub.alertgroup.web;

import jakarta.validation.constraints.Size;

public record DismissAlertGroupRequest(@Size(max = 2000) String reason) {}
