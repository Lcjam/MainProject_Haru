package com.haru.migration;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperPhysicalNameTest {
    private static final Pattern TABLE_REFERENCE = Pattern.compile(
            "(?i)\\b(FROM|JOIN|INTO|UPDATE|DELETE\\s+FROM)\\s+`?([A-Za-z_][A-Za-z0-9_]*)`?");
    private static final Pattern DUPLICATE_KEY_PREFIX = Pattern.compile("(?is).*ON\\s+DUPLICATE\\s+KEY\\s*$");
    private static final Set<String> DIRECT_SQL_ANNOTATIONS = Set.of("Select", "Insert", "Update", "Delete");

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
        List<String> mismatches = physicalNameMismatches(physicalNames, tableReferences());
        assertTrue(mismatches.isEmpty(), "Mapper table references do not match physical schema exactly:\n" + String.join("\n", mismatches));
    }

    static List<String> physicalNameMismatches(Set<String> physicalNames, List<TableReference> references) {
        List<String> mismatches = new ArrayList<>();
        for (TableReference reference : references) {
            if (!physicalNames.contains(reference.table())) {
                String caseMatch = physicalNames.stream().filter(name -> name.equalsIgnoreCase(reference.table()))
                        .findFirst().orElse("<missing>");
                mismatches.add(reference.mapper() + ": " + reference.table() + " -> " + caseMatch);
            }
        }
        return List.copyOf(mismatches);
    }

    @Test
    void javaSqlAnnotationsExposeConcatenatedWrongCaseReferencesWithoutCommentFalsePositives() {
        Path mapper = Path.of("SyntheticMapper.java");
        String source = """
                interface SyntheticMapper {
                    // @Select("SELECT * FROM ignored_comment")
                    String note = "@Select(\\\"SELECT * FROM ignored_string\\\")";

                    @Select(value = "SELECT * FROM chatrooms " +
                            "JOIN products p ON p.id = chatrooms.product_id",
                            databaseId = "FROM ignored_attribute")
                    Object find();

                    @SelectProvider(type = SqlProvider.class, method = "FROM ignored_provider")
                    Object provided();
                }
                """;

        List<TableReference> references = javaAnnotationTableReferences(mapper, source);

        assertEquals(List.of("SyntheticMapper.java: products -> Products"),
                physicalNameMismatches(Set.of("chatrooms", "Products"), references));
    }

    @Test
    void javaSqlAnnotationsRejectExternalConstantsInsteadOfSilentlySkippingThem() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> javaAnnotationTableReferences(Path.of("ConstantMapper.java"),
                        "interface ConstantMapper { @Select(SQL) Object find(); }"));

        assertTrue(exception.getMessage().contains("Unsupported non-literal SQL annotation value"));
    }

    static List<TableReference> tableReferences() throws IOException {
        List<TableReference> references = new ArrayList<>();
        for (Path mapperRoot : List.of(locateRepositoryRoot().resolve("CoreService/src/main/resources/mapper"),
                locateRepositoryRoot().resolve("AssistService/src/main/resources/mapper"))) {
            try (Stream<Path> paths = Files.walk(mapperRoot)) {
                for (Path path : paths.filter(file -> file.toString().endsWith(".xml")).toList()) {
                    String xml = Files.readString(path, StandardCharsets.UTF_8).replaceAll("(?s)<[^>]+>", " ");
                    addTableReferences(locateRepositoryRoot().relativize(path), xml, references);
                }
            }
        }
        for (Path mapperRoot : List.of(
                locateRepositoryRoot().resolve("CoreService/src/main/java/com/example/demo/mapper"),
                locateRepositoryRoot().resolve("AssistService/src/main/java/com/example/demo/dao"))) {
            try (Stream<Path> paths = Files.walk(mapperRoot)) {
                for (Path path : paths.filter(file -> file.toString().endsWith(".java")).toList()) {
                    Path relativePath = locateRepositoryRoot().relativize(path);
                    references.addAll(javaAnnotationTableReferences(
                            relativePath, Files.readString(path, StandardCharsets.UTF_8)));
                }
            }
        }
        return List.copyOf(references);
    }

    static List<TableReference> javaAnnotationTableReferences(Path mapper, String source) {
        List<TableReference> references = new ArrayList<>();
        CompilationUnitTree unit = parseJava(mapper, source);
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitAnnotation(AnnotationTree annotation, Void unused) {
                String annotationName = annotation.getAnnotationType().toString();
                annotationName = annotationName.substring(annotationName.lastIndexOf('.') + 1);
                if (DIRECT_SQL_ANNOTATIONS.contains(annotationName)) {
                    addTableReferences(mapper, sqlAnnotationValue(mapper, annotation), references);
                }
                return super.visitAnnotation(annotation, unused);
            }
        }.scan(unit, null);
        return List.copyOf(references);
    }

    private static CompilationUnitTree parseJava(Path mapper, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler is required to inspect " + mapper);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaFileObject sourceFile = new SimpleJavaFileObject(URI.create("string:///MapperSource.java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
        try {
            JavacTask task = (JavacTask) compiler.getTask(null, null, diagnostics,
                    List.of("-proc:none"), null, List.of(sourceFile));
            CompilationUnitTree unit = task.parse().iterator().next();
            List<String> errors = diagnostics.getDiagnostics().stream()
                    .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                    .map(diagnostic -> diagnostic.getLineNumber() + ": " + diagnostic.getMessage(null))
                    .toList();
            if (!errors.isEmpty()) {
                throw new IllegalStateException("Cannot parse mapper " + mapper + ":\n" + String.join("\n", errors));
            }
            return unit;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot parse mapper " + mapper, exception);
        }
    }

    private static String sqlAnnotationValue(Path mapper, AnnotationTree annotation) {
        List<? extends ExpressionTree> arguments = annotation.getArguments();
        if (arguments.size() == 1 && !(arguments.get(0) instanceof AssignmentTree)) {
            return stringConstant(mapper, arguments.get(0));
        }
        for (ExpressionTree argument : arguments) {
            if (argument instanceof AssignmentTree assignment && assignment.getVariable().toString().equals("value")) {
                return stringConstant(mapper, assignment.getExpression());
            }
        }
        throw new IllegalStateException("Direct SQL annotation has no value in " + mapper);
    }

    private static String stringConstant(Path mapper, ExpressionTree expression) {
        if (expression instanceof LiteralTree literal && literal.getValue() instanceof String value) return value;
        if (expression instanceof BinaryTree binary && binary.getKind() == com.sun.source.tree.Tree.Kind.PLUS) {
            return stringConstant(mapper, binary.getLeftOperand()) + stringConstant(mapper, binary.getRightOperand());
        }
        if (expression instanceof ParenthesizedTree parenthesized) {
            return stringConstant(mapper, parenthesized.getExpression());
        }
        if (expression instanceof NewArrayTree array && array.getInitializers() != null) {
            List<String> lines = new ArrayList<>();
            for (ExpressionTree initializer : array.getInitializers()) {
                lines.add(stringConstant(mapper, initializer));
            }
            return String.join(" ", lines);
        }
        throw new IllegalStateException("Unsupported non-literal SQL annotation value in " + mapper + ": " + expression);
    }

    private static void addTableReferences(Path mapper, String sql, List<TableReference> references) {
        Matcher matcher = TABLE_REFERENCE.matcher(sql);
        while (matcher.find()) {
            if (matcher.group(1).equalsIgnoreCase("UPDATE") &&
                    DUPLICATE_KEY_PREFIX.matcher(sql.substring(0, matcher.start())).matches()) continue;
            references.add(new TableReference(mapper, matcher.group(2)));
        }
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
