package com.haru.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class SchemaInspector {
    private SchemaInspector() {}

    static boolean databaseExists(Connection connection, String database) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?")) {
            statement.setString(1, database);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    static int lowerCaseTableNames(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT @@lower_case_table_names")) {
            if (!result.next()) throw new SQLException("lower_case_table_names was not returned");
            return result.getInt(1);
        }
    }

    static SchemaDefinition inspect(Connection connection, String database, String historyTable) throws SQLException {
        Map<String, MutableTable> tables = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA=? AND TABLE_TYPE='BASE TABLE' AND TABLE_NAME<>?
                ORDER BY TABLE_NAME
                """)) {
            statement.setString(1, database);
            statement.setString(2, historyTable);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    tables.put(result.getString(1), new MutableTable(
                            result.getString(1), result.getString(2), result.getString(3)));
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT c.TABLE_NAME, c.COLUMN_NAME, c.COLUMN_TYPE, c.IS_NULLABLE, c.COLUMN_DEFAULT, c.EXTRA,
                       c.COLLATION_NAME
                FROM information_schema.COLUMNS c
                JOIN information_schema.TABLES t ON t.TABLE_SCHEMA=c.TABLE_SCHEMA AND t.TABLE_NAME=c.TABLE_NAME
                WHERE c.TABLE_SCHEMA=? AND t.TABLE_TYPE='BASE TABLE' AND c.TABLE_NAME<>?
                ORDER BY c.TABLE_NAME, c.ORDINAL_POSITION
                """)) {
            statement.setString(1, database);
            statement.setString(2, historyTable);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    MutableTable table = tables.get(result.getString(1));
                    if (table == null) continue;
                    table.columns.put(result.getString(2), new SchemaDefinition.Column(
                            SchemaDefinition.normalizeType(result.getString(3)), "YES".equals(result.getString(4)),
                            SchemaDefinition.normalizeDefault(result.getString(5)), normalizeExtra(result.getString(6)),
                            result.getString(7)));
                }
            }
        }
        loadIndexes(connection, database, historyTable, tables);
        loadForeignKeys(connection, database, historyTable, tables);
        Map<String, SchemaDefinition.Table> immutable = new LinkedHashMap<>();
        tables.forEach((name, table) -> immutable.put(name, table.freeze()));
        return SchemaDefinition.fromInspected(immutable);
    }

    static boolean hasAnyUserObjects(Connection connection, String database) throws SQLException {
        String sql = """
                SELECT (
                  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=?) +
                  (SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=?) +
                  (SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=?) +
                  (SELECT COUNT(*) FROM information_schema.EVENTS WHERE EVENT_SCHEMA=?)
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 4; index++) statement.setString(index, database);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1) != 0;
            }
        }
    }

    static List<String> nonTableObjects(Connection connection, String database) throws SQLException {
        String sql = """
                SELECT object_type, object_name FROM (
                  SELECT 'VIEW' object_type, TABLE_NAME object_name
                    FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_TYPE='VIEW'
                  UNION ALL
                  SELECT ROUTINE_TYPE, ROUTINE_NAME
                    FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=?
                  UNION ALL
                  SELECT 'TRIGGER', TRIGGER_NAME
                    FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=?
                  UNION ALL
                  SELECT 'EVENT', EVENT_NAME
                    FROM information_schema.EVENTS WHERE EVENT_SCHEMA=?
                  UNION ALL
                  SELECT 'CHECK', CONSTRAINT_NAME
                    FROM information_schema.TABLE_CONSTRAINTS
                    WHERE CONSTRAINT_SCHEMA=? AND CONSTRAINT_TYPE='CHECK'
                ) objects ORDER BY object_type, object_name
                """;
        List<String> objects = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 5; index++) statement.setString(index, database);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) objects.add(result.getString(1) + " " + result.getString(2));
            }
        }
        return objects;
    }

    private static void loadIndexes(Connection connection, String database, String historyTable,
                                    Map<String, MutableTable> tables) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, COLUMN_NAME,
                       SUB_PART, INDEX_TYPE, IS_VISIBLE, COLLATION, EXPRESSION
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA=? AND TABLE_NAME<>?
                ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX
                """)) {
            statement.setString(1, database);
            statement.setString(2, historyTable);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    MutableTable table = tables.get(result.getString(1));
                    if (table == null) continue;
                    if (result.getObject(5) != null || !"BTREE".equals(result.getString(6)) ||
                            !"YES".equals(result.getString(7)) || !"A".equals(result.getString(8)) ||
                            result.getString(9) != null) {
                        throw new IllegalStateException("Unsupported index definition: " +
                                result.getString(1) + "." + result.getString(2));
                    }
                    String indexName = result.getString(2);
                    MutableIndex index = table.indexes.get(indexName);
                    if (index == null) {
                        index = new MutableIndex(result.getInt(3) == 0);
                        table.indexes.put(indexName, index);
                    }
                    index.columns.add(result.getString(4));
                }
            }
        }
    }

    private static void loadForeignKeys(Connection connection, String database, String historyTable,
                                        Map<String, MutableTable> tables) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT k.TABLE_NAME, k.CONSTRAINT_NAME, k.COLUMN_NAME, k.REFERENCED_TABLE_SCHEMA, k.REFERENCED_TABLE_NAME,
                       k.REFERENCED_COLUMN_NAME, r.DELETE_RULE, r.UPDATE_RULE
                FROM information_schema.KEY_COLUMN_USAGE k
                JOIN information_schema.REFERENTIAL_CONSTRAINTS r
                  ON r.CONSTRAINT_SCHEMA=k.CONSTRAINT_SCHEMA AND r.CONSTRAINT_NAME=k.CONSTRAINT_NAME
                WHERE k.CONSTRAINT_SCHEMA=? AND k.REFERENCED_TABLE_NAME IS NOT NULL AND k.TABLE_NAME<>?
                ORDER BY k.TABLE_NAME, k.CONSTRAINT_NAME, k.ORDINAL_POSITION
                """)) {
            statement.setString(1, database);
            statement.setString(2, historyTable);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    MutableTable table = tables.get(result.getString(1));
                    if (table == null) continue;
                    String keyName = result.getString(2);
                    MutableForeignKey key = table.foreignKeys.get(keyName);
                    if (key == null) {
                        String referencedSchema = result.getString(4);
                        String referencedTable = database.equals(referencedSchema) ? result.getString(5) :
                                referencedSchema + "." + result.getString(5);
                        key = new MutableForeignKey(referencedTable,
                                SchemaDefinition.normalizeAction(result.getString(7)),
                                SchemaDefinition.normalizeAction(result.getString(8)));
                        table.foreignKeys.put(keyName, key);
                    }
                    key.columns.add(result.getString(3));
                    key.referencedColumns.add(result.getString(6));
                }
            }
        }
    }

    private static String normalizeExtra(String extra) {
        // DEFAULT_GENERATED accompanies expression defaults already compared above.
        // Preserve unknown attributes so invisible/generated columns cannot look ordinary.
        return extra == null ? "" : extra.toLowerCase(Locale.ROOT).replace("default_generated", "")
                .replace("()", "").trim().replaceAll("\\s+", " ");
    }

    private static final class MutableTable {
        final String name;
        final String engine;
        final String collation;
        final Map<String, SchemaDefinition.Column> columns = new LinkedHashMap<>();
        final Map<String, MutableIndex> indexes = new LinkedHashMap<>();
        final Map<String, MutableForeignKey> foreignKeys = new LinkedHashMap<>();
        MutableTable(String name, String engine, String collation) {
            this.name = name;
            this.engine = engine;
            this.collation = collation;
        }
        SchemaDefinition.Table freeze() {
            Map<String, SchemaDefinition.Index> finalIndexes = new LinkedHashMap<>();
            indexes.forEach((key, value) -> finalIndexes.put(key, new SchemaDefinition.Index(value.unique, value.columns)));
            Map<String, SchemaDefinition.ForeignKey> finalKeys = new LinkedHashMap<>();
            foreignKeys.forEach((key, value) -> finalKeys.put(key, new SchemaDefinition.ForeignKey(
                    value.columns, value.referencedTable, value.referencedColumns, value.deleteRule, value.updateRule)));
            return new SchemaDefinition.Table(name, engine, collation, columns, finalIndexes, finalKeys);
        }
    }

    private static final class MutableIndex {
        final boolean unique;
        final List<String> columns = new ArrayList<>();
        MutableIndex(boolean unique) { this.unique = unique; }
    }

    private static final class MutableForeignKey {
        final List<String> columns = new ArrayList<>();
        final String referencedTable;
        final List<String> referencedColumns = new ArrayList<>();
        final String deleteRule;
        final String updateRule;
        MutableForeignKey(String referencedTable, String deleteRule, String updateRule) {
            this.referencedTable = referencedTable;
            this.deleteRule = deleteRule;
            this.updateRule = updateRule;
        }
    }
}
