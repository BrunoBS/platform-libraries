package br.com.portalmanager.platform.library.testing;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfrastructureDependencyOptionalityTest {

    private static final Set<String> MYSQL_FIXTURE_DEPENDENCIES = Set.of(
            "com.mysql:mysql-connector-j",
            "org.springframework.boot:spring-boot-testcontainers",
            "org.testcontainers:testcontainers-mysql"
    );

    private static final Set<String> OPTIONAL_INFRASTRUCTURE = Set.of(
            "org.springframework.boot:spring-boot-starter-jdbc",
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
            "com.azure:azure-storage-blob"
    );

    @Test
    void shouldKeepOptionalInfrastructureOptionalAndExposeMySqlFixtureDependencies() throws Exception {
        var document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile());

        var dependencies = document.getElementsByTagName("dependency");
        var verifiedOptional = new HashSet<String>();
        var verifiedMySqlFixture = new HashSet<String>();

        for (int i = 0; i < dependencies.getLength(); i++) {
            var dependency = (Element) dependencies.item(i);
            var coordinate = childText(dependency, "groupId") + ":" + childText(dependency, "artifactId");

            if (OPTIONAL_INFRASTRUCTURE.contains(coordinate)) {
                assertEquals("true", childText(dependency, "optional"),
                        () -> coordinate + " must remain optional");
                verifiedOptional.add(coordinate);
            }

            if (MYSQL_FIXTURE_DEPENDENCIES.contains(coordinate)) {
                assertEquals("", childText(dependency, "optional"),
                        () -> coordinate + " must be available to consumers of the MySQL fixture");
                verifiedMySqlFixture.add(coordinate);
            }
        }

        assertEquals(OPTIONAL_INFRASTRUCTURE, verifiedOptional);
        assertEquals(MYSQL_FIXTURE_DEPENDENCIES, verifiedMySqlFixture);
    }

    private static String childText(Element element, String tagName) {
        var nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }
}
