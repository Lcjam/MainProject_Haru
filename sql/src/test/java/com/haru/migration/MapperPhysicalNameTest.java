package com.haru.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperPhysicalNameTest {
    private static final Pattern TABLE_REFERENCE = Pattern.compile(
            "(?i)\\b(FROM|JOIN|INTO|UPDATE|DELETE\\s+FROM)\\s+`?([A-Za-z_][A-Za-z0-9_]*)`?");
    private static final Pattern DUPLICATE_KEY_PREFIX = Pattern.compile("(?is).*ON\\s+DUPLICATE\\s+KEY\\s*$");

    @Test
    void everyMapperTableReferenceUsesExactPhysicalCase() throws IOException {
        Path sql = locateRepositoryRoot().resolve("sql");
        String baseline = Files.readString(sql.resolve("schema.sql"), StandardCharsets.UTF_8);
        SchemaDefinition definition = SchemaDefinition.baseline(baseline);
        try (Stream<Path> paths = Files.list(sql.resolve("migrations"))) {
            List<MigrationFile> migrations = paths.filter(path -> path.toString().endsWith(".sql"))
                    .map(path -> migration(path)).sorted(MigrationFile.numericOrder()).toList();
            for (MigrationFile migration : migrations) definition = definition.applying(migration.content());
        }
        Set<String> physicalNames = definition.tables.keySet();
        List<String> mismatches = new ArrayList<>();
        for (TableReference reference : tableReferences()) {
            if (!physicalNames.contains(reference.table())) {
                String caseMatch = physicalNames.stream().filter(name -> name.equalsIgnoreCase(reference.table()))
                        .findFirst().orElse("<missing>");
                mismatches.add(reference.mapper() + ": " + reference.table() + " -> " + caseMatch);
            }
        }
        assertTrue(mismatches.isEmpty(), "Mapper table references do not match physical schema exactly:\n" + String.join("\n", mismatches));
    }

    static List<TableReference> tableReferences() throws IOException {
        List<TableReference> references = new ArrayList<>();
        for (Path mapperRoot : List.of(locateRepositoryRoot().resolve("CoreService/src/main/resources/mapper"),
                locateRepositoryRoot().resolve("AssistService/src/main/resources/mapper"))) {
            try (Stream<Path> paths = Files.walk(mapperRoot)) {
                for (Path path : paths.filter(file -> file.toString().endsWith(".xml")).toList()) {
                    String xml = Files.readString(path, StandardCharsets.UTF_8).replaceAll("(?s)<[^>]+>", " ");
                    Matcher matcher = TABLE_REFERENCE.matcher(xml);
                    while (matcher.find()) {
                        if (matcher.group(1).equalsIgnoreCase("UPDATE") &&
                                DUPLICATE_KEY_PREFIX.matcher(xml.substring(0, matcher.start())).matches()) continue;
                        references.add(new TableReference(locateRepositoryRoot().relativize(path), matcher.group(2)));
                    }
                }
            }
        }
        return List.copyOf(references);
    }

    record TableReference(Path mapper, String table) {}

    private static MigrationFile migration(Path path) {
        try {
            return MigrationFile.from(path, "unused", Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read migration " + path.getFileName(), exception);
        }
    }

    private static Path locateRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        if (Files.isDirectory(current.resolve("sql"))) return current;
        if (Files.isRegularFile(current.resolve("schema.sql"))) return current.getParent();
        throw new IllegalStateException("Cannot locate repository root");
    }
}
