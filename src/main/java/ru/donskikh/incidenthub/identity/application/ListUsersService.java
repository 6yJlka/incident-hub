package ru.donskikh.incidenthub.identity.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserRepository;

import java.util.Objects;

@Service
public class ListUsersService {

    private final UserRepository userRepository;

    public ListUsersService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ListUsersResult execute(ListUsersQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Order.asc("displayName"), Sort.Order.asc("id"))
        );
        Page<User> users = query.active() == null
                ? userRepository.findAll(pageable)
                : userRepository.findAllByActive(query.active(), pageable);

        return new ListUsersResult(
                users.getContent().stream().map(this::toItem).toList(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.hasNext(),
                users.hasPrevious()
        );
    }

    private ListUserItem toItem(User user) {
        return new ListUserItem(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
