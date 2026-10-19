package ru.donskikh.incidenthub.catalog.application;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.PostgreSQLIntegrationTest;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceDependency;
import ru.donskikh.incidenthub.catalog.ServiceDependencyRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamRepository;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class GetAffectedServicesServiceIntegrationTest extends PostgreSQLIntegrationTest {

    @Autowired
    private GetAffectedServicesService service;

    @Autowired
    private BusinessServiceRepository businessServiceRepository;

    @Autowired
    private ServiceDependencyRepository serviceDependencyRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    private Team ownerTeam;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from incident_audit_events");
        jdbcTemplate.update("delete from incidents");
        jdbcTemplate.update("delete from service_dependencies");
        jdbcTemplate.update("delete from business_services");
        jdbcTemplate.update("delete from teams");

        ownerTeam = teamRepository.saveAndFlush(new Team("Platform", "PLATFORM"));
    }

    @Test
    void traversesLinearChainWithIncreasingDepth() {
        BusinessService serviceA = createService("A");
        BusinessService serviceB = createService("B");
        BusinessService serviceC = createService("C");
        BusinessService serviceD = createService("D");
        addDependency(serviceB, serviceA, DependencyType.SYNC);
        addDependency(serviceC, serviceB, DependencyType.ASYNC);
        addDependency(serviceD, serviceC, DependencyType.DATA);
        entityManager.clear();

        GetAffectedServicesResult result = getAffected(serviceA, 10);

        assertThat(result.items()).extracting(AffectedServiceItem::code)
                .containsExactly("B", "C", "D");
        assertThat(result.items()).extracting(AffectedServiceItem::depth)
                .containsExactly(1, 2, 3);
        assertThat(result.items()).extracting(AffectedServiceItem::dependencyType)
                .containsExactly(DependencyType.SYNC, DependencyType.ASYNC, DependencyType.DATA);
        assertThat(result.items().get(0).dependencies()).containsExactly(new AffectedDependencyItem(serviceA.getId(), DependencyType.SYNC));
        assertThat(result.items().get(1).dependencies()).containsExactly(new AffectedDependencyItem(serviceB.getId(), DependencyType.ASYNC));
        assertThat(result.items().get(2).dependencies()).containsExactly(new AffectedDependencyItem(serviceC.getId(), DependencyType.DATA));
        assertThat(result.items()).allSatisfy(item -> {
            assertThat(item.ownerTeamId()).isEqualTo(ownerTeam.getId());
            assertThat(item.ownerTeamName()).isEqualTo("Platform");
            assertThat(item.tier()).isEqualTo(ServiceTier.TIER_2);
            assertThat(item.active()).isTrue();
        });
    }

    @Test
    void returnsDiamondTargetOnceAtMinimumDepth() {
        BusinessService serviceA = createService("A");
        BusinessService serviceB = createService("B");
        BusinessService serviceC = createService("C");
        BusinessService serviceD = createService("D");
        addDependency(serviceB, serviceA, DependencyType.SYNC);
        addDependency(serviceC, serviceA, DependencyType.ASYNC);
        addDependency(serviceD, serviceB, DependencyType.DATA);
        addDependency(serviceD, serviceC, DependencyType.SYNC);
        entityManager.clear();

        GetAffectedServicesResult result = getAffected(serviceA, 10);

        assertThat(result.items()).extracting(AffectedServiceItem::code)
                .containsExactly("B", "C", "D");
        assertThat(result.items()).filteredOn(item -> item.code().equals("D"))
                .singleElement()
                .extracting(AffectedServiceItem::depth)
                .isEqualTo(2);
        assertThat(result.items().get(2).dependencies()).containsExactlyInAnyOrder(
                new AffectedDependencyItem(serviceB.getId(), DependencyType.DATA),
                new AffectedDependencyItem(serviceC.getId(), DependencyType.SYNC));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void terminatesOnCycleAndDoesNotReturnSourceService() {
        BusinessService serviceA = createService("A");
        BusinessService serviceB = createService("B");
        BusinessService serviceC = createService("C");
        addDependency(serviceB, serviceA, DependencyType.SYNC);
        addDependency(serviceC, serviceB, DependencyType.SYNC);
        addDependency(serviceA, serviceC, DependencyType.SYNC);
        entityManager.clear();

        GetAffectedServicesResult result = getAffected(serviceA, 10);

        assertThat(result.items()).extracting(AffectedServiceItem::code)
                .containsExactly("B", "C")
                .doesNotContain("A");
        assertThat(result.items().get(0).dependencies()).containsExactly(new AffectedDependencyItem(serviceA.getId(), DependencyType.SYNC));
        assertThat(result.items().get(1).dependencies()).containsExactly(new AffectedDependencyItem(serviceB.getId(), DependencyType.SYNC));
    }

    @Test
    void limitsTraversalInsideRecursiveQueryByMaximumDepth() {
        BusinessService serviceA = createService("A");
        BusinessService serviceB = createService("B");
        BusinessService serviceC = createService("C");
        BusinessService serviceD = createService("D");
        addDependency(serviceB, serviceA, DependencyType.SYNC);
        addDependency(serviceC, serviceB, DependencyType.SYNC);
        addDependency(serviceD, serviceC, DependencyType.SYNC);
        addDependency(serviceB, serviceD, DependencyType.DATA);
        entityManager.clear();

        GetAffectedServicesResult result = getAffected(serviceA, 2);

        assertThat(result.items()).extracting(AffectedServiceItem::code)
                .containsExactly("B", "C")
                .doesNotContain("D");
        assertThat(result.items()).extracting(AffectedServiceItem::depth)
                .containsExactly(1, 2);
        assertThat(result.items()).flatExtracting(AffectedServiceItem::dependencies)
                .extracting(AffectedDependencyItem::serviceId).doesNotContain(serviceD.getId());
    }

    @Test
    void usesThreeQueriesRegardlessOfReachedServiceCount() {
        var root = createService("ROOT");
        var previous = root;
        for (int index = 1; index <= 6; index++) {
            var next = createService("NODE_" + index);
            addDependency(next, previous, DependencyType.SYNC);
            previous = next;
        }
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        for (int depth : new int[]{1, 6}) {
            statistics.clear();
            var result = getAffected(root, depth);
            assertThat(result.items()).hasSize(depth);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        }
    }

    @Test
    void returnsEmptyResultForIsolatedService() {
        BusinessService isolated = createService("ISOLATED");
        entityManager.clear();

        GetAffectedServicesResult result = getAffected(isolated, 10);

        assertThat(result.items()).isEmpty();
    }

    @Test
    void doesNotCrossIntoDisconnectedGraphComponent() {
        BusinessService serviceA = createService("A");
        BusinessService serviceB = createService("B");
        BusinessService serviceX = createService("X");
        BusinessService serviceY = createService("Y");
        addDependency(serviceB, serviceA, DependencyType.SYNC);
        addDependency(serviceY, serviceX, DependencyType.DATA);
        entityManager.clear();

        GetAffectedServicesResult result = getAffected(serviceA, 10);

        assertThat(result.items()).extracting(AffectedServiceItem::code)
                .containsExactly("B")
                .doesNotContain("X", "Y");
    }

    private BusinessService createService(String code) {
        return businessServiceRepository.saveAndFlush(new BusinessService(
                code,
                code + " service",
                code + " service description",
                ownerTeam,
                ServiceTier.TIER_2
        ));
    }

    private void addDependency(
            BusinessService dependent,
            BusinessService dependency,
            DependencyType type
    ) {
        serviceDependencyRepository.saveAndFlush(new ServiceDependency(dependent, dependency, type));
    }

    private GetAffectedServicesResult getAffected(BusinessService businessService, int maxDepth) {
        return service.get(new GetAffectedServicesQuery(businessService.getId(), maxDepth));
    }
}
