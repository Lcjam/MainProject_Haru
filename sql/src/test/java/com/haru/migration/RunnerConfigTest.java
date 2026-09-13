package com.haru.migration;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RunnerConfigTest {
    @Test
    void acceptsEmptyPasswordFromEnvironment() {
        RunnerConfig config = RunnerConfig.parse(new String[]{"migrate", "--url", "jdbc:mysql://localhost:3306/",
                "--database", "haru_db", "--user", "root"}, Map.of("HARU_DB_PASSWORD", ""));
        assertEquals("", config.password());
    }

    @Test
    void requiresExplicitAdoptVersion() {
        assertThrows(IllegalArgumentException.class, () -> RunnerConfig.parse(new String[]{"adopt", "--url",
                "jdbc:mysql://localhost:3306/", "--database", "haru_db", "--user", "root"},
                Map.of("HARU_DB_PASSWORD", "secret")));
    }

    @Test
    void rejectsDatabasePathAndUrlCredentials() {
        assertThrows(IllegalArgumentException.class, () -> RunnerConfig.validateServerUrl("jdbc:mysql://localhost:3306/haru_db"));
        assertThrows(IllegalArgumentException.class, () -> RunnerConfig.validateServerUrl("jdbc:mysql://root@localhost:3306/"));
        assertThrows(IllegalArgumentException.class, () -> RunnerConfig.validateServerUrl("jdbc:mysql://localhost:3306/?password=secret"));
    }

    @Test
    void passwordArgumentsAreRejectedWithoutEchoingTheirValue() {
        String secret = "r02-do-not-print-this";
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> RunnerConfig.parse(new String[]{"migrate", "--url", "jdbc:mysql://localhost:3306/",
                                "--database", "haru_db", "--user", "root", "--password", secret},
                        Map.of("HARU_DB_PASSWORD", "")));
        org.junit.jupiter.api.Assertions.assertFalse(failure.getMessage().contains(secret));
    }
}
