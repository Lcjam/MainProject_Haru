package com.haru.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

record RunnerConfig(Command command, String url, String database, String user, String password,
                    Integer adoptVersion, int lockTimeoutSeconds) {
    enum Command { INIT, ADOPT, MIGRATE }

    private static final Pattern DATABASE = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,47}");

    static RunnerConfig parse(String[] args, Map<String, String> environment) {
        if (args.length == 0) {
            throw new IllegalArgumentException(usage());
        }
        Command command;
        try {
            command = Command.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown command. " + usage());
        }

        String url = null;
        String database = null;
        String user = null;
        Integer adoptVersion = null;
        int lockTimeout = 30;
        List<String> unknown = new ArrayList<>();
        for (int index = 1; index < args.length; index++) {
            String option = args[index];
            if (option.equals("--url") && index + 1 < args.length) {
                url = args[++index];
            } else if (option.equals("--database") && index + 1 < args.length) {
                database = args[++index];
            } else if (option.equals("--user") && index + 1 < args.length) {
                user = args[++index];
            } else if (option.equals("--version") && index + 1 < args.length) {
                try { adoptVersion = Integer.valueOf(args[++index]); }
                catch (NumberFormatException exception) { throw new IllegalArgumentException("--version must be 0 or 1"); }
            } else if (option.equals("--lock-timeout") && index + 1 < args.length) {
                try { lockTimeout = Integer.parseInt(args[++index]); }
                catch (NumberFormatException exception) { throw new IllegalArgumentException("--lock-timeout must be an integer"); }
            } else {
                unknown.add(option);
            }
        }
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown or incomplete option(s); password arguments are unsupported");
        }
        validateServerUrl(url);
        if (database == null || !DATABASE.matcher(database).matches()) {
            throw new IllegalArgumentException("--database must match " + DATABASE.pattern());
        }
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("--user is required");
        }
        String password = environment.get("HARU_DB_PASSWORD");
        if (password == null) {
            throw new IllegalArgumentException("HARU_DB_PASSWORD is required (password arguments are intentionally unsupported)");
        }
        if (lockTimeout < 0 || lockTimeout > 300) {
            throw new IllegalArgumentException("--lock-timeout must be between 0 and 300 seconds");
        }
        if (command == Command.ADOPT && (adoptVersion == null || (adoptVersion != 0 && adoptVersion != 1))) {
            throw new IllegalArgumentException("adopt requires --version 0 or --version 1");
        }
        if (command != Command.ADOPT && adoptVersion != null) {
            throw new IllegalArgumentException("--version is valid only with adopt");
        }
        return new RunnerConfig(command, url, database, user, password, adoptVersion, lockTimeout);
    }

    static void validateServerUrl(String url) {
        if (url == null || !url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("--url must be a MySQL JDBC server URL");
        }
        int authorityStart = "jdbc:mysql://".length();
        int slash = url.indexOf('/', authorityStart);
        if (slash < 0) {
            throw new IllegalArgumentException("--url must end its server authority with / and must not select a database");
        }
        String authority = url.substring(authorityStart, slash);
        String suffix = url.substring(slash + 1);
        String path = suffix.contains("?") ? suffix.substring(0, suffix.indexOf('?')) : suffix;
        String query = suffix.contains("?") ? suffix.substring(suffix.indexOf('?') + 1).toLowerCase(Locale.ROOT) : "";
        if (authority.isBlank() || authority.contains("@") || !path.isEmpty()) {
            throw new IllegalArgumentException("--url must identify only a server; credentials and database paths are forbidden");
        }
        if (query.matches(".*(?:^|&)(?:user|password)=[^&]*.*")) {
            throw new IllegalArgumentException("JDBC URL credentials are forbidden; use --user and HARU_DB_PASSWORD");
        }
    }

    static String usage() {
        return "Usage: init|migrate|adopt --url jdbc:mysql://host:port/ --database NAME --user USER " +
                "[--version 0|1 for adopt] [--lock-timeout SECONDS]; password: HARU_DB_PASSWORD";
    }
}
