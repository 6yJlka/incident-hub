package ru.donskikh.incidenthub;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class PostgreSQLIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("incident_hub")
            .withUsername("incident_user")
            .withPassword("incident_password");

    static {
        POSTGRESQL.start();
    }
}
