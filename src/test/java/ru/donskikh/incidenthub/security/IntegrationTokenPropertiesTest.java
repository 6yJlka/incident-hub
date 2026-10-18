package ru.donskikh.incidenthub.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import ru.donskikh.incidenthub.alert.AlertProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationTokenPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void requiresTokenOutsideLocalProfile() {
        runner.run(context -> assertThat(context).hasFailed());
        assertThatThrownBy(() -> new IntegrationTokenProperties(" ", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("INTEGRATION_TOKEN must be configured outside the local profile");
    }

    @Test
    void bindsConfiguredTokenAndDefaultBatchSize() {
        runner.withPropertyValues("app.security.integration.token=configured-token").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(IntegrationTokenProperties.class).matches("configured-token")).isTrue();
            assertThat(context.getBean(AlertProperties.class).maxBatchSize()).isEqualTo(500);
        });
    }

    @Test
    void rejectsLocalTokenOutsideLocalSettingsAndAllowsItLocally() {
        assertThatThrownBy(() -> new IntegrationTokenProperties(IntegrationTokenProperties.LOCAL_TOKEN, false))
                .isInstanceOf(IllegalStateException.class);
        assertThat(new IntegrationTokenProperties(IntegrationTokenProperties.LOCAL_TOKEN, true)
                .matches(IntegrationTokenProperties.LOCAL_TOKEN)).isTrue();
    }

    @Test
    void comparesTokensAndRedactsTheirRepresentation() {
        var properties = new IntegrationTokenProperties("secret-token", false);
        assertThat(properties.matches("secret-token")).isTrue();
        assertThat(properties.matches("wrong")).isFalse();
        assertThat(properties.matches("")).isFalse();
        assertThat(properties.toString()).doesNotContain("secret-token").contains("REDACTED");
    }

    @Test
    void validatesAndBindsBatchSize() {
        assertThatThrownBy(() -> new AlertProperties(0)).isInstanceOf(IllegalArgumentException.class);
        runner.withPropertyValues("app.security.integration.token=configured-token", "app.alerts.max-batch-size=17")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AlertProperties.class).maxBatchSize()).isEqualTo(17);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({IntegrationTokenProperties.class, AlertProperties.class})
    static class PropertiesConfiguration {
    }
}
