package br.com.portalmanager.core.testing;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfrastructureDependencyOptionalityTest {

    private static final Set<String> OPTIONAL_INFRASTRUCTURE = Set.of(
            "org.springframework.boot:spring-boot-starter-jdbc",
            "com.mysql:mysql-connector-j",
            "org.springframework.boot:spring-boot-testcontainers",
            "org.springframework.boot:spring-boot-starter-kafka",
            "org.testcontainers:testcontainers-mysql",
            "org.testcontainers:testcontainers-kafka",
            "org.testcontainers:testcontainers-junit-jupiter"
    );

    @Test
    void shouldKeepInfrastructureDependenciesOptionalForConsumers() throws Exception {
        var document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile());

        var dependencies = document.getElementsByTagName("dependency");
        var verified = new HashSet<String>();

        for (int i = 0; i < dependencies.getLength(); i++) {
            var dependency = (Element) dependencies.item(i);
            var coordinate = childText(dependency, "groupId") + ":" + childText(dependency, "artifactId");

            if (OPTIONAL_INFRASTRUCTURE.contains(coordinate)) {
                assertEquals("true", childText(dependency, "optional"),
                        () -> coordinate + " must remain optional");
                verified.add(coordinate);
            }
        }

        assertEquals(OPTIONAL_INFRASTRUCTURE, verified);
    }

    private static String childText(Element element, String tagName) {
        var nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }
}
