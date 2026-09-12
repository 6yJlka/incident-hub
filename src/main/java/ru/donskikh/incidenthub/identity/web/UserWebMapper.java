package ru.donskikh.incidenthub.identity.web;

import org.springframework.stereotype.Component;
import ru.donskikh.incidenthub.identity.application.CreateUserCommand;
import ru.donskikh.incidenthub.identity.application.CreateUserResult;
import ru.donskikh.incidenthub.identity.application.ListUserItem;
import ru.donskikh.incidenthub.identity.application.ListUsersQuery;
import ru.donskikh.incidenthub.identity.application.ListUsersResult;

@Component
public class UserWebMapper {

    public CreateUserCommand toCommand(CreateUserRequest request) {
        return new CreateUserCommand(request.email(), request.displayName());
    }

    public ListUsersQuery toQuery(int page, int size, Boolean active) {
        return new ListUsersQuery(page, size, active);
    }

    public CreateUserResponse toResponse(CreateUserResult result) {
        return new CreateUserResponse(result.userId(), result.email(), result.active());
    }

    public ListUsersResponse toResponse(ListUsersResult result) {
        return new ListUsersResponse(
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    private UserListItemResponse toResponse(ListUserItem item) {
        return new UserListItemResponse(
                item.id(),
                item.email(),
                item.displayName(),
                item.active(),
                item.createdAt(),
                item.updatedAt()
        );
    }
}
