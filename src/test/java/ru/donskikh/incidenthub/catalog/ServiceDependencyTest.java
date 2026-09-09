package ru.donskikh.incidenthub.catalog;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.team.Team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceDependencyTest {

    private final Team ownerTeam = new Team("Platform", "PLATFORM");
    private final BusinessService dependent = createService("CHECKOUT", "Checkout");
    private final BusinessService dependency = createService("BILLING", "Billing");

    @Test
    void createsServiceDependency() {
        ServiceDependency serviceDependency = new ServiceDependency(
                dependent,
                dependency,
                DependencyType.SYNC
        );

        assertThat(serviceDependency.getDependent()).isSameAs(dependent);
        assertThat(serviceDependency.getDependency()).isSameAs(dependency);
        assertThat(serviceDependency.getType()).isEqualTo(DependencyType.SYNC);
    }

    @Test
    void rejectsNullDependentDependencyAndType() {
        assertThatThrownBy(() -> new ServiceDependency(null, dependency, DependencyType.SYNC))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dependent must not be null");
        assertThatThrownBy(() -> new ServiceDependency(dependent, null, DependencyType.SYNC))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dependency must not be null");
        assertThatThrownBy(() -> new ServiceDependency(dependent, dependency, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type must not be null");
    }

    @Test
    void rejectsSelfReference() {
        assertThatThrownBy(() -> new ServiceDependency(dependent, dependent, DependencyType.SYNC))
                .isInstanceOf(ServiceSelfDependencyNotAllowedException.class)
                .hasMessage("A business service cannot depend on itself");
    }

    @Test
    void changesDependencyType() {
        ServiceDependency serviceDependency = new ServiceDependency(
                dependent,
                dependency,
                DependencyType.SYNC
        );

        serviceDependency.changeType(DependencyType.DATA);

        assertThat(serviceDependency.getType()).isEqualTo(DependencyType.DATA);
    }

    @Test
    void rejectsNullChangedDependencyType() {
        ServiceDependency serviceDependency = new ServiceDependency(
                dependent,
                dependency,
                DependencyType.SYNC
        );

        assertThatThrownBy(() -> serviceDependency.changeType(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type must not be null");
    }

    private BusinessService createService(String code, String name) {
        return new BusinessService(code, name, name + " service", ownerTeam, ServiceTier.TIER_1);
    }
}
