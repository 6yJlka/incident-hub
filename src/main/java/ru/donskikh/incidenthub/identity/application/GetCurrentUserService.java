package ru.donskikh.incidenthub.identity.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserNotFoundException;
import ru.donskikh.incidenthub.identity.UserRepository;

@Service
public class GetCurrentUserService {

    private final UserRepository userRepository;

    public GetCurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public GetCurrentUserResult get(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return new GetCurrentUserResult(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.isActive()
        );
    }
}
