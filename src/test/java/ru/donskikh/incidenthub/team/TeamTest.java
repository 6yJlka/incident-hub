package ru.donskikh.incidenthub.team;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamTest {

    @Test
    void createsActiveTeamAndNormalizesFields() {
        Team team = new Team("  Platform Team  ", "  platform  ");

        assertThat(team.getName()).isEqualTo("Platform Team");
        assertThat(team.getCode()).isEqualTo("PLATFORM");
        assertThat(team.isActive()).isTrue();
    }

    @Test
    void rejectsNullAndBlankName() {
        assertThatThrownBy(() -> new Team(null, "PLATFORM"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
        assertThatThrownBy(() -> new Team("   ", "PLATFORM"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }

    @Test
    void rejectsNullAndBlankCode() {
        assertThatThrownBy(() -> new Team("Platform", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must not be blank");
        assertThatThrownBy(() -> new Team("Platform", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must not be blank");
    }

    @Test
    void activatesAndDeactivatesTeam() {
        Team team = new Team("Platform", "PLATFORM");

        team.deactivate();
        assertThat(team.isActive()).isFalse();

        team.activate();
        assertThat(team.isActive()).isTrue();
    }

    @Test
    void changesAndTrimsName() {
        Team team = new Team("Platform", "PLATFORM");

        team.changeName("  Core Platform  ");

        assertThat(team.getName()).isEqualTo("Core Platform");
    }

    @Test
    void rejectsBlankChangedName() {
        Team team = new Team("Platform", "PLATFORM");

        assertThatThrownBy(() -> team.changeName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }
}
