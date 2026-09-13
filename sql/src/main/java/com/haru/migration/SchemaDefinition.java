package com.haru.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SchemaDefinition {
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?is)CREATE\\s+TABLE\\s+`([^`]+)`\\s*\\((.*?)\\)\\s*ENGINE\\s*=\\s*(InnoDB)\\s+DEFAULT\\s+CHARSET\\s*=\\s*utf8mb4\\s+COLLATE\\s*=\\s*([A-Za-z0-9_]+)\\s*;");
    private static final Pattern ADD_COLUMN = Pattern.compile(
            "(?is)ALTER\\s+TABLE\\s+`([^`]+)`\\s+ADD\\s+COLUMN\\s+`([^`]+)`\\s+(.+?)(?:\\s+AFTER\\s+`[^`]+`|\\s+FIRST)?\\s*;");
    private static final Pattern DEFAULT = Pattern.compile(
            "(?is)\\bDEFAULT\\s+('(?:''|[^'])*'|[^\\s,]+)");
    private static final Pattern COLUMN_ATTRIBUTES = Pattern.compile(
            "(?is)(?:\\s*(?:NOT\\s+NULL|NULL|AUTO_INCREMENT|ON\\s+UPDATE\\s+CURRENT_TIMESTAMP(?:\\(\\))?|" +
            "DEFAULT\\s+(?:'(?:''|\\\\.|[^'\\\\])*'|NULL|[+-]?\\d+(?:\\.\\d+)?|CURRENT_TIMESTAMP(?:\\(\\))?)|" +
            "COMMENT\\s+'(?:''|\\\\.|[^'\\\\])*'))*\\s*");
    private static final Pattern REFERENCE = Pattern.compile(
            "(?is)^CONSTRAINT\\s+`([^`]+)`\\s+FOREIGN\\s+KEY\\s*\\(([^)]+)\\)\\s+REFERENCES\\s+`([^`]+)`\\s*\\(([^)]+)\\)(.*)$");

    final Map<String, Table> tables;

    private SchemaDefinition(Map<String, Table> tables) {
        this.tables = Collections.unmodifiableMap(tables);
    }

    static SchemaDefinition fromInspected(Map<String, Table> tables) {
        return new SchemaDefinition(new LinkedHashMap<>(tables));
    }

    static SchemaDefinition baseline(String sql) {
        return new SchemaDefinition(parseTables(sql));
    }

    SchemaDefinition applying(String sql) {
        String normalized = normalizeStatements(sql);
        Map<String, Table> combined = new LinkedHashMap<>(tables);
        combined.putAll(parseTables(normalized));
        Matcher alter = ADD_COLUMN.matcher(normalized);
        while (alter.find()) {
            Table current = combined.get(alter.group(1));
            if (current == null) throw new IllegalArgumentException("ALTER references unknown table " + alter.group(1));
            Map<String, Column> columns = new LinkedHashMap<>(current.columns);
            Column added = parseColumn(alter.group(3), current.collation);
            columns.put(alter.group(2), added);
            combined.put(current.name, new Table(current.name, current.engine, current.collation,
                    columns, current.indexes, current.foreignKeys));
        }
        return new SchemaDefinition(combined);
    }

    static void validateAddColumn(String sql) {
        Matcher alter = ADD_COLUMN.matcher(normalizeStatements(sql));
        if (!alter.matches()) throw new IllegalArgumentException("Unsupported ADD COLUMN declaration");
        parseColumn(alter.group(3), "utf8mb4_0900_ai_ci");
    }

    private static String normalizeStatements(String sql) {
        return SqlScript.stripOrdinaryComments(String.join(";\n", SqlScript.statements(sql))).trim() + ";";
    }

    List<String> differences(SchemaDefinition actual) {
        List<String> differences = new ArrayList<>();
        compareKeys("table", tables, actual.tables, differences);
        for (Map.Entry<String, Table> entry : tables.entrySet()) {
            Table observed = actual.tables.get(entry.getKey());
            if (observed == null) continue;
            Table expected = entry.getValue();
            if (!expected.engine.equalsIgnoreCase(observed.engine) || !expected.collation.equals(observed.collation)) {
                differences.add("table " + expected.name + " expected engine/collation " + expected.engine + "/" +
                        expected.collation + " but was " + observed.engine + "/" + observed.collation);
            }
            compareKeys("column in " + expected.name, expected.columns, observed.columns, differences);
            expected.columns.forEach((name, value) -> {
                Column other = observed.columns.get(name);
                if (other != null && !value.equals(other)) differences.add(
                        "column " + expected.name + "." + name + " expected " + value + " but was " + other);
            });
            compareKeys("index in " + expected.name, expected.indexes, observed.indexes, differences);
            expected.indexes.forEach((name, value) -> {
                Index other = observed.indexes.get(name);
                if (other != null && !value.equals(other)) differences.add(
                        "index " + expected.name + "." + name + " expected " + value + " but was " + other);
            });
            compareKeys("foreign key in " + expected.name, expected.foreignKeys, observed.foreignKeys, differences);
            expected.foreignKeys.forEach((name, value) -> {
                ForeignKey other = observed.foreignKeys.get(name);
                if (other != null && !value.equals(other)) differences.add(
                        "foreign key " + expected.name + "." + name + " expected " + value + " but was " + other);
            });
        }
        return differences;
    }

    private static <T> void compareKeys(String kind, Map<String, T> expected, Map<String, T> actual,
                                        List<String> differences) {
        for (String name : expected.keySet()) if (!actual.containsKey(name)) differences.add("missing " + kind + ": " + name);
        for (String name : actual.keySet()) if (!expected.containsKey(name)) differences.add("unexpected " + kind + ": " + name);
    }

    private static Map<String, Table> parseTables(String sql) {
        Map<String, Table> tables = new LinkedHashMap<>();
        Matcher matcher = CREATE_TABLE.matcher(normalizeStatements(sql));
        while (matcher.find()) {
            String tableName = matcher.group(1);
            String engine = matcher.group(3);
            String collation = matcher.group(4);
            Map<String, Column> columns = new LinkedHashMap<>();
            Map<String, Index> indexes = new LinkedHashMap<>();
            Map<String, ForeignKey> foreignKeys = new LinkedHashMap<>();
            for (String rawItem : splitTopLevel(matcher.group(2))) {
                String item = removeLineComments(rawItem).trim();
                if (item.isEmpty()) continue;
                if (item.startsWith("`")) {
                    int end = item.indexOf('`', 1);
                    String name = item.substring(1, end);
                    columns.put(name, parseColumn(item.substring(end + 1), collation));
                } else if (item.toUpperCase(Locale.ROOT).startsWith("PRIMARY KEY")) {
                    indexes.put("PRIMARY", new Index(true, identifierList(parenthesized(item))));
                } else if (item.toUpperCase(Locale.ROOT).startsWith("UNIQUE KEY")) {
                    String name = backtickAt(item, item.toUpperCase(Locale.ROOT).indexOf("KEY") + 3);
                    indexes.put(name, new Index(true, identifierList(parenthesized(item))));
                } else if (item.toUpperCase(Locale.ROOT).startsWith("KEY")) {
                    String name = backtickAt(item, 3);
                    indexes.put(name, new Index(false, identifierList(parenthesized(item))));
                } else if (item.toUpperCase(Locale.ROOT).startsWith("CONSTRAINT")) {
                    Matcher reference = REFERENCE.matcher(item);
                    if (!reference.matches()) throw new IllegalArgumentException("Unsupported foreign key declaration: " + item);
                    String actions = reference.group(5).toUpperCase(Locale.ROOT);
                    foreignKeys.put(reference.group(1), new ForeignKey(
                            identifierList(reference.group(2)), reference.group(3), identifierList(reference.group(4)),
                            action(actions, "DELETE"), action(actions, "UPDATE")));
                } else {
                    throw new IllegalArgumentException("Unsupported CREATE TABLE clause: " + item);
                }
            }
            foreignKeys.forEach((constraint, key) -> {
                boolean covered = indexes.values().stream().anyMatch(index -> startsWith(index.columns, key.columns));
                if (!covered) indexes.put(constraint, new Index(false, key.columns));
            });
            Table previous = tables.put(tableName, new Table(tableName, engine, collation, columns, indexes, foreignKeys));
            if (previous != null) throw new IllegalArgumentException("Duplicate CREATE TABLE for " + tableName);
        }
        return tables;
    }

    private static Column parseColumn(String declaration, String tableCollation) {
        String clean = removeLineComments(declaration).trim().replaceFirst(",$", "");
        int typeEnd = typeEnd(clean);
        String type = normalizeType(clean.substring(0, typeEnd));
        String attributes = clean.substring(typeEnd).trim();
        if (!COLUMN_ATTRIBUTES.matcher(attributes).matches()) {
            throw new IllegalArgumentException("Unsupported column attributes in migration");
        }
        boolean nullable = !attributes.toUpperCase(Locale.ROOT).contains("NOT NULL");
        Matcher defaultMatcher = DEFAULT.matcher(attributes);
        String defaultValue = defaultMatcher.find() ? normalizeDefault(defaultMatcher.group(1)) : null;
        String upper = attributes.toUpperCase(Locale.ROOT);
        List<String> extras = new ArrayList<>();
        if (upper.contains("AUTO_INCREMENT")) extras.add("auto_increment");
        if (upper.contains("ON UPDATE CURRENT_TIMESTAMP")) extras.add("on update current_timestamp");
        String collation = isCharacterType(type) ? tableCollation : null;
        return new Column(type, nullable, defaultValue, String.join(" ", extras), collation);
    }

    private static int typeEnd(String value) {
        int depth = 0;
        boolean quoted = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\'' && (index == 0 || value.charAt(index - 1) != '\\')) quoted = !quoted;
            if (!quoted && current == '(') depth++;
            else if (!quoted && current == ')') depth--;
            else if (!quoted && depth == 0 && Character.isWhitespace(current)) return index;
        }
        return value.length();
    }

    static String normalizeType(String value) {
        String compact = value.trim().replaceAll("\\s+", " ");
        StringBuilder result = new StringBuilder(compact.length());
        boolean literal = false;
        for (int index = 0; index < compact.length(); index++) {
            char current = compact.charAt(index);
            if (current == '\'' && (index == 0 || compact.charAt(index - 1) != '\\')) literal = !literal;
            result.append(literal ? current : Character.toLowerCase(current));
        }
        return result.toString();
    }

    static String normalizeDefault(String value) {
        if (value == null || value.equalsIgnoreCase("NULL")) return null;
        String result = value.trim();
        if (result.length() >= 2 && result.startsWith("'") && result.endsWith("'")) {
            return result.substring(1, result.length() - 1).replace("''", "'");
        }
        if (result.equalsIgnoreCase("current_timestamp") || result.equalsIgnoreCase("current_timestamp()")) {
            return "current_timestamp";
        }
        return result;
    }

    private static String action(String text, String operation) {
        Matcher matcher = Pattern.compile("ON\\s+" + operation + "\\s+(CASCADE|RESTRICT|SET NULL|NO ACTION)").matcher(text);
        return normalizeAction(matcher.find() ? matcher.group(1) : "RESTRICT");
    }

    // InnoDB enforces NO ACTION immediately, with the same semantics as RESTRICT.
    static String normalizeAction(String action) {
        return "NO ACTION".equalsIgnoreCase(action) ? "RESTRICT" : action.toUpperCase(Locale.ROOT);
    }

    private static boolean isCharacterType(String type) {
        return type.startsWith("char") || type.startsWith("varchar") || type.contains("text") ||
                type.startsWith("enum") || type.startsWith("set");
    }

    private static boolean startsWith(List<String> values, List<String> prefix) {
        return values.size() >= prefix.size() && values.subList(0, prefix.size()).equals(prefix);
    }

    private static String removeLineComments(String value) {
        return SqlScript.stripOrdinaryComments(value);
    }

    private static String parenthesized(String value) {
        int start = value.indexOf('(');
        int end = value.lastIndexOf(')');
        if (start < 0 || end < start) throw new IllegalArgumentException("Expected parenthesized identifiers: " + value);
        return value.substring(start + 1, end);
    }

    private static String backtickAt(String value, int start) {
        int left = value.indexOf('`', start);
        int right = left < 0 ? -1 : value.indexOf('`', left + 1);
        if (left < 0 || right < 0) throw new IllegalArgumentException("Expected named index: " + value);
        return value.substring(left + 1, right);
    }

    private static List<String> identifierList(String value) {
        List<String> names = new ArrayList<>();
        Matcher matcher = Pattern.compile("`([^`]+)`").matcher(value);
        while (matcher.find()) names.add(matcher.group(1));
        if (names.isEmpty()) throw new IllegalArgumentException("Expected quoted identifier list: " + value);
        return names;
    }

    private static List<String> splitTopLevel(String value) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean quoted = false;
        boolean lineComment = false;
        boolean blockComment = false;
        int start = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            char next = index + 1 < value.length() ? value.charAt(index + 1) : '\0';
            if (lineComment) {
                if (current == '\n') lineComment = false;
                continue;
            }
            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    index++;
                }
                continue;
            }
            if (!quoted && ((current == '-' && next == '-' &&
                    (index + 2 >= value.length() || Character.isWhitespace(value.charAt(index + 2)))) || current == '#')) {
                lineComment = true;
                continue;
            }
            if (!quoted && current == '/' && next == '*') {
                blockComment = true;
                index++;
                continue;
            }
            if (current == '\'' && (index == 0 || value.charAt(index - 1) != '\\')) quoted = !quoted;
            if (!quoted && current == '(') depth++;
            else if (!quoted && current == ')') depth--;
            else if (!quoted && depth == 0 && current == ',') {
                parts.add(value.substring(start, index));
                start = index + 1;
            }
        }
        parts.add(value.substring(start));
        return parts;
    }

    record Table(String name, String engine, String collation, Map<String, Column> columns, Map<String, Index> indexes,
                 Map<String, ForeignKey> foreignKeys) {
        Table {
            columns = Collections.unmodifiableMap(new LinkedHashMap<>(columns));
            indexes = Collections.unmodifiableMap(new LinkedHashMap<>(indexes));
            foreignKeys = Collections.unmodifiableMap(new LinkedHashMap<>(foreignKeys));
        }
    }

    record Column(String type, boolean nullable, String defaultValue, String extra, String collation) {}
    record Index(boolean unique, List<String> columns) { Index { columns = List.copyOf(columns); } }
    record ForeignKey(List<String> columns, String referencedTable, List<String> referencedColumns,
                      String deleteRule, String updateRule) {
        ForeignKey {
            columns = List.copyOf(columns);
            referencedColumns = List.copyOf(referencedColumns);
            Objects.requireNonNull(referencedTable);
        }
    }
}
