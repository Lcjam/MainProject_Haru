package com.haru.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaDefinitionTest {
    private static final String DDL = """
            CREATE TABLE `Parent` (
              `id` BIGINT NOT NULL AUTO_INCREMENT,
              `state` ENUM('Active','inactive') NOT NULL DEFAULT 'Active',
              PRIMARY KEY (`id`),
              UNIQUE KEY `uq_state` (`state`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
            CREATE TABLE `Child` (
              `id` BIGINT NOT NULL,
              `parent_id` BIGINT NOT NULL,
              CONSTRAINT `fk_parent` FOREIGN KEY (`parent_id`) REFERENCES `Parent` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
            """;

    @Test
    void parsesColumnIndexForeignKeyAndImplicitForeignKeyIndex() {
        SchemaDefinition schema = SchemaDefinition.baseline(DDL);
        assertTrue(schema.tables.get("Child").indexes().containsKey("fk_parent"));
        assertEqualsPreservingEnumCase(schema);
    }

    @Test
    void detectsLiteralCaseDifference() {
        SchemaDefinition expected = SchemaDefinition.baseline(DDL);
        SchemaDefinition changed = SchemaDefinition.baseline(DDL.replace("DEFAULT 'Active'", "DEFAULT 'active'"));
        assertFalse(expected.differences(changed).isEmpty());
    }

    @Test
    void finalSemicolonAndOrdinaryCommentsDoNotChangeExpectedSchema() {
        SchemaDefinition baseline = SchemaDefinition.baseline(DDL);
        assertTrue(baseline.differences(SchemaDefinition.baseline(DDL.stripTrailing().replaceFirst(";$", ""))).isEmpty());
        String alter = "/* note */ ALTER TABLE `Parent` ADD COLUMN `note` VARCHAR(20) DEFAULT NULL";
        SqlScript.validateForIsolatedTarget(alter, false);
        assertTrue(baseline.applying(alter).tables.get("Parent").columns().containsKey("note"));
    }

    private static void assertEqualsPreservingEnumCase(SchemaDefinition schema) {
        String type = schema.tables.get("Parent").columns().get("state").type();
        assertTrue(type.contains("'Active'"));
    }
}
