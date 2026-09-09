package ru.donskikh.incidenthub.catalog.application;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceCodeAlreadyExistsException;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamNotFoundException;
import ru.donskikh.incidenthub.team.TeamRepository;

import java.util.Objects;

@Service
public class CreateBusinessServiceService {

    private static final String CODE_UNIQUE_CONSTRAINT = "uk_business_services_code_normalized";

    private final TeamRepository teamRepository;
    private final BusinessServiceRepository businessServiceRepository;

    public CreateBusinessServiceService(
            TeamRepository teamRepository,
            BusinessServiceRepository businessServiceRepository
    ) {
        this.teamRepository = teamRepository;
        this.businessServiceRepository = businessServiceRepository;
    }

    @Transactional
    public CreateBusinessServiceResult create(CreateBusinessServiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Team ownerTeam = teamRepository.findById(command.ownerTeamId())
                .orElseThrow(() -> new TeamNotFoundException(command.ownerTeamId()));
        BusinessService businessService = new BusinessService(
                command.code(),
                command.name(),
                command.description(),
                ownerTeam,
                command.tier()
        );

        if (businessServiceRepository.existsByNormalizedCode(businessService.getCode())) {
            throw new BusinessServiceCodeAlreadyExistsException(businessService.getCode());
        }

        BusinessService savedBusinessService;
        try {
            savedBusinessService = businessServiceRepository.saveAndFlush(businessService);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException constraintViolation
                    && CODE_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                throw new BusinessServiceCodeAlreadyExistsException(businessService.getCode(), exception);
            }
            throw exception;
        }

        return new CreateBusinessServiceResult(
                savedBusinessService.getId(),
                savedBusinessService.getCode(),
                savedBusinessService.getTier(),
                savedBusinessService.isActive()
        );
    }
}
