package br.com.portalmanager.platform.library.testing.redis;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;

import org.junit.jupiter.api.Test;

import java.util.List;

class RedisCustomizerTest {
    @WithRedis
    static class Default {}

    @WithRedis
    static class SameImage {}

    @WithRedis(cleanupBeforeEach = false)
    static class CleanupDisabled {}

    @WithRedis(image = "redis:7.4.8-alpine")
    static class Versioned {}

    @WithRedis(image = "redis:latest")
    static class Invalid {}

    @Test
    void discoversOnlyAnnotatedTestsAndValidatesImages() {
        var factory = new RedisTestContextCustomizerFactory();
        assertThat(factory.createContextCustomizer(getClass(), List.of())).isNull();
        assertThat(factory.createContextCustomizer(Default.class, List.of()))
                .isEqualTo(factory.createContextCustomizer(SameImage.class, List.of()));
        assertThat(factory.createContextCustomizer(Default.class, List.of()))
                .isNotEqualTo(factory.createContextCustomizer(CleanupDisabled.class, List.of()));
        assertThat(factory.createContextCustomizer(Default.class, List.of()))
                .isNotEqualTo(factory.createContextCustomizer(Versioned.class, List.of()));
        assertThatThrownBy(() -> factory.createContextCustomizer(Invalid.class, List.of()))
                .isInstanceOf(PlatformConfigurationException.class);
    }

    @Test
    void namespaceIsUniquePerContextAndFixtureHasNoProductionDomainDependency() {
        var factory = new RedisTestContextCustomizerFactory();
        var customizer = factory.createContextCustomizer(Default.class, List.of());
        try (var first = new org.springframework.context.support.GenericApplicationContext();
                var second = new org.springframework.context.support.GenericApplicationContext()) {
            customizer.customizeContext(first, null);
            customizer.customizeContext(second, null);
            assertThat(first.getEnvironment().getProperty("platform.redis.namespace"))
                    .isNotEqualTo(second.getEnvironment().getProperty("platform.redis.namespace"));
        }
        var classes =
                new com.tngtech.archunit.core.importer.ClassFileImporter()
                        .importPackages("br.com.portalmanager.platform.library.testing.redis");
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..library.redis..",
                        "..testing.database..",
                        "..testing.kafka..",
                        "..testing.http..",
                        "..testing.cloud..")
                .check(classes);
    }
}
