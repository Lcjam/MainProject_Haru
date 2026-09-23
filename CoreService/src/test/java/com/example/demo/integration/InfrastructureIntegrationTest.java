package com.example.demo.integration;

import com.example.demo.integration.support.MySqlTestDatabase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@Tag("mysql")
class InfrastructureIntegrationTest {
    @Test
    void migrationsUseBaselineAndV1WithCaseSensitiveSchemaAndFixtureForeignKeys() throws Exception {
        try (MySqlTestDatabase database = MySqlTestDatabase.create()) {
            database.seedFixture();
            assertEquals(2, database.jdbc().queryForObject("SELECT COUNT(*) FROM haru_schema_history WHERE status='SUCCESS'", Integer.class));
            assertEquals("schema.sql", database.jdbc().queryForObject("SELECT script FROM haru_schema_history WHERE version='0'", String.class));
            assertEquals(1, database.jdbc().queryForObject("SELECT COUNT(*) FROM information_schema.TABLES WHERE table_schema=? AND table_name='Users'", Integer.class, database.name()));
            assertEquals(0, database.jdbc().queryForObject("SELECT COUNT(*) FROM information_schema.TABLES WHERE table_schema=? AND BINARY table_name = BINARY 'users'", Integer.class, database.name()));
            assertEquals(1, database.jdbc().queryForObject("SELECT COUNT(*) FROM chatrooms WHERE chatroom_id=300", Integer.class));
            DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                    () -> database.jdbc().update("INSERT INTO productrequests (product_id, requester_email) VALUES (?, ?)", 999999L, "outsider@r03.example.test"));
            SQLException sqlError = (SQLException) error.getMostSpecificCause();
            assertEquals("23000", sqlError.getSQLState());
            assertEquals(1452, sqlError.getErrorCode());
        }
    }

    @Test
    void databasesAreIsolatedAndClosedDatabaseIsRemoved() throws Exception {
        MySqlTestDatabase first = MySqlTestDatabase.create();
        String firstName = first.name();
        try (MySqlTestDatabase second = MySqlTestDatabase.create()) {
            assertNotEquals(firstName, second.name());
            first.jdbc().execute("CREATE TABLE r03_isolation (id INT PRIMARY KEY)");
            assertEquals(0, second.jdbc().queryForObject("SELECT COUNT(*) FROM information_schema.TABLES WHERE table_schema=? AND table_name='r03_isolation'", Integer.class, second.name()));
        } finally {
            first.close();
        }
        try (var connection = java.sql.DriverManager.getConnection(System.getenv("HARU_TEST_MYSQL_URL"), System.getenv("HARU_TEST_MYSQL_USER"), System.getenv("HARU_TEST_MYSQL_PASSWORD"));
             var statement = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME=?")) {
            statement.setString(1, firstName);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
        }
    }
}
