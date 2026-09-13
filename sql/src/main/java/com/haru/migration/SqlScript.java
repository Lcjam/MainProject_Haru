package com.haru.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SqlScript {
    private static final Pattern USE = Pattern.compile("(?is)^\\s*(?:--[^\\r\\n]*(?:\\r?\\n|$)|#[^\\r\\n]*(?:\\r?\\n|$)|/\\*(?!\\!)[\\s\\S]*?\\*/\\s*)*USE\\s+`?([^`\\s]+)`?\\s*$");
    private static final Pattern REPOSITORY_CREATE_DATABASE = Pattern.compile(
            "(?is)^CREATE\\s+DATABASE\\s+(?:/\\*!\\d+\\s+IF\\s+NOT\\s+EXISTS\\*/\\s+|IF\\s+NOT\\s+EXISTS\\s+)?`haru_db`(?:\\s+/\\*!.*?\\*/)?\\s*$");
    private static final Pattern QUALIFIED_IDENTIFIER = Pattern.compile(
            "(?i)(?:`[A-Za-z_][A-Za-z0-9_]*`|[A-Za-z_][A-Za-z0-9_]*)\\s*\\.\\s*(?:`[A-Za-z_][A-Za-z0-9_]*`|[A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern EXECUTABLE_COMMENT = Pattern.compile("(?is)^\\s*/\\*!\\d{5,6}\\s+([\\s\\S]*?)\\*/\\s*$");
    private static final Pattern SINGLE_ADD_COLUMN = Pattern.compile(
            "(?is)^ALTER\\s+TABLE\\s+`[A-Za-z_][A-Za-z0-9_]*`\\s+ADD\\s+COLUMN\\s+`[A-Za-z_][A-Za-z0-9_]*`\\s+.+$");
    private static final Set<String> DUMP_SESSION_VARIABLES = Set.of(
            "character_set_client", "character_set_results", "collation_connection", "time_zone",
            "unique_checks", "foreign_key_checks", "sql_mode", "sql_notes");
    private static final Set<String> DUMP_USER_VARIABLES = Set.of(
            "old_character_set_client", "old_character_set_results", "old_collation_connection", "old_time_zone",
            "old_unique_checks", "old_foreign_key_checks", "old_sql_mode", "old_sql_notes", "saved_cs_client");

    private SqlScript() {}

    static List<String> statements(String source) {
        if (source.toUpperCase(Locale.ROOT).contains("DELIMITER ")) {
            throw new IllegalArgumentException("DELIMITER directives are unsupported; keep migrations to JDBC-executable statements");
        }
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean single = false;
        boolean doub = false;
        boolean backtick = false;
        boolean lineComment = false;
        boolean blockComment = false;
        for (int index = 0; index < source.length(); index++) {
            char value = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';
            if (lineComment) {
                current.append(value);
                if (value == '\n') lineComment = false;
                continue;
            }
            if (blockComment) {
                current.append(value);
                if (value == '*' && next == '/') {
                    current.append(next);
                    index++;
                    blockComment = false;
                }
                continue;
            }
            if (!single && !doub && !backtick) {
                if ((value == '-' && next == '-' && (index + 2 >= source.length() || Character.isWhitespace(source.charAt(index + 2)))) || value == '#') {
                    lineComment = true;
                    current.append(value);
                    continue;
                }
                if (value == '/' && next == '*') {
                    blockComment = true;
                    current.append(value).append(next);
                    index++;
                    continue;
                }
            }
            if (value == '\'' && !doub && !backtick && !escaped(source, index)) single = !single;
            else if (value == '"' && !single && !backtick && !escaped(source, index)) doub = !doub;
            else if (value == '`' && !single && !doub) backtick = !backtick;

            if (value == ';' && !single && !doub && !backtick) {
                addIfExecutable(statements, current.toString());
                current.setLength(0);
            } else {
                current.append(value);
            }
        }
        if (single || doub || backtick || blockComment) {
            throw new IllegalArgumentException("Unterminated quote or block comment in SQL script");
        }
        addIfExecutable(statements, current.toString());
        return statements;
    }

    static boolean isCreateDatabase(String statement) {
        String stripped = stripOrdinaryComments(statement).trim();
        if (!stripped.toUpperCase(Locale.ROOT).startsWith("CREATE DATABASE")) return false;
        if (!REPOSITORY_CREATE_DATABASE.matcher(stripped).matches()) {
            throw new IllegalArgumentException("Only schema.sql's CREATE DATABASE haru_db statement is allowed and it is never executed");
        }
        return true;
    }

    static boolean isUseFor(String statement, String expectedDatabase) {
        Matcher matcher = USE.matcher(stripOrdinaryComments(statement).trim());
        if (!matcher.matches()) return false;
        if (!matcher.group(1).equals("haru_db")) {
            throw new IllegalArgumentException("SQL script attempts to select an unexpected database: " + matcher.group(1));
        }
        return true;
    }

    static void validateForIsolatedTarget(String statement, boolean baseline) {
        if (isCreateDatabase(statement) || isUseFor(statement, "ignored")) return;
        String withoutOrdinaryComments = stripOrdinaryComments(statement).trim();
        Matcher executableComment = EXECUTABLE_COMMENT.matcher(withoutOrdinaryComments);
        if (executableComment.matches()) {
            validateSet(executableComment.group(1));
            return;
        }
        if (withoutOrdinaryComments.contains("/*!")) {
            throw new IllegalArgumentException("Executable comments are supported only as standalone SET statements");
        }
        String sql = stripStringsAndComments(statement).trim();
        String upper = sql.toUpperCase(Locale.ROOT);
        if (QUALIFIED_IDENTIFIER.matcher(sql).find()) {
            throw new IllegalArgumentException("Schema-qualified identifiers are unsupported in isolated migrations");
        }
        if (upper.matches("(?s).*(?:CREATE|ALTER|DROP)\\s+(?:DATABASE|SCHEMA)\\b.*") ||
                upper.matches("(?s).*(?:PREPARE|EXECUTE|CALL|LOAD\\s+DATA|SOURCE)\\b.*")) {
            throw new IllegalArgumentException("Database-level or dynamic SQL is unsupported in isolated migrations");
        }
        if (upper.startsWith("SET ")) {
            validateSet(sql);
        } else if (upper.startsWith("CREATE TABLE ")) {
            SchemaDefinition parsed = SchemaDefinition.baseline(statement.endsWith(";") ? statement : statement + ";");
            if (parsed.tables.size() != 1) throw unsupported();
        } else if (upper.startsWith("ALTER TABLE ")) {
            if (!SINGLE_ADD_COLUMN.matcher(sql).matches() || hasTopLevelComma(sql)) throw unsupported();
            SchemaDefinition.validateAddColumn(statement);
        } else if (upper.startsWith("DROP TABLE ")) {
            if (!baseline || !upper.matches("DROP\\s+TABLE\\s+IF\\s+EXISTS\\s+`[A-Z_][A-Z0-9_]*`")) throw unsupported();
        } else if (!upper.matches("SELECT\\s+SLEEP\\s*\\(\\s*\\d+\\s*\\)")) {
            throw unsupported();
        }
    }

    private static void validateSet(String statement) {
        String sql = stripStringsAndComments(statement).trim();
        String normalized = sql.replaceAll("\\s+", " ").trim();
        if (normalized.matches("(?i)SET NAMES [A-Za-z0-9_]+")) return;
        if (!normalized.toUpperCase(Locale.ROOT).startsWith("SET ") || QUALIFIED_IDENTIFIER.matcher(sql).find()) {
            throw unsafeSet();
        }
        for (String assignment : splitTopLevel(normalized.substring(4))) {
            int equals = assignment.indexOf('=');
            if (equals < 1) throw unsafeSet();
            String left = assignment.substring(0, equals).trim().toLowerCase(Locale.ROOT);
            String right = assignment.substring(equals + 1).trim().toLowerCase(Locale.ROOT);
            if (left.startsWith("@") && !left.startsWith("@@")) {
                if (!DUMP_USER_VARIABLES.contains(left.substring(1)) || !allowedSystemVariable(right)) throw unsafeSet();
            } else {
                if (!DUMP_SESSION_VARIABLES.contains(left) || !allowedDumpValue(right)) throw unsafeSet();
            }
        }
    }

    private static boolean allowedSystemVariable(String value) {
        String name = value.startsWith("@@") ? value.substring(2) : "";
        return DUMP_SESSION_VARIABLES.contains(name);
    }

    private static boolean allowedDumpValue(String value) {
        if (value.startsWith("@") && !value.startsWith("@@")) {
            return DUMP_USER_VARIABLES.contains(value.substring(1));
        }
        return allowedSystemVariable(value) || value.matches("''|[+-]?\\d+|[a-z0-9_]+");
    }

    private static IllegalArgumentException unsafeSet() {
        return new IllegalArgumentException("Only mysqldump session and save/restore SET statements are supported");
    }

    private static IllegalArgumentException unsupported() {
        return new IllegalArgumentException("Unsupported migration statement; supported forms are session SET, CREATE TABLE, " +
                "single-column ALTER TABLE ADD COLUMN, and baseline DROP TABLE IF EXISTS");
    }

    private static boolean hasTopLevelComma(String value) {
        boolean single = false;
        boolean doub = false;
        boolean backtick = false;
        int depth = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\'' && !doub && !backtick && !escaped(value, index)) single = !single;
            else if (current == '"' && !single && !backtick && !escaped(value, index)) doub = !doub;
            else if (current == '`' && !single && !doub) backtick = !backtick;
            else if (!single && !doub && !backtick && current == '(') depth++;
            else if (!single && !doub && !backtick && current == ')') depth--;
            else if (!single && !doub && !backtick && depth == 0 && current == ',') return true;
        }
        return false;
    }

    private static List<String> splitTopLevel(String value) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean single = false;
        boolean doub = false;
        int depth = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\'' && !doub && !escaped(value, index)) single = !single;
            else if (character == '"' && !single && !escaped(value, index)) doub = !doub;
            else if (!single && !doub && character == '(') depth++;
            else if (!single && !doub && character == ')') depth--;
            if (!single && !doub && depth == 0 && character == ',') {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        result.add(current.toString().trim());
        return result;
    }

    private static boolean escaped(String source, int index) {
        int count = 0;
        for (int cursor = index - 1; cursor >= 0 && source.charAt(cursor) == '\\'; cursor--) count++;
        return count % 2 == 1;
    }

    private static void addIfExecutable(List<String> statements, String statement) {
        String stripped = stripOrdinaryComments(statement).trim();
        if (!stripped.isEmpty()) statements.add(statement.trim());
    }

    static String stripOrdinaryComments(String value) {
        return scanSql(value, false);
    }

    private static String stripStringsAndComments(String value) {
        return scanSql(value, true);
    }

    // Comment markers inside string literals are data, not SQL comments.
    private static String scanSql(String value, boolean maskStrings) {
        StringBuilder result = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            char next = i + 1 < value.length() ? value.charAt(i + 1) : '\0';
            if (quote != 0) {
                boolean keep = !maskStrings || quote == '`';
                if (c == '\\' && quote != '`' && next != '\0') {
                    if (keep) result.append(c).append(next);
                    i++;
                } else if (c == quote && next == quote) {
                    if (keep) result.append(c).append(next);
                    i++;
                } else if (c == quote) {
                    result.append(c);
                    quote = 0;
                } else if (keep) {
                    result.append(c);
                }
                continue;
            }
            if (c == '\'' || c == '"' || c == '`') {
                quote = c;
                result.append(c);
            } else if (c == '#' || (c == '-' && next == '-' &&
                    (i + 2 == value.length() || Character.isWhitespace(value.charAt(i + 2))))) {
                while (i < value.length() && value.charAt(i) != '\n') i++;
                result.append('\n');
            } else if (c == '/' && next == '*') {
                int end = value.indexOf("*/", i + 2);
                if (end < 0) throw new IllegalArgumentException("Unterminated SQL comment");
                if (i + 2 < value.length() && value.charAt(i + 2) == '!') {
                    result.append(value, i, end + 2);
                } else {
                    result.append(' ');
                }
                i = end + 1;
            } else {
                result.append(c);
            }
        }
        if (quote != 0) throw new IllegalArgumentException("Unterminated SQL literal");
        return result.toString();
    }
}
