package br.com.portalmanager.platform.library.testing.architecture;

import br.com.portalmanager.platform.library.testing.annotation.CoversClasses;
import br.com.portalmanager.platform.library.testing.annotation.PlatformArchitectureTest;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Generic architecture guard for classes that customize platform behavior.
 *
 * <p>The rule is intentionally domain agnostic: catalog, CRUD, authorization,
 * messaging or any future platform module can participate without adding a
 * specialized architecture test implementation.</p>
 */
public final class PlatformArchitectureExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        Class<?> architectureTestClass = context.getRequiredTestClass();
        PlatformArchitectureTest annotation =
                architectureTestClass.getAnnotation(PlatformArchitectureTest.class);

        String[] basePackages = resolveBasePackages(annotation, architectureTestClass);
        String[] observedBasePackages = resolveObservedBasePackages(annotation);

        JavaClasses classes = new ClassFileImporter().importPackages(basePackages);

        Set<String> availableTestNames = StreamSupport.stream(classes.spliterator(), false)
                .filter(this::isTestClass)
                .map(JavaClass::getName)
                .collect(
                        LinkedHashSet::new,
                        LinkedHashSet::add,
                        LinkedHashSet::addAll
                );

        Set<Class<?>> explicitCoverage = findExplicitCoverage(classes);

        List<Violation> violations = StreamSupport.stream(classes.spliterator(), false)
                .filter(javaClass -> !isTestClass(javaClass))
                .map(JavaClass::reflect)
                .filter(this::isConcreteClass)
                .map(type -> customizationFor(type, observedBasePackages))
                .filter(customization -> !customization.overriddenMethods().isEmpty())
                .filter(customization ->
                        !explicitCoverage.contains(customization.type())
                                && !hasConventionBasedTest(customization.type(), availableTestNames))
                .map(this::toViolation)
                .toList();

        if (!violations.isEmpty()) {
            throw new AssertionError(formatViolations(
                    basePackages,
                    observedBasePackages,
                    violations
            ));
        }
    }

    private String[] resolveBasePackages(
            PlatformArchitectureTest annotation,
            Class<?> architectureTestClass
    ) {
        if (annotation != null && annotation.basePackages().length > 0) {
            return clean(annotation.basePackages());
        }

        String packageName = architectureTestClass.getPackageName();
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalStateException(
                    "@PlatformArchitectureTest requires basePackages when the test class is in the default package."
            );
        }

        return new String[]{packageName};
    }

    private String[] resolveObservedBasePackages(PlatformArchitectureTest annotation) {
        if (annotation == null || annotation.observedBasePackages().length == 0) {
            return new String[]{"br.com.portalmanager.platform"};
        }
        return clean(annotation.observedBasePackages());
    }

    private String[] clean(String[] values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toArray(String[]::new);
    }

    private boolean isConcreteClass(Class<?> type) {
        int modifiers = type.getModifiers();
        return !type.isInterface()
                && !type.isAnnotation()
                && !type.isEnum()
                && !type.isRecord()
                && !Modifier.isAbstract(modifiers);
    }

    private Customization customizationFor(
            Class<?> type,
            String[] observedBasePackages
    ) {
        Map<String, String> overriddenMethods = new LinkedHashMap<>();

        for (Method method : type.getDeclaredMethods()) {
            if (!isOverridableImplementation(method)) {
                continue;
            }

            Method inheritedMethod = findInheritedDeclaration(
                    type,
                    method.getName(),
                    method.getParameterTypes()
            );

            if (inheritedMethod != null
                    && !Modifier.isAbstract(inheritedMethod.getModifiers())
                    && matchesAnyPackage(inheritedMethod.getDeclaringClass(), observedBasePackages)) {
                overriddenMethods.put(
                        methodSignature(method),
                        inheritedMethod.getDeclaringClass().getName()
                );
            }
        }

        return new Customization(type, Map.copyOf(overriddenMethods));
    }

    private boolean isOverridableImplementation(Method method) {
        int modifiers = method.getModifiers();
        return !Modifier.isStatic(modifiers)
                && !Modifier.isPrivate(modifiers)
                && !method.isSynthetic()
                && !method.isBridge();
    }

    private Method findInheritedDeclaration(
            Class<?> type,
            String methodName,
            Class<?>[] parameterTypes
    ) {
        Class<?> superclass = type.getSuperclass();
        while (superclass != null && superclass != Object.class) {
            Method declared = declaredMethod(superclass, methodName, parameterTypes);
            if (declared != null && !Modifier.isPrivate(declared.getModifiers())) {
                return declared;
            }

            Method fromInterfaces = findInInterfaces(
                    superclass.getInterfaces(),
                    methodName,
                    parameterTypes
            );
            if (fromInterfaces != null) {
                return fromInterfaces;
            }

            superclass = superclass.getSuperclass();
        }

        return findInInterfaces(
                type.getInterfaces(),
                methodName,
                parameterTypes
        );
    }

    private Method findInInterfaces(
            Class<?>[] interfaces,
            String methodName,
            Class<?>[] parameterTypes
    ) {
        for (Class<?> contract : interfaces) {
            Method declared = declaredMethod(contract, methodName, parameterTypes);
            if (declared != null) {
                return declared;
            }

            Method nested = findInInterfaces(
                    contract.getInterfaces(),
                    methodName,
                    parameterTypes
            );
            if (nested != null) {
                return nested;
            }
        }

        return null;
    }

    private Method declaredMethod(
            Class<?> type,
            String methodName,
            Class<?>[] parameterTypes
    ) {
        try {
            return type.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private boolean matchesAnyPackage(
            Class<?> type,
            String[] observedBasePackages
    ) {
        String packageName = type.getPackageName();
        return Arrays.stream(observedBasePackages)
                .anyMatch(basePackage -> packageMatches(packageName, basePackage));
    }

    private Set<Class<?>> findExplicitCoverage(JavaClasses classes) {
        Set<Class<?>> covered = new LinkedHashSet<>();

        StreamSupport.stream(classes.spliterator(), false)
                .filter(this::isTestClass)
                .map(JavaClass::reflect)
                .forEach(testClass -> {
                    CoversClasses annotation = testClass.getAnnotation(CoversClasses.class);
                    if (annotation != null) {
                        covered.addAll(Arrays.asList(annotation.value()));
                    }
                });

        return Set.copyOf(covered);
    }

    static boolean packageMatches(String packageName, String basePackage) {
        return packageName.equals(basePackage)
                || packageName.startsWith(basePackage + ".");
    }

    static boolean hasConventionBasedTest(
            String productionClassName,
            Set<String> availableTestNames
    ) {
        int packageSeparator = productionClassName.lastIndexOf('.');
        String packageName = packageSeparator < 0 ? "" : productionClassName.substring(0, packageSeparator);
        String simpleName = productionClassName.substring(packageSeparator + 1);
        return Set.of(
                        simpleName + "Test",
                        simpleName + "IntegrationTest",
                        simpleName + "IT"
                ).stream()
                .map(testName -> packageName.isEmpty() ? testName : packageName + "." + testName)
                .anyMatch(availableTestNames::contains);
    }

    private boolean isTestClass(JavaClass javaClass) {
        URI uri = javaClass.getSource()
                .map(source -> source.getUri())
                .orElse(null);

        if (uri != null && uri.toString().contains("/test-classes/")) {
            return true;
        }

        String simpleName = javaClass.getSimpleName();
        return simpleName.endsWith("Test")
                || simpleName.endsWith("IntegrationTest")
                || simpleName.endsWith("IT");
    }

    private String methodSignature(Method method) {
        String parameters = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return method.getName() + "(" + parameters + ")";
    }

    private Violation toViolation(Customization customization) {
        String simpleName = customization.type().getSimpleName();
        return new Violation(
                customization.type().getName(),
                customization.overriddenMethods(),
                Set.of(
                        simpleName + "Test",
                        simpleName + "IntegrationTest",
                        simpleName + "IT"
                )
        );
    }

    private String formatViolations(
            String[] basePackages,
            String[] observedBasePackages,
            List<Violation> violations
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("Platform architecture violations detected.");
        lines.add("Analyzed application packages: " + String.join(", ", basePackages));
        lines.add("Observed platform packages: " + String.join(", ", observedBasePackages));
        lines.add("");
        lines.add("A class that overrides platform behavior must have focused test coverage.");
        lines.add("Coverage can be provided by naming convention or @CoversClasses.");

        for (Violation violation : violations) {
            lines.add("");
            lines.add("- " + violation.className());
            violation.overriddenMethods().forEach((method, owner) ->
                    lines.add("  override: " + method + " <- " + owner)
            );
            lines.add("  expected one of: "
                    + String.join(", ", violation.acceptedTestNames()));
            lines.add("  or annotate an existing test with @CoversClasses(...).");
        }

        return String.join(System.lineSeparator(), lines);
    }

    private record Customization(
            Class<?> type,
            Map<String, String> overriddenMethods
    ) {
    }

    private record Violation(
            String className,
            Map<String, String> overriddenMethods,
            Set<String> acceptedTestNames
    ) {
    }
}
