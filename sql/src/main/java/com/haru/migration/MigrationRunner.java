package com.haru.migration;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

final class MigrationRunner {
    static final String HISTORY_TABLE = "haru_schema_history";
    private static final String BASELINE_VERSION = "0";

    private final Path sqlDirectory;
    private final RunnerConfig config;
    private final Path baselinePath;
    private final String baselineSql;
    private final String baselineChecksum;
    private final List<MigrationFile> migrations;
    private final Map<String, MigrationFile> migrationsByCanonicalVersion;
    private final SchemaDefinition baselineDefinition;
    private final Map<String, SchemaDefinition> definitionsByCanonicalVersion;

    MigrationRunner(Path sqlDirectory, RunnerConfig config) throws IOException {
        this.sqlDirectory = sqlDirectory.toAbsolutePath().normalize();
        this.config = config;
        baselinePath = this.sqlDirectory.resolve("schema.sql");
        baselineSql = canonicalSql(Files.readString(baselinePath, StandardCharsets.UTF_8));
        baselineChecksum = checksum(baselineSql);
        migrations = discoverMigrations(this.sqlDirectory.resolve("migrations"));
        migrationsByCanonicalVersion = new LinkedHashMap<>();
        definitionsByCanonicalVersion = new LinkedHashMap<>();
        baselineDefinition = SchemaDefinition.baseline(baselineSql);
        SchemaDefinition definition = baselineDefinition;
        for (MigrationFile migration : migrations) {
            String canonical = canonicalVersion(migration.version());
            if (canonical.equals(BASELINE_VERSION)) {
                throw new IllegalArgumentException("Migration version 0 is reserved for schema.sql");
            }
            MigrationFile duplicate = migrationsByCanonicalVersion.put(canonical, migration);
            if (duplicate != null) {
                throw new IllegalArgumentException("Duplicate numeric migration version: " + duplicate.script() + " and " + migration.script());
            }
            definition = definition.applying(migration.content());
            definitionsByCanonicalVersion.put(canonical, definition);
        }
    }

    String run() throws SQLException, IOException {
        Properties properties = new Properties();
        properties.setProperty("user", config.user());
        properties.setProperty("password", config.password());
        properties.setProperty("useUnicode", "true");
        properties.setProperty("characterEncoding", "UTF-8");
        try (Connection connection = DriverManager.getConnection(config.url(), properties)) {
            connection.setAutoCommit(true);
            if (SchemaInspector.lowerCaseTableNames(connection) != 0) {
                throw new IllegalStateException("R02 requires a MySQL server with lower_case_table_names=0");
            }
            String lockName = lockName(config.database());
            acquireLock(connection, lockName, config.lockTimeoutSeconds());
            try {
                return switch (config.command()) {
                    case INIT -> initialize(connection);
                    case ADOPT -> adopt(connection, config.adoptVersion());
                    case MIGRATE -> migrateExisting(connection);
                };
            } finally {
                releaseLock(connection, lockName);
            }
        }
    }

