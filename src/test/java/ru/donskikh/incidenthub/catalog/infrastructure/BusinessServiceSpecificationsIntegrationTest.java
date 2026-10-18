package ru.donskikh.incidenthub.catalog.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.PostgreSQLIntegrationTest;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.catalog.ServiceDependency;
import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.application.ListBusinessServicesService;
import ru.donskikh.incidenthub.catalog.application.ListBusinessServicesQuery;
import ru.donskikh.incidenthub.catalog.application.ListBusinessServiceItem;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentStatus;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class BusinessServiceSpecificationsIntegrationTest extends PostgreSQLIntegrationTest {

    @Autowired
    private BusinessServiceRepository businessServiceRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ListBusinessServicesService listBusinessServicesService;

    private Long platformTeamId;
    private Long paymentsTeamId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from incident_audit_events");
        jdbcTemplate.update("delete from incidents");
        jdbcTemplate.update("delete from service_dependencies");
        jdbcTemplate.update("delete from business_services");
        jdbcTemplate.update("delete from teams");

        Team platformTeam = teamRepository.saveAndFlush(new Team("Platform", "PLATFORM"));
        Team paymentsTeam = teamRepository.saveAndFlush(new Team("Payments", "PAYMENTS"));
        platformTeamId = platformTeam.getId();
        paymentsTeamId = paymentsTeam.getId();

        businessServiceRepository.save(new BusinessService(
                "BILLING", "Billing", "Billing service", platformTeam, ServiceTier.TIER_1
        ));
        businessServiceRepository.save(new BusinessService(
                "CHECKOUT", "Checkout", "Checkout service", paymentsTeam, ServiceTier.TIER_1
        ));
        BusinessService ledger = new BusinessService(
                "LEDGER", "Ledger", "Ledger service", paymentsTeam, ServiceTier.TIER_2
        );
        ledger.deactivate();
        businessServiceRepository.save(ledger);
        businessServiceRepository.saveAndFlush(new BusinessService(
                "NOTIFICATIONS", "Notifications", "Notification service", platformTeam, ServiceTier.TIER_3
        ));
        entityManager.clear();
    }

    @Test
    void filtersByEachOptionalField() {
        Page<BusinessService> byOwner = findAll(platformTeamId, null, null, 0, 10);
        Page<BusinessService> byTier = findAll(null, ServiceTier.TIER_1, null, 0, 10);
        Page<BusinessService> byActive = findAll(null, null, false, 0, 10);

        assertThat(byOwner.getContent()).extracting(BusinessService::getCode)
                .containsExactly("BILLING", "NOTIFICATIONS");
        assertThat(byTier.getContent()).extracting(BusinessService::getCode)
                .containsExactly("BILLING", "CHECKOUT");
        assertThat(byActive.getContent()).extracting(BusinessService::getCode)
                .containsExactly("LEDGER");
    }

    @Test
    void combinesFilters() {
        Page<BusinessService> result = findAll(
                paymentsTeamId,
                ServiceTier.TIER_1,
                true,
                0,
                10
        );

        assertThat(result.getContent()).extracting(BusinessService::getCode)
                .containsExactly("CHECKOUT");
    }

    @Test
    void returnsEmptyPageForUnknownOwnerTeam() {
        Page<BusinessService> result = findAll(999_999L, null, null, 0, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void loadsOwnerTeamsWithoutAdditionalQueries() {
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        Page<BusinessService> result = findAll(null, null, null, 0, 2);
        result.getContent().forEach(businessService -> businessService.getOwnerTeam().getName());

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void listsDirectCountsAndOnlyActiveIncidentsIncludingZeroCounts() {
        BusinessService billing = findService("BILLING");
        BusinessService checkout = findService("CHECKOUT");
        BusinessService ledger = findService("LEDGER");
        BusinessService notifications = findService("NOTIFICATIONS");
        entityManager.persist(new ServiceDependency(billing, checkout, DependencyType.SYNC));
        entityManager.persist(new ServiceDependency(billing, ledger, DependencyType.DATA));
        entityManager.persist(new ServiceDependency(notifications, billing, DependencyType.ASYNC));
        entityManager.persist(new ServiceDependency(checkout, ledger, DependencyType.DATA));
        BusinessService isolated = businessServiceRepository.save(new BusinessService(
                "ISOLATED", "Isolated", "No relationships", billing.getOwnerTeam(), ServiceTier.TIER_3
        ));
        User reporter = new User("catalog-counts@example.com", "Catalog Reporter");
        entityManager.persist(reporter);
        for (IncidentStatus status : IncidentStatus.values()) {
            Incident incident = new Incident(
                    status.name(), "Incident for count test", billing, IncidentSource.MANUAL,
                    IncidentPriority.HIGH, IncidentSeverity.SEV2, reporter, billing.getOwnerTeam()
            );
            if (status == IncidentStatus.CANCELLED) {
                incident.cancel();
            } else if (status != IncidentStatus.OPEN) {
                incident.assignTo(reporter);
                if (status != IncidentStatus.ASSIGNED) {
                    incident.startProgress();
                    if (status != IncidentStatus.IN_PROGRESS) {
                        incident.resolve();
                        if (status == IncidentStatus.CLOSED) {
                            incident.close();
                        }
                    }
                }
            }
            entityManager.persist(incident);
        }
        entityManager.flush();
        entityManager.clear();

        var result = listBusinessServicesService.execute(new ListBusinessServicesQuery(0, 20, null, null, null));
        assertThat(result.items()).filteredOn(item -> item.id().equals(billing.getId())).singleElement()
                .satisfies(item -> assertCounts(item, 2, 1, 3));
        assertThat(result.items()).filteredOn(item -> item.id().equals(ledger.getId())).singleElement()
                .satisfies(item -> assertCounts(item, 0, 2, 0));
        assertThat(result.items()).filteredOn(item -> item.id().equals(isolated.getId())).singleElement()
                .satisfies(item -> assertCounts(item, 0, 0, 0));
    }

    @Test
    void listsCountsAndOwnerTeamsWithFixedQueryCountForDifferentPageSizes() {
        for (int index = 0; index < 8; index++) {
            Team team = teamRepository.save(new Team("Extra " + index, "EXTRA_" + index));
            BusinessService service = businessServiceRepository.save(new BusinessService(
                    "EXTRA_" + index, "Extra " + index, "Extra service", team, ServiceTier.TIER_2
            ));
            entityManager.persist(new ServiceDependency(service, findService("BILLING"), DependencyType.SYNC));
        }
        entityManager.flush();

        assertListQueryCount(1);
        assertListQueryCount(10);
    }

    @Test
    void returnsEmptyCatalogPageWithoutRunningAggregateQuery() {
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var result = listBusinessServicesService.execute(
                new ListBusinessServicesQuery(0, 20, 999_999L, null, null)
        );

        assertThat(result.items()).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    private void assertListQueryCount(int size) {
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var result = listBusinessServicesService.execute(new ListBusinessServicesQuery(0, size, null, null, null));

        assertThat(result.items()).hasSize(size);
        assertThat(result.totalElements()).isEqualTo(12);
        assertThat(result.items()).allSatisfy(item -> assertThat(item.ownerTeamName()).isNotBlank());
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    private BusinessService findService(String code) {
        return entityManager.createQuery("select service from BusinessService service where service.code = :code",
                        BusinessService.class)
                .setParameter("code", code)
                .getSingleResult();
    }

    private static void assertCounts(ListBusinessServiceItem item, long dependencies, long dependents, long incidents) {
        assertThat(item.dependencyCount()).isEqualTo(dependencies);
        assertThat(item.dependentCount()).isEqualTo(dependents);
        assertThat(item.activeIncidentCount()).isEqualTo(incidents);
    }

    private Page<BusinessService> findAll(
            Long ownerTeamId,
            ServiceTier tier,
            Boolean active,
            int page,
            int size
    ) {
        return businessServiceRepository.findAll(
                BusinessServiceSpecifications.withFilters(ownerTeamId, tier, active),
                pageRequest(page, size)
        );
    }

    private static Pageable pageRequest(int page, int size) {
        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))
        );
    }
}
