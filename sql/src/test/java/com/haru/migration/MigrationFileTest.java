package com.haru.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationFileTest {
    @Test
    void ordersNumericVersionsRatherThanFilenames() {
        assertTrue(MigrationFile.compareVersions("2", "10") < 0);
        assertEquals("1", MigrationRunner.canonicalVersion("01.0"));
    }

    @Test
    void rejectsBaselineAliasesAndDuplicateNumericVersions(@TempDir Path temporary) throws Exception {
        Path migrations = temporary.resolve("migrations");
        Files.createDirectories(migrations);
        Files.writeString(temporary.resolve("schema.sql"), "", StandardCharsets.UTF_8);
        Files.writeString(migrations.resolve("V0__reserved.sql"), "SELECT SLEEP(0);", StandardCharsets.UTF_8);
        RunnerConfig config = new RunnerConfig(RunnerConfig.Command.MIGRATE, "jdbc:mysql://localhost:3306/", "haru_test",
                "test", "", null, 0);
        assertThrows(IllegalArgumentException.class, () -> new MigrationRunner(temporary, config));

        Files.delete(migrations.resolve("V0__reserved.sql"));
        Files.writeString(migrations.resolve("V1__one.sql"), "SELECT SLEEP(0);", StandardCharsets.UTF_8);
        Files.writeString(migrations.resolve("V01.0__alias.sql"), "SELECT SLEEP(0);", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> new MigrationRunner(temporary, config));
    }
}
