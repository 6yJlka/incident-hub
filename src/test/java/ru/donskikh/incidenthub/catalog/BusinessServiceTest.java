package ru.donskikh.incidenthub.catalog;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.team.Team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessServiceTest {

    private final Team ownerTeam = new Team("Platform", "PLATFORM");

    @Test
    void createsActiveBusinessServiceAndNormalizesFields() {
        BusinessService service = new BusinessService(
                "  billing-api  ",
                "  Billing API  ",
                "  Processes payments  ",
                ownerTeam,
                ServiceTier.TIER_1
        );

        assertThat(service.getCode()).isEqualTo("BILLING-API");
        assertThat(service.getName()).isEqualTo("Billing API");
        assertThat(service.getDescription()).isEqualTo("Processes payments");
        assertThat(service.getOwnerTeam()).isSameAs(ownerTeam);
        assertThat(service.getTier()).isEqualTo(ServiceTier.TIER_1);
        assertThat(service.isActive()).isTrue();
    }

    @Test
    void rejectsNullAndBlankCode() {
        assertThatThrownBy(() -> createService(null, "Billing API", "Processes payments"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must not be blank");
        assertThatThrownBy(() -> createService("   ", "Billing API", "Processes payments"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must not be blank");
    }

    @Test
    void rejectsNullAndBlankName() {
        assertThatThrownBy(() -> createService("BILLING-API", null, "Processes payments"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
        assertThatThrownBy(() -> createService("BILLING-API", "   ", "Processes payments"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }

    @Test
    void rejectsNullAndBlankDescription() {
        assertThatThrownBy(() -> createService("BILLING-API", "Billing API", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description must not be blank");
        assertThatThrownBy(() -> createService("BILLING-API", "Billing API", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description must not be blank");
    }

    @Test
    void rejectsNullOwnerTeamAndTier() {
        assertThatThrownBy(() -> new BusinessService(
                "BILLING-API", "Billing API", "Processes payments", null, ServiceTier.TIER_1
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ownerTeam must not be null");
        assertThatThrownBy(() -> new BusinessService(
                "BILLING-API", "Billing API", "Processes payments", ownerTeam, null
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("tier must not be null");
    }

    @Test
    void changesMutableFields() {
        Team newOwnerTeam = new Team("Payments", "PAYMENTS");
        BusinessService service = createService("BILLING-API", "Billing API", "Processes payments");

        service.changeName("  Payments API  ");
        service.updateDescription("  Handles customer payments  ");
        service.changeOwnerTeam(newOwnerTeam);
        service.changeTier(ServiceTier.TIER_2);

        assertThat(service.getName()).isEqualTo("Payments API");
        assertThat(service.getDescription()).isEqualTo("Handles customer payments");
        assertThat(service.getOwnerTeam()).isSameAs(newOwnerTeam);
        assertThat(service.getTier()).isEqualTo(ServiceTier.TIER_2);
    }

    @Test
    void validatesChangedFields() {
        BusinessService service = createService("BILLING-API", "Billing API", "Processes payments");

        assertThatThrownBy(() -> service.changeName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
        assertThatThrownBy(() -> service.updateDescription(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description must not be blank");
        assertThatThrownBy(() -> service.changeOwnerTeam(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ownerTeam must not be null");
        assertThatThrownBy(() -> service.changeTier(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("tier must not be null");
    }

    @Test
    void activatesAndDeactivatesBusinessService() {
        BusinessService service = createService("BILLING-API", "Billing API", "Processes payments");

        service.deactivate();
        assertThat(service.isActive()).isFalse();

        service.activate();
        assertThat(service.isActive()).isTrue();
    }

    private BusinessService createService(String code, String name, String description) {
        return new BusinessService(code, name, description, ownerTeam, ServiceTier.TIER_1);
    }
}
