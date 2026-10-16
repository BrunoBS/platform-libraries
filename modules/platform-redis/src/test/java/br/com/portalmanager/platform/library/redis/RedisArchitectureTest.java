package br.com.portalmanager.platform.library.redis;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;

import org.junit.jupiter.api.Test;

class RedisArchitectureTest {
    @Test
    void apiDoesNotExposeDriversOrDependOnConsumerDomains() {
        var classes =
                new ClassFileImporter()
                        .importPackages("br.com.portalmanager.platform.library.redis");
        noClasses()
                .that()
                .resideInAnyPackage(
                        "..redis.core..",
                        "..redis.operations..",
                        "..redis.pubsub..",
                        "..redis.stream..",
                        "..redis.annotation..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.data.redis..", "io.lettuce..", "redis.clients..")
                .check(classes);
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..messagequeue..", "..workspace..", "..authorization..")
                .check(classes);
    }
}
