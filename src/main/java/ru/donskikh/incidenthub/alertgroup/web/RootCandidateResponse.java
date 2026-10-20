package ru.donskikh.incidenthub.alertgroup.web;



public record RootCandidateResponse(long serviceId, double score, String serviceCode, String serviceName) {}
