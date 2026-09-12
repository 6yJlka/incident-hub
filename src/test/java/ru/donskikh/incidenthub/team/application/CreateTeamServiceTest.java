package ru.donskikh.incidenthub.team.application;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamCodeAlreadyExistsException;
import ru.donskikh.incidenthub.team.TeamRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private CreateTeamService service;

    @Test
    void createsTeamWithNormalizedCode() {
        Team savedTeam = mock(Team.class);
        when(teamRepository.existsByNormalizedCode("PLATFORM")).thenReturn(false);
        when(teamRepository.saveAndFlush(any(Team.class))).thenReturn(savedTeam);
        when(savedTeam.getId()).thenReturn(7L);
        when(savedTeam.getCode()).thenReturn("PLATFORM");
        when(savedTeam.isActive()).thenReturn(true);

        CreateTeamResult result = service.create(new CreateTeamCommand("  platform  ", "Platform"));

        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("PLATFORM");
        assertThat(captor.getValue().getName()).isEqualTo("Platform");
        assertThat(result).isEqualTo(new CreateTeamResult(7L, "PLATFORM", true));
    }

    @Test
    void throwsDomainExceptionWhenNormalizedCodeAlreadyExists() {
        when(teamRepository.existsByNormalizedCode("PLATFORM")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateTeamCommand("platform", "Platform")))
                .isInstanceOf(TeamCodeAlreadyExistsException.class)
                .hasMessage("Team code already exists: PLATFORM");

        verify(teamRepository, never()).saveAndFlush(any(Team.class));
    }

    @Test
    void translatesConcurrentCodeConflictToDomainException() {
        DataIntegrityViolationException databaseException = databaseException("uk_teams_code_normalized");
        when(teamRepository.existsByNormalizedCode("PLATFORM")).thenReturn(false);
        when(teamRepository.saveAndFlush(any(Team.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.create(new CreateTeamCommand("platform", "Platform")))
                .isInstanceOf(TeamCodeAlreadyExistsException.class)
                .hasMessage("Team code already exists: PLATFORM")
                .hasCause(databaseException);
    }

    @Test
    void doesNotTranslateDifferentIntegrityConstraint() {
        DataIntegrityViolationException databaseException = databaseException("chk_teams_name_not_blank");
        when(teamRepository.existsByNormalizedCode("PLATFORM")).thenReturn(false);
        when(teamRepository.saveAndFlush(any(Team.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.create(new CreateTeamCommand("platform", "Platform")))
                .isSameAs(databaseException);
    }

    @Test
    void doesNotTranslateIntegrityViolationWithoutConstraintName() {
        DataIntegrityViolationException databaseException = databaseException(null);
        when(teamRepository.existsByNormalizedCode("PLATFORM")).thenReturn(false);
        when(teamRepository.saveAndFlush(any(Team.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.create(new CreateTeamCommand("platform", "Platform")))
                .isSameAs(databaseException);
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(teamRepository);
    }

    private static DataIntegrityViolationException databaseException(String constraintName) {
        ConstraintViolationException violation = mock(ConstraintViolationException.class);
        when(violation.getConstraintName()).thenReturn(constraintName);
        return new DataIntegrityViolationException("integrity violation", violation);
    }
}
