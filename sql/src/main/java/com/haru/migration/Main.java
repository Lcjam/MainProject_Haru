package com.haru.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            RunnerConfig config = RunnerConfig.parse(args, System.getenv());
            String result = new MigrationRunner(findSqlDirectory(), config).run();
            System.out.println(result);
        } catch (Exception exception) {
            System.err.println("Migration refused: " + safeMessage(exception));
            System.exit(2);
        }
    }

    private static Path findSqlDirectory() {
        String configured = System.getenv("HARU_SQL_DIR");
        if (configured != null && !configured.isBlank()) return Path.of(configured);
        Path nested = Path.of("sql");
        if (Files.isRegularFile(nested.resolve("schema.sql"))) return nested;
        Path current = Path.of(".");
        if (Files.isRegularFile(current.resolve("schema.sql"))) return current;
        throw new IllegalStateException("Cannot find sql/schema.sql; run from the repository root or set HARU_SQL_DIR");
    }

    private static String safeMessage(Exception exception) {
        if (exception instanceof SQLException sqlException) {
            return "database operation failed (SQLState=" + sqlException.getSQLState() +
                    ", vendorCode=" + sqlException.getErrorCode() + ")";
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
