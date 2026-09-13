package com.haru.migration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlScriptTest {
    @Test
    void splitsOnlyUnquotedSemicolonsAndKeepsExecutableComments() {
        List<String> statements = SqlScript.statements("-- note\nSELECT 'a;b'; /*!40101 SET @x='c;d' */; # tail\n");
        assertEquals(2, statements.size());
        assertTrue(statements.get(1).contains("/*!40101"));
    }

    @Test
    void recognizesOnlyTheRepositoryDatabaseControlStatements() {
        assertTrue(SqlScript.isCreateDatabase("-- dump\nCREATE DATABASE `haru_db`"));
        assertTrue(SqlScript.isUseFor("-- select\nUSE `haru_db`", "isolated"));
        assertTrue(SqlScript.isUseFor("-- select\n\n-- another comment\n\nUSE `haru_db`", "isolated"));
        assertThrows(IllegalArgumentException.class, () -> SqlScript.isUseFor("USE `other`", "isolated"));
        assertFalse(SqlScript.isUseFor("SELECT 1", "isolated"));
    }

    @Test
    void rejectsUnsupportedDelimiterScripts() {
        assertThrows(IllegalArgumentException.class, () -> SqlScript.statements("DELIMITER //\nSELECT 1//"));
    }

    @Test
    void permitsBaselineExecutableSetCommentsButRejectsConditionalAndQualifiedEscapes() {
        SqlScript.validateForIsolatedTarget("-- dump header\n/*!40101 SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */", true);
        assertThrows(IllegalArgumentException.class,
                () -> SqlScript.validateForIsolatedTarget("/*!40101 DROP DATABASE other */", true));
        assertThrows(IllegalArgumentException.class,
                () -> SqlScript.validateForIsolatedTarget("SET @x=1 /*!40101 DROP DATABASE other */", true));
        assertThrows(IllegalArgumentException.class,
                () -> SqlScript.validateForIsolatedTarget("DROP TABLE `other`.`sentinel`", false));
        assertThrows(IllegalArgumentException.class,
                () -> SqlScript.validateForIsolatedTarget("CREATE TABLE `probe` LIKE `other`.`sentinel`", false));
    }

    @Test
    void rejectsSchemaChangingDdlThatTheDefinitionParserDoesNotSupport() {
        assertThrows(IllegalArgumentException.class,
                () -> SqlScript.validateForIsolatedTarget("ALTER TABLE `Users` DROP COLUMN `name`", false));
        assertThrows(IllegalArgumentException.class,
                () -> SqlScript.validateForIsolatedTarget("RENAME TABLE `Users` TO `People`", false));
        assertThrows(IllegalArgumentException.class,
                () -> SqlScript.validateForIsolatedTarget("SET GLOBAL sql_mode=''", false));
        assertThrows(IllegalArgumentException.class,
                () -> SqlScript.validateForIsolatedTarget("SET PASSWORD='changed'", false));
        assertThrows(IllegalArgumentException.class,
                () -> SqlScript.validateForIsolatedTarget("SET DEFAULT ROLE ALL TO user", false));
    }

    @Test
    void quotedCommentMarkersCannotHideAdditionalDdl() {
        for (String marker : List.of("#", "-- note", "/* note */")) {
            String sql = "ALTER TABLE `Users` ADD COLUMN `probe` INT COMMENT '" + marker + "', DROP COLUMN `name`";
            assertThrows(IllegalArgumentException.class, () -> SqlScript.validateForIsolatedTarget(sql, false));
        }
        assertEquals("SELECT '#literal' \n", SqlScript.stripOrdinaryComments("SELECT '#literal' -- real comment\n"));
    }

    @Test
    void rejectsUnmodelledTableOptionsAndColumnAttributes() {
        for (String option : List.of("DATA DIRECTORY='/tmp/r02'", "TABLESPACE shared_space", "ENCRYPTION='Y'")) {
            String sql = "CREATE TABLE `probe` (`id` INT) ENGINE=InnoDB " + option +
                    " DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci";
            assertThrows(IllegalArgumentException.class, () -> SqlScript.validateForIsolatedTarget(sql, false));
        }
        for (String attributes : List.of("GENERATED ALWAYS AS (1)", "INVISIBLE", "PRIMARY KEY", "UNIQUE", "REFERENCES `Users` (`id`)")) {
            assertThrows(IllegalArgumentException.class, () -> SqlScript.validateForIsolatedTarget(
                    "ALTER TABLE `Users` ADD COLUMN `probe` INT " + attributes, false));
            assertThrows(IllegalArgumentException.class, () -> SchemaDefinition.baseline(
                    "CREATE TABLE `probe` (`id` INT " + attributes +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;"));
        }
    }
}
