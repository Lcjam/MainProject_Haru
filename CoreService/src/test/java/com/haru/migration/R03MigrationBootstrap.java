package com.haru.migration;

import java.nio.file.Files;
import java.nio.file.Path;

/** Test-only bridge to the package-private R02 migration runner. */
public final class R03MigrationBootstrap {
    private R03MigrationBootstrap() {}

    public static void initialize(String serverUrl, String database, String user, String password) throws Exception {
        validateServerUrl(serverUrl);
        if (password == null) throw new IllegalArgumentException("HARU_TEST_MYSQL_PASSWORD must not be null");
        Path sqlDirectory = locateSqlDirectory();
        RunnerConfig config = new RunnerConfig(RunnerConfig.Command.INIT, serverUrl, database, user, password, null, 30);
        new MigrationRunner(sqlDirectory, config).run();
    }

    public static void validateServerUrl(String serverUrl) {
        RunnerConfig.validateServerUrl(serverUrl);
    }

    private static Path locateSqlDirectory() {
        Path[] candidates = {Path.of("..", "sql"), Path.of("sql")};
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate.resolve("schema.sql"))) return candidate.toAbsolutePath().normalize();
        }
        throw new IllegalStateException("Cannot locate sql/schema.sql for R03 test bootstrap");
    }
}
