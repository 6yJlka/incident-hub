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
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class BusinessServiceSpecificationsIntegrationTest {

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

    private Long platformTeamId;
    private Long paymentsTeamId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from service_dependencies");
        jdbcTemplate.update("delete from business_services");
        jdbcTemplate.update("delete from incident_audit_events");
        jdbcTemplate.update("delete from incidents");
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
