package ru.donskikh.incidenthub.team.web;

import org.springframework.stereotype.Component;
import ru.donskikh.incidenthub.team.application.CreateTeamCommand;
import ru.donskikh.incidenthub.team.application.CreateTeamResult;
import ru.donskikh.incidenthub.team.application.ListTeamItem;
import ru.donskikh.incidenthub.team.application.ListTeamsQuery;
import ru.donskikh.incidenthub.team.application.ListTeamsResult;

@Component
public class TeamWebMapper {

    public CreateTeamCommand toCommand(CreateTeamRequest request) {
        return new CreateTeamCommand(request.code(), request.name());
    }

    public ListTeamsQuery toQuery(int page, int size, Boolean active) {
        return new ListTeamsQuery(page, size, active);
    }

    public CreateTeamResponse toResponse(CreateTeamResult result) {
        return new CreateTeamResponse(result.teamId(), result.code(), result.active());
    }

    public ListTeamsResponse toResponse(ListTeamsResult result) {
        return new ListTeamsResponse(
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    private TeamListItemResponse toResponse(ListTeamItem item) {
        return new TeamListItemResponse(
                item.id(),
                item.code(),
                item.name(),
                item.active(),
                item.createdAt(),
                item.updatedAt()
        );
    }
}
