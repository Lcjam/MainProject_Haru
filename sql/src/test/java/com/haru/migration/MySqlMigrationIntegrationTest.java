package com.haru.migration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("mysql")
class MySqlMigrationIntegrationTest {
    private static final String PREFIX = "haru_r02_";

    @Test
    void mapperPhysicalIdentifiersResolveOnCaseSensitiveMySql() throws Exception {
        var references = MapperPhysicalNameTest.tableReferences();
        assertFalse(references.isEmpty());
        withDatabase(database -> {
            runner(sqlDirectory(), "init", database, 10, null).run();
            assertEquals(0, serverScalar("SELECT @@lower_case_table_names"));
            for (String table : references.stream().map(MapperPhysicalNameTest.TableReference::table).distinct().toList()) {
                executeIn(database, "SELECT 1 FROM `" + table + "` LIMIT 0");
            }
        });
    }

    @Test
    void adoptionRejectsUnmodelledIndexColumnAndCheckAttributes() throws Exception {
        for (String mutation : new String[]{
                "ALTER TABLE `Users` ALTER COLUMN `nickname` SET INVISIBLE",
                "ALTER TABLE `Users` ADD CONSTRAINT `r02_check` CHECK (CHAR_LENGTH(`nickname`) > 0)",
                "ALTER TABLE `Users` DROP INDEX `nickname`, ADD UNIQUE KEY `nickname` (`nickname`(8))",
                "ALTER TABLE `Users` ALTER INDEX `nickname` INVISIBLE"}) {
            withDatabase(database -> {
                createDatabase(database);
                applyRaw(database, sqlDirectory().resolve("schema.sql"));
                executeIn(database, mutation);
                assertThrows(IllegalStateException.class, () -> runner(sqlDirectory(), "adopt", database, 10, 0).run());
                assertEquals(0, scalar(database, "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='" +
                        database + "' AND TABLE_NAME='haru_schema_history'"));
            });
        }
    }

    @Test
    void successfulDdlWithSchemaMismatchIsNeverMarkedSuccess(@TempDir Path temporary) throws Exception {
        Path copy = copySql(temporary);
        Files.writeString(copy.resolve("migrations/V2__schema_assert_probe.sql"), "SELECT SLEEP(2);", StandardCharsets.UTF_8);
        withDatabase(database -> {
            CompletableFuture<Exception> first = CompletableFuture.supplyAsync(() -> {
                try { runner(copy, "init", database, 10, null).run(); return null; }
                catch (Exception exception) { return exception; }
            });
            try {
                awaitHistory(database, "2", "RUNNING");
                // A non-cooperating writer is outside GET_LOCK's advisory protection.
                executeIn(database, "CREATE TABLE `r02_unexpected` (`id` INT)");
                assertTrue(first.get(10, TimeUnit.SECONDS) instanceof IllegalStateException);
                assertEquals("FAILED", historyStatus(database, "2"));
                assertThrows(IllegalStateException.class, () -> runner(copy, "migrate", database, 10, null).run());
            } finally {
                first.get(10, TimeUnit.SECONDS);
            }
        });
    }

    @Test
    void emptyInitReachesV1AndRerunIsSafe() throws Exception {
        withDatabase(database -> {
            assertEquals(0, serverScalar("SELECT @@lower_case_table_names"));
            boolean haruDbBefore = databaseExists("haru_db");
            String result = runner(sqlDirectory(), "init", database, 10, null).run();
            assertTrue(result.contains("version 1"));
            assertEquals(28, baseTableCount(database));
            assertEquals(2, historyCount(database));
            assertThrows(IllegalStateException.class, () -> runner(sqlDirectory(), "init", database, 10, null).run());
            runner(sqlDirectory(), "migrate", database, 10, null).run();
            assertEquals(2, historyCount(database));
            assertEquals(haruDbBefore, databaseExists("haru_db"), "schema.sql must not create its hard-coded database");
        });
    }

    @Test
    void baselineAdoptionPreservesDataThenMigrates() throws Exception {
        withDatabase(database -> {
            createDatabase(database);
            applyRaw(database, sqlDirectory().resolve("schema.sql"));
            executeIn(database, "INSERT INTO `Users` (email,password_hash,name,nickname,login_method) " +
                    "VALUES ('preserved@example.test','x','Preserved','r02-preserved','EMAIL')");
            runner(sqlDirectory(), "adopt", database, 10, 0).run();
            assertEquals("ADOPTED", historyStatus(database, "0"));
            runner(sqlDirectory(), "migrate", database, 10, null).run();
            assertEquals(1, scalar(database, "SELECT COUNT(*) FROM `Users` WHERE email='preserved@example.test'"));
            assertEquals("SUCCESS", historyStatus(database, "1"));
        });
    }

