package ru.donskikh.incidenthub.team.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamRepository;

import java.util.Objects;

@Service
public class ListTeamsService {

    private final TeamRepository teamRepository;

    public ListTeamsService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public ListTeamsResult execute(ListTeamsQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))
        );
        Page<Team> teams = query.active() == null
                ? teamRepository.findAll(pageable)
                : teamRepository.findAllByActive(query.active(), pageable);

        return new ListTeamsResult(
                teams.getContent().stream().map(this::toItem).toList(),
                teams.getNumber(),
                teams.getSize(),
                teams.getTotalElements(),
                teams.getTotalPages(),
                teams.hasNext(),
                teams.hasPrevious()
        );
    }

    private ListTeamItem toItem(Team team) {
        return new ListTeamItem(
                team.getId(),
                team.getCode(),
                team.getName(),
                team.isActive(),
                team.getCreatedAt(),
                team.getUpdatedAt()
        );
    }
}
