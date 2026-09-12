package ru.donskikh.incidenthub.team.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTeamsServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private ListTeamsService service;

    @Test
    void listsAllTeamsWithStableSortingAndMetadata() {
        Team team = team();
        when(teamRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(team), PageRequest.of(1, 5), 8));

        ListTeamsResult result = service.execute(new ListTeamsQuery(1, 5, null));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(teamRepository).findAll(captor.capture());
        assertThat(captor.getValue().getSort().toList()).extracting(Sort.Order::getProperty)
                .containsExactly("name", "id");
        assertThat(result.items().getFirst()).isEqualTo(new ListTeamItem(
                7L,
                "PLATFORM",
                "Platform",
                true,
                Instant.parse("2026-09-01T10:00:00Z"),
                Instant.parse("2026-09-02T11:00:00Z")
        ));
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(6);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void filtersByActive() {
        when(teamRepository.findAllByActive(false, PageRequest.of(
                0, 20, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))
        ))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.execute(new ListTeamsQuery(0, 20, false));

        verify(teamRepository).findAllByActive(false, PageRequest.of(
                0, 20, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))
        ));
    }

    @Test
    void rejectsNullQuery() {
        assertThatThrownBy(() -> service.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("query must not be null");

        verifyNoInteractions(teamRepository);
    }

    private static Team team() {
        Team team = mock(Team.class);
        when(team.getId()).thenReturn(7L);
        when(team.getCode()).thenReturn("PLATFORM");
        when(team.getName()).thenReturn("Platform");
        when(team.isActive()).thenReturn(true);
        when(team.getCreatedAt()).thenReturn(Instant.parse("2026-09-01T10:00:00Z"));
        when(team.getUpdatedAt()).thenReturn(Instant.parse("2026-09-02T11:00:00Z"));
        return team;
    }
}