    @Test
    void v1AdoptionRecordsHistoryWithoutReapplyingDdl() throws Exception {
        withDatabase(database -> {
            createDatabase(database);
            applyRaw(database, sqlDirectory().resolve("schema.sql"));
            applyRaw(database, sqlDirectory().resolve("migrations/V1__board_location_tables.sql"));
            runner(sqlDirectory(), "adopt", database, 10, 1).run();
            assertEquals("ADOPTED", historyStatus(database, "0"));
            assertEquals("ADOPTED", historyStatus(database, "1"));
            runner(sqlDirectory(), "migrate", database, 10, null).run();
            assertEquals(2, historyCount(database));
        });
    }

    @Test
    void migrateWithoutHistoryAndInvalidSchemasAreRejected() throws Exception {
        withDatabase(database -> {
            createDatabase(database);
            applyRaw(database, sqlDirectory().resolve("schema.sql"));
            assertThrows(IllegalStateException.class, () -> runner(sqlDirectory(), "migrate", database, 10, null).run());
            executeIn(database, "ALTER TABLE `Users` ALTER COLUMN `account_status` SET DEFAULT 'Dormant'");
            assertThrows(IllegalStateException.class, () -> runner(sqlDirectory(), "adopt", database, 10, 0).run());
        });
        withDatabase(database -> {
            createDatabase(database);
            executeIn(database, "CREATE VIEW only_a_view AS SELECT 1 AS value");
            assertThrows(IllegalStateException.class, () -> runner(sqlDirectory(), "init", database, 10, null).run());
        });
    }

    @Test
    void adoptAndMigrateRejectNonTableObjects() throws Exception {
        withDatabase(database -> {
            createDatabase(database);
            applyRaw(database, sqlDirectory().resolve("schema.sql"));
            executeIn(database, "CREATE VIEW r02_view AS SELECT 1 AS value");
            executeIn(database, "CREATE PROCEDURE r02_procedure() SELECT 1");
            executeIn(database, "CREATE TRIGGER r02_trigger BEFORE INSERT ON `Users` FOR EACH ROW SET NEW.name=NEW.name");
            executeIn(database, "CREATE EVENT r02_event ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 1 DAY DO SET @r02_event=1");
            try (Connection connection = connect()) {
                connection.setCatalog(database);
                assertEquals(4, SchemaInspector.nonTableObjects(connection, database).size());
            }
            assertThrows(IllegalStateException.class, () -> runner(sqlDirectory(), "adopt", database, 10, 0).run());
        });
        withDatabase(database -> {
            runner(sqlDirectory(), "init", database, 10, null).run();
            executeIn(database, "CREATE VIEW r02_view AS SELECT 1 AS value");
            assertThrows(IllegalStateException.class, () -> runner(sqlDirectory(), "migrate", database, 10, null).run());
        });
    }

