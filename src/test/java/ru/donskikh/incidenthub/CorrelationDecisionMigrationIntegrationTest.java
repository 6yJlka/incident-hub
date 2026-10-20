package ru.donskikh.incidenthub;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.alert.correlation.Evidence;
import ru.donskikh.incidenthub.alert.correlation.RootAssessment;
import ru.donskikh.incidenthub.alert.correlation.RootCandidate;
import ru.donskikh.incidenthub.alert.infrastructure.CorrelationJsonCodec;

import java.sql.DriverManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationDecisionMigrationIntegrationTest extends PostgreSQLIntegrationTest {
    private static final String SCHEMA = "incident_hub_decision_migration_test";

    @Test
    void backfillsExistingGroupsAndLinksAndEnforcesRelationalConstraints() throws Exception {
        try (var connection = DriverManager.getConnection(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword());
             var sql = connection.createStatement()) {
            try {
                flyway("14").migrate();
                sql.execute("set search_path to " + SCHEMA);
                sql.execute("insert into teams (id,name,code) values (1,'Legacy team','LEGACY_TEAM')");
                sql.execute("insert into business_services (id,code,name,description,owner_team_id,tier) "
                        + "values (2,'LEGACY_SERVICE','Legacy service','Description',1,'TIER_2')");
                sql.execute("insert into alert_groups (id,probable_root_service_id,first_alert_at,last_alert_at) "
                        + "values (3,2,now(),now())");
                sql.execute("insert into alerts (id,source,external_id,business_service_id,signal_type,severity,occurred_at,received_at,description,group_id) "
                        + "values (4,'legacy','linked',2,'LATENCY','WARNING',now(),now(),'Signal',3),"
                        + "(5,'legacy','unassigned',2,'LATENCY','INFO',now(),now(),'Signal',null)");
                flyway(null).migrate();
                assertThat(flyway(null).migrate().migrationsExecuted).isZero();
                try (var rows = sql.executeQuery("select root_assessment::text,root_assessment_status,probable_root_service_id from alert_groups")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(CorrelationJsonCodec.readAssessment(rows.getString(1))).isEqualTo(new RootAssessment.Ranked(
                            List.of(new RootCandidate(2, 1.0)), List.of(new Evidence.Migrated())));
                    assertThat(rows.getString(2)).isEqualTo("RANKED");
                    assertThat(rows.getLong(3)).isEqualTo(2);
                }
                try (var rows = sql.executeQuery("select link_strength,link_evidence::text from alerts where id=4")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("TENTATIVE");
                    assertThat(CorrelationJsonCodec.readEvidence(rows.getString(2))).containsExactly(new Evidence.Migrated());
                }
                try (var rows = sql.executeQuery("select link_strength,link_evidence from alerts where id=5")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isNull();
                    assertThat(rows.getString(2)).isNull();
                }
                assertThatThrownBy(() -> sql.executeUpdate("update alert_groups set probable_root_service_id=null where id=3"))
                        .hasMessageContaining("chk_alert_groups_root_assessment_root");
                assertThatThrownBy(() -> sql.executeUpdate("update alert_groups set root_assessment_status='UNDETERMINED' where id=3"))
                        .hasMessageContaining("chk_alert_groups_root_assessment_root");
                assertThatThrownBy(() -> sql.executeUpdate("update alerts set group_id=null where id=4"))
                        .hasMessageContaining("chk_alerts_link_metadata");
                assertThatThrownBy(() -> sql.executeUpdate("update alerts set link_strength='UNKNOWN' where id=4"))
                        .hasMessageContaining("chk_alerts_link_strength");
            } finally {
                sql.execute("drop schema if exists " + SCHEMA + " cascade");
            }
        }
    }

    private Flyway flyway(String target) {
        var config = Flyway.configure().dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .schemas(SCHEMA).defaultSchema(SCHEMA).locations("classpath:db/migration");
        if (target != null) config.target(target);
        return config.load();
    }
}
