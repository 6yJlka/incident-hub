package ru.donskikh.incidenthub;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class AuditActorMigrationIntegrationTest extends PostgreSQLIntegrationTest {

    private static final String SCHEMA = "incident_hub_actor_migration_test";

    @Test
    void migratesExistingAuditRowsWithoutInventingAnActor() throws Exception {
        try {
            flyway(MigrationVersion.fromVersion("8")).migrate();

            try (Connection connection = connection(); Statement statement = connection.createStatement()) {
                statement.execute("set search_path to " + SCHEMA);
                statement.execute("insert into users (id, email, display_name, role) "
                        + "values (1, 'legacy@example.com', 'Legacy Reporter', 'REPORTER')");
                statement.execute("insert into teams (id, name, code) values (2, 'Legacy Team', 'LEGACY_TEAM')");
                statement.execute("insert into business_services "
                        + "(id, code, name, description, owner_team_id, tier) "
                        + "values (3, 'LEGACY_SERVICE', 'Legacy Service', 'Description', 2, 'TIER_2')");
                statement.execute("insert into incidents "
                        + "(id, title, description, affected_service_id, source, priority, severity, status, reporter_id) "
                        + "values (4, 'Legacy incident', 'Description', 3, 'MANUAL', 'HIGH', 'SEV2', 'OPEN', 1)");
                statement.execute("insert into incident_audit_events "
                        + "(id, incident_id, event_type, from_status, to_status) "
                        + "values (5, 4, 'CREATED', null, 'OPEN')");
            }

            flyway(null).migrate();

            try (Connection connection = connection(); Statement statement = connection.createStatement()) {
                statement.execute("set search_path to " + SCHEMA);
                try (ResultSet result = statement.executeQuery(
                        "select actor_id from incident_audit_events where id = 5")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getObject("actor_id")).isNull();
                }
                try (ResultSet result = statement.executeQuery(
                        "select is_nullable from information_schema.columns "
                                + "where table_schema = '" + SCHEMA + "' "
                                + "and table_name = 'incident_audit_events' and column_name = 'actor_id'")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString("is_nullable")).isEqualTo("YES");
                }
                try (ResultSet result = statement.executeQuery(
                        "select count(*) from incident_audit_events")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getLong(1)).isEqualTo(1L);
                }
            }
        } finally {
            try (Connection connection = connection(); Statement statement = connection.createStatement()) {
                statement.execute("drop schema if exists " + SCHEMA + " cascade");
            }
        }
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .locations("classpath:db/migration");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                POSTGRESQL.getJdbcUrl(),
                POSTGRESQL.getUsername(),
                POSTGRESQL.getPassword()
        );
    }
}
