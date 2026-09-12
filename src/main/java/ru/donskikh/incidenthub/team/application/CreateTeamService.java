package ru.donskikh.incidenthub.team.application;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamCodeAlreadyExistsException;
import ru.donskikh.incidenthub.team.TeamRepository;

import java.util.Objects;

@Service
public class CreateTeamService {

    private static final String CODE_UNIQUE_CONSTRAINT = "uk_teams_code_normalized";

    private final TeamRepository teamRepository;

    public CreateTeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Transactional
    public CreateTeamResult create(CreateTeamCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Team team = new Team(command.name(), command.code());
        if (teamRepository.existsByNormalizedCode(team.getCode())) {
            throw new TeamCodeAlreadyExistsException(team.getCode());
        }

        Team savedTeam;
        try {
            savedTeam = teamRepository.saveAndFlush(team);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException constraintViolation
                    && CODE_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                throw new TeamCodeAlreadyExistsException(team.getCode(), exception);
            }
            throw exception;
        }

        return new CreateTeamResult(savedTeam.getId(), savedTeam.getCode(), savedTeam.isActive());
    }
}