    private String initialize(Connection connection) throws SQLException, IOException {
        if (!SchemaInspector.databaseExists(connection, config.database())) {
            execute(connection, "CREATE DATABASE `" + config.database() +
                    "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
        connection.setCatalog(config.database());
        if (SchemaInspector.hasAnyUserObjects(connection, config.database())) {
            throw new IllegalStateException("init is empty-only; target contains schema objects");
        }
        createHistoryTable(connection);
        long baselineRank = insertRunning(connection, BASELINE_VERSION, "baseline", baselinePath.getFileName().toString(),
                baselineChecksum);
        Instant baselineStart = Instant.now();
        try {
            executeScript(connection, baselineSql, true);
            assertSchema(connection, baselineDefinition, "baseline initialization");
            markSuccess(connection, baselineRank, elapsedSince(baselineStart));
        } catch (SQLException | RuntimeException exception) {
            markFailed(connection, baselineRank, safeFailure(exception));
            throw new IllegalStateException("Baseline initialization failed; FAILED or RUNNING history blocks retry; manual diagnosis is required",
                    exception);
        }
        applyPending(connection);
        return "Initialized and migrated database " + config.database() + " to version " + latestVersion();
    }

    private String adopt(Connection connection, int requestedVersion) throws SQLException {
        requireExistingDatabase(connection);
        connection.setCatalog(config.database());
        if (historyTableExists(connection)) {
            throw new IllegalStateException("adopt requires a database without migration history");
        }
        SchemaDefinition expected = requestedVersion == 0 ? baselineDefinition : definitionForVersion("1");
        assertSchema(connection, expected, "adopt version " + requestedVersion);
        createHistoryTable(connection);
        insertCompleted(connection, BASELINE_VERSION, "baseline", baselinePath.getFileName().toString(),
                baselineChecksum, "ADOPTED", 0);
        if (requestedVersion == 1) {
            for (MigrationFile migration : migrations) {
                if (MigrationFile.compareVersions(migration.version(), "1") <= 0) {
                    insertCompleted(connection, migration.version(), migration.description(), migration.script(),
                            migration.checksum(), "ADOPTED", 0);
                }
            }
        }
        return "Adopted existing database " + config.database() + " at version " + requestedVersion +
                "; run migrate to apply later versions";
    }

    private String migrateExisting(Connection connection) throws SQLException, IOException {
        requireExistingDatabase(connection);
        connection.setCatalog(config.database());
        if (!historyTableExists(connection)) {
            throw new IllegalStateException("migration history is absent; use adopt --version 0|1 after verifying the existing schema");
        }
        Map<String, HistoryRow> history = loadAndValidateHistory(connection);
        assertSchema(connection, definitionForHistory(history), "recorded migration history");
        applyPending(connection);
        return "Database " + config.database() + " is at version " + latestVersion();
    }

    private void applyPending(Connection connection) throws SQLException, IOException {
        Map<String, HistoryRow> history = loadAndValidateHistory(connection);
        for (MigrationFile migration : migrations) {
            String canonical = canonicalVersion(migration.version());
            if (history.containsKey(canonical)) continue;
            long rank = insertRunning(connection, migration.version(), migration.description(), migration.script(), migration.checksum());
            Instant start = Instant.now();
            try {
                executeScript(connection, migration.content(), false);
                assertSchema(connection, definitionsByCanonicalVersion.get(canonical), "migration " + migration.script());
                markSuccess(connection, rank, elapsedSince(start));
            } catch (SQLException | RuntimeException exception) {
                markFailed(connection, rank, safeFailure(exception));
                throw new IllegalStateException("Migration " + migration.script() +
                        " failed; FAILED or RUNNING history blocks retry; manual diagnosis is required", exception);
            }
        }
    }

    private Map<String, HistoryRow> loadAndValidateHistory(Connection connection) throws SQLException {
        Map<String, HistoryRow> rows = new LinkedHashMap<>();
        List<String> encountered = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT version, script, checksum, status FROM `" + HISTORY_TABLE + "` ORDER BY installed_rank")) {
            while (result.next()) {
                HistoryRow row = new HistoryRow(result.getString(1), result.getString(2), result.getString(3), result.getString(4));
                if (!row.status.equals("SUCCESS") && !row.status.equals("ADOPTED")) {
                    throw new IllegalStateException("Migration history contains " + row.status + " entry for " + row.script +
                            "; automatic repair or retry is forbidden");
                }
                String canonical = canonicalVersion(row.version);
                encountered.add(canonical);
                if (rows.put(canonical, row) != null) {
                    throw new IllegalStateException("Migration history contains duplicate numeric version " + row.version);
                }
            }
        }
        HistoryRow baseline = rows.get(BASELINE_VERSION);
        if (baseline == null || !baseline.version.equals(BASELINE_VERSION) ||
                !baseline.script.equals(baselinePath.getFileName().toString()) ||
                !baseline.checksum.equals(baselineChecksum)) {
            throw new IllegalStateException("Baseline history is absent or its checksum/script does not match schema.sql");
        }
        for (Map.Entry<String, HistoryRow> entry : rows.entrySet()) {
            if (entry.getKey().equals(BASELINE_VERSION)) continue;
            MigrationFile migration = migrationsByCanonicalVersion.get(entry.getKey());
            if (migration == null) throw new IllegalStateException("History contains unknown migration version " + entry.getValue().version);
            if (!migration.version().equals(entry.getValue().version) || !migration.script().equals(entry.getValue().script) ||
                    !migration.checksum().equals(entry.getValue().checksum)) {
                throw new IllegalStateException("Checksum or script mismatch for migration " + migration.version());
            }
        }
        List<String> expectedPrefix = new ArrayList<>();
        expectedPrefix.add(BASELINE_VERSION);
        for (MigrationFile migration : migrations) {
            String canonical = canonicalVersion(migration.version());
            if (!rows.containsKey(canonical)) break;
            expectedPrefix.add(canonical);
        }
        if (!encountered.equals(expectedPrefix) || rows.size() != expectedPrefix.size()) {
            throw new IllegalStateException("Migration history is not a contiguous, ordered prefix of repository migrations");
        }
        return rows;
    }

    private SchemaDefinition definitionForHistory(Map<String, HistoryRow> history) {
        SchemaDefinition current = baselineDefinition;
        for (MigrationFile migration : migrations) {
            if (history.containsKey(canonicalVersion(migration.version()))) {
                current = definitionsByCanonicalVersion.get(canonicalVersion(migration.version()));
            } else {
                break;
            }
        }
        return current;
    }

    private SchemaDefinition definitionForVersion(String version) {
        SchemaDefinition definition = definitionsByCanonicalVersion.get(canonicalVersion(version));
        if (definition == null) throw new IllegalStateException("Repository does not contain migration version " + version);
        return definition;
    }

    private void assertSchema(Connection connection, SchemaDefinition expected, String context) throws SQLException {
        SchemaDefinition actual = SchemaInspector.inspect(connection, config.database(), HISTORY_TABLE);
        List<String> differences = expected.differences(actual);
        for (String object : SchemaInspector.nonTableObjects(connection, config.database())) {
            differences.add("unexpected non-table object: " + object);
        }
        if (!differences.isEmpty()) {
            int shown = Math.min(12, differences.size());
            throw new IllegalStateException("Schema mismatch during " + context + ": " +
                    String.join("; ", differences.subList(0, shown)) +
                    (differences.size() > shown ? "; and " + (differences.size() - shown) + " more" : ""));
        }
    }

    private void executeScript(Connection connection, String sql, boolean baseline) throws SQLException {
        List<String> statements = SqlScript.statements(sql);
        for (String statement : statements) SqlScript.validateForIsolatedTarget(statement, baseline);
        for (String statement : statements) {
            if (SqlScript.isCreateDatabase(statement)) continue;
            if (SqlScript.isUseFor(statement, config.database())) continue;
            execute(connection, statement);
        }
        if (!config.database().equals(connection.getCatalog())) {
            throw new IllegalStateException("SQL changed the active database unexpectedly");
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.execute(sql); }
    }

    private void createHistoryTable(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE `haru_schema_history` (
                  `installed_rank` BIGINT NOT NULL AUTO_INCREMENT,
                  `version` VARCHAR(50) NOT NULL,
                  `description` VARCHAR(200) NOT NULL,
                  `script` VARCHAR(255) NOT NULL,
                  `checksum` CHAR(64) NOT NULL,
                  `installed_on` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `execution_ms` INT NOT NULL DEFAULT 0,
                  `status` VARCHAR(16) NOT NULL,
                  `error_message` VARCHAR(512) DEFAULT NULL,
                  PRIMARY KEY (`installed_rank`),
                  UNIQUE KEY `uq_haru_schema_history_version` (`version`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                """);
    }

    private boolean historyTableExists(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM information_schema.TABLES
                WHERE TABLE_SCHEMA=? AND TABLE_NAME=? AND TABLE_TYPE='BASE TABLE'
                """)) {
            statement.setString(1, config.database());
            statement.setString(2, HISTORY_TABLE);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    private void requireExistingDatabase(Connection connection) throws SQLException {
        if (!SchemaInspector.databaseExists(connection, config.database())) {
            throw new IllegalStateException("Target database does not exist; use init only for a new empty database");
        }
    }

    private long insertRunning(Connection connection, String version, String description, String script,
                               String checksum) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO `haru_schema_history`
                  (version, description, script, checksum, status)
                VALUES (?, ?, ?, ?, 'RUNNING')
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, version);
            statement.setString(2, description);
            statement.setString(3, script);
            statement.setString(4, checksum);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("History rank was not generated");
                return keys.getLong(1);
            }
        }
    }

    private void insertCompleted(Connection connection, String version, String description, String script,
                                 String checksum, String status, int elapsed) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO `haru_schema_history`
                  (version, description, script, checksum, execution_ms, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, version);
            statement.setString(2, description);
            statement.setString(3, script);
            statement.setString(4, checksum);
            statement.setInt(5, elapsed);
            statement.setString(6, status);
            statement.executeUpdate();
        }
    }

    private void markSuccess(Connection connection, long rank, int elapsed) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE `haru_schema_history` SET status='SUCCESS', execution_ms=? WHERE installed_rank=? AND status='RUNNING'")) {
            statement.setInt(1, elapsed);
            statement.setLong(2, rank);
            if (statement.executeUpdate() != 1) throw new SQLException("Could not finalize migration history");
        }
    }

    private void markFailed(Connection connection, long rank, String message) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE `haru_schema_history` SET status='FAILED', error_message=? WHERE installed_rank=? AND status='RUNNING'")) {
            statement.setString(1, message);
            statement.setLong(2, rank);
            statement.executeUpdate();
        } catch (SQLException ignored) {
            // The original failure remains primary. A RUNNING row is itself fail-closed on the next execution.
        }
    }

    private static void acquireLock(Connection connection, String name, int timeout) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, name);
            statement.setInt(2, timeout);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new IllegalStateException("Could not acquire the database migration lock");
                }
            }
        }
    }

    private static void releaseLock(Connection connection, String name) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, name);
            statement.executeQuery().close();
        } catch (SQLException ignored) {
            // Closing the same JDBC session releases a lock even if this explicit release fails.
        }
    }

    private String latestVersion() {
        return migrations.isEmpty() ? BASELINE_VERSION : migrations.get(migrations.size() - 1).version();
    }

    private static List<MigrationFile> discoverMigrations(Path directory) throws IOException {
        List<MigrationFile> result = new ArrayList<>();
        try (Stream<Path> paths = Files.list(directory)) {
            for (Path path : paths.filter(value -> value.getFileName().toString().endsWith(".sql")).toList()) {
                String content = canonicalSql(Files.readString(path, StandardCharsets.UTF_8));
                result.add(MigrationFile.from(path, checksum(content), content));
            }
        }
        result.sort(MigrationFile.numericOrder());
        return List.copyOf(result);
    }

    static String canonicalVersion(String version) {
        String[] parts = version.split("\\.");
        List<String> canonical = new ArrayList<>();
        for (String part : parts) canonical.add(new BigInteger(part).toString());
        while (canonical.size() > 1 && canonical.get(canonical.size() - 1).equals("0")) {
            canonical.remove(canonical.size() - 1);
        }
        return String.join(".", canonical);
    }

    private static String lockName(String database) {
        return "haru:r02:" + sha256(database.getBytes(StandardCharsets.UTF_8)).substring(0, 32);
    }

    private static int elapsedSince(Instant start) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, Duration.between(start, Instant.now()).toMillis()));
    }

    private static String canonicalSql(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String checksum(String content) {
        return sha256(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String safeFailure(Exception exception) {
        if (exception instanceof SQLException sqlException) {
            return "SQLState=" + sqlException.getSQLState() + ", vendorCode=" + sqlException.getErrorCode();
        }
        return exception.getClass().getSimpleName();
    }

    private record HistoryRow(String version, String script, String checksum, String status) {}
}
