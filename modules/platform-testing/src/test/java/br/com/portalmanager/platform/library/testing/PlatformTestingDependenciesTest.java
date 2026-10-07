package br.com.portalmanager.platform.library.testing;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformTestingDependenciesTest {

    private static final Set<String> FIXTURE_DEPENDENCIES = Set.of(
            "br.com.portalmanager.platform.library:platform-authorization",
            "org.springframework.boot:spring-boot-starter-web",
            "org.springframework.boot:spring-boot-starter-kafka",
            "org.testcontainers:testcontainers-kafka",
            "org.testcontainers:testcontainers-junit-jupiter",
            "org.testcontainers:testcontainers-localstack",
            "org.testcontainers:testcontainers-azure",
            "org.testcontainers:testcontainers-mssqlserver",
            "com.microsoft.sqlserver:mssql-jdbc",
            "software.amazon.awssdk:sqs",
            "software.amazon.awssdk:s3",
            "com.azure:azure-messaging-servicebus",
            "com.azure:azure-storage-blob",
            "com.mysql:mysql-connector-j",
            "org.springframework.boot:spring-boot-testcontainers",
            "org.testcontainers:testcontainers-mysql"
    );

    @Test
    void shouldExposeFixtureDependenciesTransitivelyFromTheSingleLibraryDependency() throws Exception {
        var document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile());

        var dependencies = document.getElementsByTagName("dependency");
        var verifiedDependencies = new HashSet<String>();

        for (int i = 0; i < dependencies.getLength(); i++) {
            var dependency = (Element) dependencies.item(i);
            var coordinate = childText(dependency, "groupId") + ":" + childText(dependency, "artifactId");

            if (FIXTURE_DEPENDENCIES.contains(coordinate)) {
                assertEquals("", childText(dependency, "optional"),
                        () -> coordinate + " must be available transitively to platform-testing consumers");
                verifiedDependencies.add(coordinate);
            }
        }

        assertEquals(FIXTURE_DEPENDENCIES, verifiedDependencies);

        var jdbcStarter = findDependency(dependencies, "org.springframework.boot:spring-boot-starter-jdbc");
        assertEquals("true", childText(jdbcStarter, "optional"),
                "JDBC starter stays optional to avoid activating datasource auto-configuration in consumers without a database");
    }

    private static Element findDependency(org.w3c.dom.NodeList dependencies, String coordinate) {
        for (int i = 0; i < dependencies.getLength(); i++) {
            var dependency = (Element) dependencies.item(i);
            var actual = childText(dependency, "groupId") + ":" + childText(dependency, "artifactId");
            if (coordinate.equals(actual)) {
                return dependency;
            }
        }
        throw new AssertionError("Missing dependency " + coordinate);
    }

    private static String childText(Element element, String tagName) {
        var nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }
}
