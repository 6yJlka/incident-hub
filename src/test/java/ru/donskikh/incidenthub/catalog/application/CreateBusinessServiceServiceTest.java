package ru.donskikh.incidenthub.catalog.application;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceCodeAlreadyExistsException;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamNotFoundException;
import ru.donskikh.incidenthub.team.TeamRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBusinessServiceServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private BusinessServiceRepository businessServiceRepository;

    @InjectMocks
    private CreateBusinessServiceService service;

    @Test
    void createsBusinessServiceWithNormalizedCode() {
        Team ownerTeam = new Team("Platform", "PLATFORM");
        BusinessService savedBusinessService = org.mockito.Mockito.mock(BusinessService.class);
        when(teamRepository.findById(7L)).thenReturn(Optional.of(ownerTeam));
        when(businessServiceRepository.existsByNormalizedCode("BILLING-API")).thenReturn(false);
        when(businessServiceRepository.saveAndFlush(any(BusinessService.class)))
                .thenReturn(savedBusinessService);
        when(savedBusinessService.getId()).thenReturn(42L);
        when(savedBusinessService.getCode()).thenReturn("BILLING-API");
        when(savedBusinessService.getTier()).thenReturn(ServiceTier.TIER_1);
        when(savedBusinessService.isActive()).thenReturn(true);

        CreateBusinessServiceResult result = service.create(command());

        ArgumentCaptor<BusinessService> serviceCaptor = ArgumentCaptor.forClass(BusinessService.class);
        verify(businessServiceRepository).saveAndFlush(serviceCaptor.capture());
        BusinessService created = serviceCaptor.getValue();
        assertThat(created.getCode()).isEqualTo("BILLING-API");
        assertThat(created.getName()).isEqualTo("Billing API");
        assertThat(created.getDescription()).isEqualTo("Processes payments");
        assertThat(created.getOwnerTeam()).isSameAs(ownerTeam);
        assertThat(created.getTier()).isEqualTo(ServiceTier.TIER_1);
        assertThat(result.businessServiceId()).isEqualTo(42L);
        assertThat(result.code()).isEqualTo("BILLING-API");
        assertThat(result.tier()).isEqualTo(ServiceTier.TIER_1);
        assertThat(result.active()).isTrue();
    }

    @Test
    void throwsWhenOwnerTeamDoesNotExist() {
        when(teamRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(TeamNotFoundException.class)
                .hasMessage("Team not found: 7");

        verifyNoInteractions(businessServiceRepository);
    }

    @Test
    void throwsDomainExceptionWhenNormalizedCodeAlreadyExists() {
        Team ownerTeam = new Team("Platform", "PLATFORM");
        when(teamRepository.findById(7L)).thenReturn(Optional.of(ownerTeam));
        when(businessServiceRepository.existsByNormalizedCode("BILLING-API")).thenReturn(true);

        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(BusinessServiceCodeAlreadyExistsException.class)
                .hasMessage("Business service code already exists: BILLING-API");

        verify(businessServiceRepository, never()).saveAndFlush(any(BusinessService.class));
    }

    @Test
    void translatesConcurrentCodeConflictToDomainException() {
        Team ownerTeam = new Team("Platform", "PLATFORM");
        DataIntegrityViolationException databaseException = databaseException(
                "uk_business_services_code_normalized"
        );
        when(teamRepository.findById(7L)).thenReturn(Optional.of(ownerTeam));
        when(businessServiceRepository.existsByNormalizedCode("BILLING-API")).thenReturn(false);
        when(businessServiceRepository.saveAndFlush(any(BusinessService.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(BusinessServiceCodeAlreadyExistsException.class)
                .hasMessage("Business service code already exists: BILLING-API")
                .hasCause(databaseException);
    }

    @Test
    void doesNotTranslateDifferentIntegrityConstraint() {
        Team ownerTeam = new Team("Platform", "PLATFORM");
        DataIntegrityViolationException databaseException = databaseException(
                "fk_business_services_owner_team"
        );
        when(teamRepository.findById(7L)).thenReturn(Optional.of(ownerTeam));
        when(businessServiceRepository.existsByNormalizedCode("BILLING-API")).thenReturn(false);
        when(businessServiceRepository.saveAndFlush(any(BusinessService.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.create(command())).isSameAs(databaseException);
    }

    @Test
    void doesNotTranslateIntegrityViolationWithoutConstraintName() {
        Team ownerTeam = new Team("Platform", "PLATFORM");
        DataIntegrityViolationException databaseException = databaseException(null);
        when(teamRepository.findById(7L)).thenReturn(Optional.of(ownerTeam));
        when(businessServiceRepository.existsByNormalizedCode("BILLING-API")).thenReturn(false);
        when(businessServiceRepository.saveAndFlush(any(BusinessService.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.create(command())).isSameAs(databaseException);
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(teamRepository, businessServiceRepository);
    }

    private static CreateBusinessServiceCommand command() {
        return new CreateBusinessServiceCommand(
                "  billing-api  ",
                "Billing API",
                "Processes payments",
                7L,
                ServiceTier.TIER_1
        );
    }

    private static DataIntegrityViolationException databaseException(String constraintName) {
        ConstraintViolationException constraintViolation = mock(ConstraintViolationException.class);
        when(constraintViolation.getConstraintName()).thenReturn(constraintName);
        return new DataIntegrityViolationException("integrity violation", constraintViolation);
    }
}
