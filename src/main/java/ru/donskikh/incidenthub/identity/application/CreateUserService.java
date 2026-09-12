package ru.donskikh.incidenthub.identity.application;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserEmailAlreadyExistsException;
import ru.donskikh.incidenthub.identity.UserRepository;

import java.util.Objects;

@Service
public class CreateUserService {

    private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_users_email_lower";

    private final UserRepository userRepository;

    public CreateUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public CreateUserResult create(CreateUserCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        User user = new User(command.email(), command.displayName());
        if (userRepository.existsByNormalizedEmail(user.getEmail())) {
            throw new UserEmailAlreadyExistsException(user.getEmail());
        }

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException constraintViolation
                    && EMAIL_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                throw new UserEmailAlreadyExistsException(user.getEmail(), exception);
            }
            throw exception;
        }

        return new CreateUserResult(savedUser.getId(), savedUser.getEmail(), savedUser.isActive());
    }
}
