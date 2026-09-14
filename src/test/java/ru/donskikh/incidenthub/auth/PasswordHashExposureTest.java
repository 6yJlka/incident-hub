package ru.donskikh.incidenthub.auth;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.auth.application.LoginResult;
import ru.donskikh.incidenthub.auth.application.RegisterUserResult;
import ru.donskikh.incidenthub.auth.web.LoginResponse;
import ru.donskikh.incidenthub.auth.web.RegisterResponse;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashExposureTest {

    @Test
    void credentialsAreAbsentFromAuthenticationResultsAndResponses() {
        Stream<Class<?>> outputTypes = Stream.of(
                RegisterUserResult.class,
                LoginResult.class,
                RegisterResponse.class,
                LoginResponse.class
        );

        assertThat(outputTypes
                .flatMap(type -> Arrays.stream(type.getRecordComponents()))
                .map(RecordComponent::getName))
                .doesNotContain("password", "passwordHash");
    }
}
