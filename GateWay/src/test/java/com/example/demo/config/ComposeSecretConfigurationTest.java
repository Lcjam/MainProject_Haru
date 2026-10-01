package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ComposeSecretConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SecretProbe.class);

    @Test
    void composeProfileRequiresJwtSecret() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getResourceAsStream("/application-compose.properties"));
        assertThat(properties.getProperty("jwt.secret")).isEqualTo("${JWT_SECRET}");

        runner.withPropertyValues("jwt.secret=${S05_UNSET_JWT_SECRET}").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "Could not resolve placeholder 'S05_UNSET_JWT_SECRET' in value \"${S05_UNSET_JWT_SECRET}\"");
        });
    }

    @Test
    void composeProfileAcceptsExplicitJwtSecret() {
        runner.withPropertyValues("jwt.secret=explicit-test-secret-at-least-32-bytes")
                .run(context -> assertThat(context).getBean("jwtSecret")
                        .isEqualTo("explicit-test-secret-at-least-32-bytes"));
    }

    @Configuration(proxyBeanMethods = false)
    static class SecretProbe {
        @Bean
        static PropertySourcesPlaceholderConfigurer placeholders() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        String jwtSecret(@Value("${jwt.secret}") String secret) {
            return secret;
        }
    }
}
