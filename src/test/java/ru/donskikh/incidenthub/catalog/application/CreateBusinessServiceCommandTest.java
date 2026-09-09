package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.catalog.ServiceTier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateBusinessServiceCommandTest {

    @Test
    void acceptsValidValues() {
        CreateBusinessServiceCommand command = command(
                "BILLING", "Billing", "Billing service", 7L, ServiceTier.TIER_1
        );

        assertThat(command.code()).isEqualTo("BILLING");
        assertThat(command.name()).isEqualTo("Billing");
        assertThat(command.description()).isEqualTo("Billing service");
        assertThat(command.ownerTeamId()).isEqualTo(7L);
        assertThat(command.tier()).isEqualTo(ServiceTier.TIER_1);
    }

    @Test
    void rejectsNullAndBlankCode() {
        assertThatThrownBy(() -> command(null, "Billing", "Billing service", 7L, ServiceTier.TIER_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must not be blank");
        assertThatThrownBy(() -> command("   ", "Billing", "Billing service", 7L, ServiceTier.TIER_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must not be blank");
    }

    @Test
    void rejectsNullAndBlankName() {
        assertThatThrownBy(() -> command("BILLING", null, "Billing service", 7L, ServiceTier.TIER_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
        assertThatThrownBy(() -> command("BILLING", "   ", "Billing service", 7L, ServiceTier.TIER_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }

    @Test
    void rejectsNullAndBlankDescription() {
        assertThatThrownBy(() -> command("BILLING", "Billing", null, 7L, ServiceTier.TIER_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description must not be blank");
        assertThatThrownBy(() -> command("BILLING", "Billing", "   ", 7L, ServiceTier.TIER_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description must not be blank");
    }

    @Test
    void rejectsNonPositiveOwnerTeamId() {
        assertThatThrownBy(() -> command("BILLING", "Billing", "Billing service", 0L, ServiceTier.TIER_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ownerTeamId must be positive");
        assertThatThrownBy(() -> command("BILLING", "Billing", "Billing service", -1L, ServiceTier.TIER_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ownerTeamId must be positive");
    }

    @Test
    void rejectsNullTier() {
        assertThatThrownBy(() -> command("BILLING", "Billing", "Billing service", 7L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("tier must not be null");
    }

    private static CreateBusinessServiceCommand command(
            String code,
            String name,
            String description,
            long ownerTeamId,
            ServiceTier tier
    ) {
        return new CreateBusinessServiceCommand(code, name, description, ownerTeamId, tier);
    }
}
