package ru.donskikh.incidenthub.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues("app.security.jwt.access-token-ttl=PT1H");

    @Test
    void rejectsMissingSecretDuringConfigurationBinding() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .rootCause()
                    .hasMessage("JWT_SECRET must be configured outside the local profile");
        });
    }

    @Test
    void acceptsConfiguredSecret() {
        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues(
                        "app.security.jwt.secret=aW5jaWRlbnQtaHViLXRlc3Qtc2VjcmV0LW11c3QtYmUtYXQtbGVhc3QtMzItYnl0ZXM=",
                        "app.security.jwt.access-token-ttl=PT1H"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void rejectsRepositorySecretOutsideLocalProfile() {
        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues(
                        "app.security.jwt.secret=" + JwtProperties.LOCAL_DEVELOPMENT_SECRET,
                        "app.security.jwt.access-token-ttl=PT1H"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .rootCause()
                            .hasMessage("The repository JWT secret may only be used with the local profile");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class PropertiesConfiguration {
    }
}
