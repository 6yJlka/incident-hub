package ru.donskikh.incidenthub.team.application;

public record ListTeamsQuery(int page, int size, Boolean active) {

    private static final int MAX_PAGE_SIZE = 100;

    public ListTeamsQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must not exceed " + MAX_PAGE_SIZE);
        }
    }
}
