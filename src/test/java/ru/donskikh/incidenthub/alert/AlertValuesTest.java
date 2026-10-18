package ru.donskikh.incidenthub.alert;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.alert.application.IncomingAlert;

import ru.donskikh.incidenthub.alert.application.AlertAcceptanceResult;
import ru.donskikh.incidenthub.alert.application.AlertAcceptanceStatus;
import ru.donskikh.incidenthub.alert.application.AlertRejectionReason;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertValuesTest {
    @Test
    void validatesExternalKeyAndPreservesCase() {
        assertThatThrownBy(() -> new AlertKey(" ", "id")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlertKey("source", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlertKey("x".repeat(101), "id")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlertKey("source", "x".repeat(256))).isInstanceOf(IllegalArgumentException.class);
        assertThat(new AlertKey("Source", "ID")).isNotEqualTo(new AlertKey("source", "ID"));
    }

    @Test
    void validatesInsertionValuesAndNormalizesServiceCode() {
        var key = new AlertKey("source", "id");
        Instant now = Instant.now();
        assertThatThrownBy(() -> new NewAlert(key, 0, AlertSignalType.LATENCY,
                AlertSeverity.INFO, now, now, "Description")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NewAlert(key, 1, AlertSignalType.LATENCY,
                AlertSeverity.INFO, now, now, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NewAlert(key, 1, null,
                AlertSeverity.INFO, now, now, "Description")).isInstanceOf(NullPointerException.class);
        var input = new IncomingAlert("source", "id", " core_database ", AlertSignalType.LATENCY,
                AlertSeverity.INFO, now, "Description");
        assertThat(input.serviceCode()).isEqualTo("CORE_DATABASE");
        assertThatThrownBy(() -> new IncomingAlert("source", "id", " ", AlertSignalType.LATENCY,
                AlertSeverity.INFO, now, "Description")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesAcceptanceResults() {
        assertThatThrownBy(() -> new AlertAcceptanceResult("id", AlertAcceptanceStatus.ACCEPTED, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlertAcceptanceResult("id", AlertAcceptanceStatus.REJECTED, 1L, AlertRejectionReason.UNKNOWN_SERVICE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlertAcceptanceResult("id", AlertAcceptanceStatus.REJECTED, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