    @Test
    void adoptRejectsCaseSensitiveDefaultsAndPhysicalDefinitionMutations() throws Exception {
        withDatabase(database -> {
            createDatabase(database);
            applyRaw(database, sqlDirectory().resolve("schema.sql"));
            applyRaw(database, sqlDirectory().resolve("migrations/V1__board_location_tables.sql"));
            executeIn(database, "ALTER TABLE `board_members` ALTER COLUMN `role` SET DEFAULT 'member'");
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> runner(sqlDirectory(), "adopt", database, 10, 1).run());
            assertTrue(failure.getMessage().contains("defaultValue=MEMBER"));
        });
        withDatabase(database -> {
            createDatabase(database);
            applyRaw(database, sqlDirectory().resolve("schema.sql"));
            executeIn(database, "ALTER TABLE `Payments` DROP FOREIGN KEY `payments_ibfk_1`");
            executeIn(database, "ALTER TABLE `Payments` ENGINE=MyISAM");
            executeIn(database, "ALTER TABLE `Payments` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            executeIn(database, "ALTER TABLE `Payments` MODIFY COLUMN `amount` DECIMAL(11,2) NULL");
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> runner(sqlDirectory(), "adopt", database, 10, 0).run());
            assertTrue(failure.getMessage().contains("engine/collation"));
            assertTrue(failure.getMessage().contains("column Payments.amount"));
            assertTrue(failure.getMessage().contains("foreign key in Payments"));
        });
    }

    @Test
    void checksumMutationIsRejected(@TempDir Path temporary) throws Exception {
        Path copy = copySql(temporary);
        withDatabase(database -> {
            runner(copy, "init", database, 10, null).run();
            Files.writeString(copy.resolve("migrations/V1__board_location_tables.sql"), "\n-- mutated\n",
                    StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> runner(copy, "migrate", database, 10, null).run());
            assertTrue(failure.getMessage().contains("Checksum"));
        });
    }

    @Test
    void ddlFailurePersistsAndBlocksRetry(@TempDir Path temporary) throws Exception {
        Path copy = copySql(temporary);
        Files.writeString(copy.resolve("migrations/V2__intentional_failure.sql"), """
                CREATE TABLE `r02_partial_ddl` (`id` BIGINT NOT NULL, PRIMARY KEY (`id`))
                  ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
                CREATE TABLE `this_is_invalid` (`id` BIGINT NOT NULL, PRIMARY KEY (`id`),
                  CONSTRAINT `invalid_reference` FOREIGN KEY (`id`) REFERENCES `r02_absent` (`id`))
                  ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
                """, StandardCharsets.UTF_8);
        withDatabase(database -> {
            assertThrows(IllegalStateException.class, () -> runner(copy, "init", database, 10, null).run());
            assertEquals("FAILED", historyStatus(database, "2"));
            assertEquals(1, scalar(database, "SELECT COUNT(*) FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA='" + database + "' AND TABLE_NAME='r02_partial_ddl'"));
            IllegalStateException retry = assertThrows(IllegalStateException.class,
                    () -> runner(copy, "migrate", database, 10, null).run());
            assertTrue(retry.getMessage().contains("automatic repair or retry is forbidden"));
        });
    }

    @Test
    void baselineFailureIsPersisted(@TempDir Path temporary) throws Exception {
        Path copy = copySql(temporary);
        String baseline = Files.readString(copy.resolve("schema.sql"), StandardCharsets.UTF_8)
                .replaceFirst("ENGINE=InnoDB", "ENGINE=NO_SUCH_ENGINE");
        Files.writeString(copy.resolve("schema.sql"), baseline, StandardCharsets.UTF_8);
        withDatabase(database -> {
            assertThrows(IllegalStateException.class, () -> runner(copy, "init", database, 10, null).run());
            assertEquals("FAILED", historyStatus(database, "0"));
            assertThrows(IllegalStateException.class, () -> runner(copy, "init", database, 10, null).run());
        });
    }

    @Test
    void preflightProtectsASeparateDatabaseFromQualifiedDdl(@TempDir Path temporary) throws Exception {
        Path copy = copySql(temporary);
        withDatabase(target -> withDatabase(sentinelDatabase -> {
            createDatabase(sentinelDatabase);
            executeIn(sentinelDatabase, "CREATE TABLE `sentinel` (`id` INT NOT NULL PRIMARY KEY)");
            executeIn(sentinelDatabase, "INSERT INTO `sentinel` VALUES (7)");
            Files.writeString(copy.resolve("migrations/V2__qualified_escape.sql"), """
                    CREATE TABLE `r02_should_not_run` (`id` BIGINT NOT NULL, PRIMARY KEY (`id`))
                      ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
                    DROP TABLE `%s`.`sentinel`;
                    """.formatted(sentinelDatabase), StandardCharsets.UTF_8);
            assertThrows(IllegalStateException.class, () -> runner(copy, "init", target, 10, null).run());
            assertEquals(1, scalar(sentinelDatabase, "SELECT COUNT(*) FROM `sentinel` WHERE id=7"));
            assertEquals(0, scalar(target, "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='" +
                    target + "' AND TABLE_NAME='r02_should_not_run'"));
            assertEquals("FAILED", historyStatus(target, "2"));
        }));
    }

    @Test
    void externalSchemaForeignKeyIsNotAcceptedForAdoption() throws Exception {
        withDatabase(target -> withDatabase(referenced -> {
            createDatabase(target);
            applyRaw(target, sqlDirectory().resolve("schema.sql"));
            applyRaw(target, sqlDirectory().resolve("migrations/V1__board_location_tables.sql"));
            createDatabase(referenced);
            executeIn(referenced, "CREATE TABLE `Users` LIKE `" + target + "`.`Users`");
            executeIn(target, "ALTER TABLE `board_posts` DROP FOREIGN KEY `board_posts_ibfk_2`");
            executeIn(target, "ALTER TABLE `board_posts` ADD CONSTRAINT `board_posts_ibfk_2` " +
                    "FOREIGN KEY (`author_email`) REFERENCES `" + referenced + "`.`Users` (`email`) ON DELETE CASCADE");
            try {
                IllegalStateException failure = assertThrows(IllegalStateException.class,
                        () -> runner(sqlDirectory(), "adopt", target, 10, 1).run());
                assertTrue(failure.getMessage().contains(referenced + ".Users"));
            } finally {
                executeIn(target, "ALTER TABLE `board_posts` DROP FOREIGN KEY `board_posts_ibfk_2`");
            }
        }));
    }

    @Test
    void historyAliasesOrderAndHolesAreRejected(@TempDir Path temporary) throws Exception {
        withDatabase(database -> {
            runner(sqlDirectory(), "init", database, 10, null).run();
            executeIn(database, "UPDATE `haru_schema_history` SET version='01' WHERE version='1'");
            assertThrows(IllegalStateException.class, () -> runner(sqlDirectory(), "migrate", database, 10, null).run());
        });
        withDatabase(database -> {
            runner(sqlDirectory(), "init", database, 10, null).run();
            executeIn(database, "UPDATE `haru_schema_history` SET installed_rank=3 WHERE version='0'");
            assertThrows(IllegalStateException.class, () -> runner(sqlDirectory(), "migrate", database, 10, null).run());
        });
        withDatabase(database -> {
            runner(sqlDirectory(), "init", database, 10, null).run();
            executeIn(database, "INSERT INTO `haru_schema_history` (version,description,script,checksum,status) " +
                    "SELECT '00',description,script,checksum,status FROM `haru_schema_history` WHERE version='0'");
            assertThrows(IllegalStateException.class, () -> runner(sqlDirectory(), "migrate", database, 10, null).run());
        });
        Path copy = copySql(temporary);
        Files.writeString(copy.resolve("migrations/V2__history_hole_probe.sql"), "SELECT SLEEP(0);", StandardCharsets.UTF_8);
        withDatabase(database -> {
            runner(copy, "init", database, 10, null).run();
            executeIn(database, "DELETE FROM `haru_schema_history` WHERE version='1'");
            assertThrows(IllegalStateException.class, () -> runner(copy, "migrate", database, 10, null).run());
        });
    }

    @Test
    void canonicalLineEndingsAndConstructionSnapshotAreStable(@TempDir Path temporary) throws Exception {
        Path crlf = copySql(temporary.resolve("crlf"));
        for (Path file : new Path[]{crlf.resolve("schema.sql"), crlf.resolve("migrations/V1__board_location_tables.sql")}) {
            String content = Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n").replace("\n", "\r\n");
            Files.writeString(file, content, StandardCharsets.UTF_8);
        }
        withDatabase(database -> {
            runner(crlf, "init", database, 10, null).run();
            Files.writeString(crlf.resolve("schema.sql"),
                    Files.readString(crlf.resolve("schema.sql"), StandardCharsets.UTF_8).replace("\r\n", "\n"),
                    StandardCharsets.UTF_8);
            Files.writeString(crlf.resolve("migrations/V1__board_location_tables.sql"),
                    Files.readString(crlf.resolve("migrations/V1__board_location_tables.sql"), StandardCharsets.UTF_8)
                            .replace("\r\n", "\n"), StandardCharsets.UTF_8);
            runner(crlf, "migrate", database, 10, null).run();
        });

        Path snapshot = copySql(temporary.resolve("snapshot"));
        withDatabase(database -> {
            MigrationRunner constructed = runner(snapshot, "init", database, 10, null);
            Files.writeString(snapshot.resolve("migrations/V1__board_location_tables.sql"), "\n-- changed after construction\n",
                    StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
            constructed.run();
            assertThrows(IllegalStateException.class, () -> runner(snapshot, "migrate", database, 10, null).run());
        });
    }

    @Test
    void concurrentRunnerCannotBypassSessionLock(@TempDir Path temporary) throws Exception {
        Path copy = copySql(temporary);
        Files.writeString(copy.resolve("migrations/V2__slow_lock_probe.sql"), """
                SELECT SLEEP(2);
                CREATE TABLE `r02_lock_probe` (`id` BIGINT NOT NULL, PRIMARY KEY (`id`))
                  ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
                """, StandardCharsets.UTF_8);
        withDatabase(database -> {
            CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> {
                try { return runner(copy, "init", database, 10, null).run(); }
                catch (Exception exception) { throw new RuntimeException(exception); }
            });
            awaitHistory(database, "2", "RUNNING");
            IllegalStateException locked = assertThrows(IllegalStateException.class,
                    () -> runner(copy, "migrate", database, 0, null).run());
            assertTrue(locked.getMessage().contains("migration lock"));
            assertTrue(first.get(10, TimeUnit.SECONDS).contains("version 2"));
            assertEquals("SUCCESS", historyStatus(database, "2"));
        });
    }

    private static MigrationRunner runner(Path sql, String command, String database, int lockTimeout,
                                          Integer version) throws IOException {
        RunnerConfig.Command parsed = RunnerConfig.Command.valueOf(command.toUpperCase());
        RunnerConfig config = new RunnerConfig(parsed, required("HARU_TEST_MYSQL_URL"), database,
                required("HARU_TEST_MYSQL_USER"), environment("HARU_TEST_MYSQL_PASSWORD"), version, lockTimeout);
        return new MigrationRunner(sql, config);
    }

    private static void withDatabase(ThrowingConsumer<String> test) throws Exception {
        String database = PREFIX + UUID.randomUUID().toString().replace("-", "");
        if (!database.matches("haru_r02_[a-f0-9]{32}")) throw new IllegalStateException("Unsafe generated database name");
        try { test.accept(database); }
        finally { dropDatabase(database); }
    }

    private static Path copySql(Path temporary) throws IOException {
        Path copy = temporary.resolve("sql");
        Files.createDirectories(copy.resolve("migrations"));
        Files.copy(sqlDirectory().resolve("schema.sql"), copy.resolve("schema.sql"));
        Files.copy(sqlDirectory().resolve("migrations/V1__board_location_tables.sql"),
                copy.resolve("migrations/V1__board_location_tables.sql"));
        return copy;
    }

    private static void createDatabase(String database) throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
    }

    private static void dropDatabase(String database) throws SQLException {
        if (!database.startsWith(PREFIX)) throw new IllegalArgumentException("Refusing to drop a non-test database");
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + database + "`");
        }
    }

    private static void applyRaw(String database, Path path) throws Exception {
        try (Connection connection = connect()) {
            connection.setCatalog(database);
            String script = Files.readString(path, StandardCharsets.UTF_8);
            for (String sql : SqlScript.statements(script)) {
                if (SqlScript.isCreateDatabase(sql) || SqlScript.isUseFor(sql, database)) continue;
                try (Statement statement = connection.createStatement()) { statement.execute(sql); }
            }
        }
    }

    private static void executeIn(String database, String sql) throws SQLException {
        try (Connection connection = connect()) {
            connection.setCatalog(database);
            try (Statement statement = connection.createStatement()) { statement.execute(sql); }
        }
    }

    private static int scalar(String database, String sql) throws SQLException {
        try (Connection connection = connect()) {
            connection.setCatalog(database);
            try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
                result.next(); return result.getInt(1);
            }
        }
    }

    private static int serverScalar(String sql) throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static int baseTableCount(String database) throws SQLException {
        return scalar(database, "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='" + database +
                "' AND TABLE_TYPE='BASE TABLE'");
    }

    private static int historyCount(String database) throws SQLException {
        return scalar(database, "SELECT COUNT(*) FROM `haru_schema_history`");
    }

    private static String historyStatus(String database, String version) throws SQLException {
        try (Connection connection = connect()) {
            connection.setCatalog(database);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT status FROM `haru_schema_history` WHERE version=?")) {
                statement.setString(1, version);
                try (ResultSet result = statement.executeQuery()) { result.next(); return result.getString(1); }
            }
        }
    }

    private static void awaitHistory(String database, String version, String status) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(8));
        while (Instant.now().isBefore(deadline)) {
            try {
                if (status.equals(historyStatus(database, version))) return;
            } catch (SQLException ignored) {
                // The initializer may not have created the database/history table yet.
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out waiting for history " + version + "=" + status);
    }

    private static boolean databaseExists(String database) throws SQLException {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.SCHEMATA WHERE SCHEMA_NAME=?")) {
            statement.setString(1, database);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    private static Connection connect() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", required("HARU_TEST_MYSQL_USER"));
        properties.setProperty("password", environment("HARU_TEST_MYSQL_PASSWORD"));
        return DriverManager.getConnection(required("HARU_TEST_MYSQL_URL"), properties);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value;
    }

    private static String environment(String name) {
        String value = System.getenv(name);
        if (value == null) throw new IllegalStateException(name + " is required");
        return value;
    }

    private static Path sqlDirectory() {
        Path root = Path.of("").toAbsolutePath();
        return Files.isDirectory(root.resolve("sql")) ? root.resolve("sql") : root;
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> { void accept(T value) throws Exception; }
}
