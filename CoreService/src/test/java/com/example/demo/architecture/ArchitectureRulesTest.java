package com.example.demo.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

class ArchitectureRulesTest {
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.demo");

    @Test
    void noStandardStreamsOrStackTraces() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(CLASSES);
    }

    @Test
    void noJavaUtilLogging() {
        NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(CLASSES);
    }

    @Test
    void noAutowiredFields() {
        freeze(fields().that().areAnnotatedWith(Autowired.class)
                .should().notBeAnnotatedWith(Autowired.class).allowEmptyShould(true))
                .check(CLASSES);
    }

    @Test
    void controllersDoNotDependOnPersistence() {
        freeze(noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAnyPackage("..mapper..", "..dao.."))
                .check(CLASSES);
    }

    @Test
    void servicesDoNotDependOnControllers() {
        noClasses().that().resideInAnyPackage("..service..", "..serviceimpl..")
                .should().dependOnClassesThat().resideInAPackage("..controller..")
                .check(CLASSES);
    }

    @Test
    void servicesDoNotDependOnResponseEntity() {
        freeze(noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.http.ResponseEntity"))
                .check(CLASSES);
    }

    @Test
    void noNewLegacyResponseWrapperDependencies() {
        freeze(noClasses().that().doNotHaveFullyQualifiedName("com.example.demo.util.BaseResponse")
                .should().dependOnClassesThat().haveFullyQualifiedName("com.example.demo.util.BaseResponse"))
                .check(CLASSES);
    }

    @Test
    void bearerExtractionHasOneOwner() {
        // shortcut: only extraction method names are visible here; diff checks must inspect Bearer string operations.
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods()
                .that().haveNameMatching("(?i).*extract.*token.*")
                .should().beDeclaredIn(com.example.demo.util.TokenUtils.class)
                .check(CLASSES);
    }
}
