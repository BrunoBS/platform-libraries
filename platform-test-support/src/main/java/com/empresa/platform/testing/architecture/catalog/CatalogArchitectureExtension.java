package com.empresa.platform.testing.architecture.catalog;

import com.empresa.platform.testing.annotation.CatalogArchitectureTest;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects platform-catalog customizations and requires a specific consumer-side test.
 *
 * <p>The implementation intentionally relies on ArchUnit only for classpath bytecode
 * discovery. The consumer does not need to know ArchUnit's API.</p>
 */
public final class CatalogArchitectureExtension implements BeforeAllCallback {

    private static final Set<String> CATALOG_BASE_TYPES = Set.of(
            "com.empresa.platform.catalog.service.BaseCatalogService",
            "com.empresa.platform.catalog.validation.BaseCatalogValidator"
    );

    private static final Set<String> CUSTOMIZATION_HOOKS = Set.of(
            "additionalAllowedFilters",
            "additionalSpecification",
            "applyAdditionalFields",
            "isNameMutable",
            "validateSettings",
            "validateAdditionalFields",
            "validateAdditionalCatalogFields",
            "validateAdditionalIntegrity",
            "validateUniqueness",
            "relatedValue",
            "relatedField",
            "relatedEntityName",
            "relatedExistsAndIsActive",
            "existsByNameAndRelatedValue"
    );

    @Override
    public void beforeAll(ExtensionContext context) {
        Class<?> architectureTestClass = context.getRequiredTestClass();
        CatalogArchitectureTest annotation =
                architectureTestClass.getAnnotation(CatalogArchitectureTest.class);

        String[] basePackages = resolveBasePackages(annotation, architectureTestClass);
        JavaClasses classes = new ClassFileImporter().importPackages(basePackages);

        List<Violation> violations = classes.stream()
                .map(JavaClass::reflect)
                .filter(this::isConcreteClass)
                .filter(this::extendsCatalogBaseType)
                .map(this::toCustomization)
                .filter(customization -> !customization.hooks().isEmpty())
                .filter(customization -> !hasSpecificTest(customization.type(), classes))
                .map(this::toViolation)
                .toList();

        if (!violations.isEmpty()) {
            throw new AssertionError(formatViolations(basePackages, violations));
        }
    }

    private String[] resolveBasePackages(
            CatalogArchitectureTest annotation,
            Class<?> architectureTestClass
    ) {
        if (annotation != null && annotation.basePackages().length > 0) {
            return Arrays.stream(annotation.basePackages())
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .toArray(String[]::new);
        }

        String packageName = architectureTestClass.getPackageName();
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalStateException(
                    "@CatalogArchitectureTest requires basePackages when the test class is in the default package."
            );
        }
        return new String[]{packageName};
    }

    private boolean isConcreteClass(Class<?> type) {
        int modifiers = type.getModifiers();
        return !type.isInterface()
                && !type.isAnnotation()
                && !Modifier.isAbstract(modifiers);
    }

    private boolean extendsCatalogBaseType(Class<?> type) {
        Class<?> current = type.getSuperclass();
        while (current != null && current != Object.class) {
            if (CATALOG_BASE_TYPES.contains(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private Customization toCustomization(Class<?> type) {
        Set<String> hooks = Arrays.stream(type.getDeclaredMethods())
                .map(Method::getName)
                .filter(CUSTOMIZATION_HOOKS::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new Customization(type, Set.copyOf(hooks));
    }

    private boolean hasSpecificTest(Class<?> customizedType, JavaClasses classes) {
        Set<String> acceptedSimpleNames = acceptedTestNames(customizedType.getSimpleName());

        return classes.stream()
                .map(JavaClass::getSimpleName)
                .anyMatch(acceptedSimpleNames::contains);
    }

    private Set<String> acceptedTestNames(String customizedSimpleName) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add(customizedSimpleName + "Test");
        names.add(customizedSimpleName + "IntegrationTest");

        String domainName = removeKnownSuffix(customizedSimpleName);
        names.add(domainName + "Test");
        names.add(domainName + "IntegrationTest");

        return Set.copyOf(names);
    }

    private String removeKnownSuffix(String simpleName) {
        for (String suffix : List.of("Service", "Validator", "Mapper")) {
            if (simpleName.endsWith(suffix) && simpleName.length() > suffix.length()) {
                return simpleName.substring(0, simpleName.length() - suffix.length());
            }
        }
        return simpleName;
    }

    private Violation toViolation(Customization customization) {
        return new Violation(
                customization.type().getName(),
                customization.hooks(),
                acceptedTestNames(customization.type().getSimpleName())
        );
    }

    private String formatViolations(String[] basePackages, List<Violation> violations) {
        List<String> lines = new ArrayList<>();
        lines.add("Catalog architecture violations detected.");
        lines.add("Analyzed packages: " + String.join(", ", basePackages));
        lines.add("");
        lines.add("A class that customizes platform-catalog behavior must have a specific consumer-side test.");

        for (Violation violation : violations) {
            lines.add("");
            lines.add("- " + violation.className());
            lines.add("  customized hooks: " + String.join(", ", violation.hooks()));
            lines.add("  expected one of: " + String.join(", ", violation.acceptedTestNames()));
        }

        lines.add("");
        lines.add("Add a focused test for the customized behavior or remove the unnecessary override.");
        return String.join(System.lineSeparator(), lines);
    }

    private record Customization(
            Class<?> type,
            Set<String> hooks
    ) {
    }

    private record Violation(
            String className,
            Set<String> hooks,
            Set<String> acceptedTestNames
    ) {
    }
}
