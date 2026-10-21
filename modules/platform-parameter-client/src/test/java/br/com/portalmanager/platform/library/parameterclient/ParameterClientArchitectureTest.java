package br.com.portalmanager.platform.library.parameterclient;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.*;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ParameterClientArchitectureTest {
    @Test
    void readOnlyClientDoesNotKnowDriversProducerOrSchedulers() throws Exception {
        var classes = new ClassFileImporter().withImportOption(com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages("br.com.portalmanager.platform.library.parameterclient");
        noClasses().that().resideInAnyPackage("..parameterclient.internal..", "..parameterclient.configuration..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.data.redis..", "io.lettuce..", "redis.clients..",
                        "..workspace..", "java.sql..", "org.springframework.scheduling..").check(classes);
        assertThat(java.util.Arrays.stream(ParameterProvider.class.getDeclaredMethods()).map(m -> m.getName()))
                .containsOnly("get", "matches");
        assertThat(Files.readString(Path.of("../platform-redis/pom.xml")))
                .doesNotContain("platform-parameter-client");
        assertThat(Files.readString(Path.of("../platform-starter/pom.xml")))
                .doesNotContain("platform-parameter-client");
        String pom = Files.readString(Path.of("pom.xml"));
        assertThat(pom).contains("<artifactId>platform-redis</artifactId>")
                .doesNotContain("<artifactId>lettuce-core</artifactId>", "<artifactId>jedis</artifactId>");
    }
}
