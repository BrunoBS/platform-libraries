package br.com.portalmanager.platform.library.testing.authorization;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformTestingAuthorizationDependenciesTest {

    private static final Set<String> REQUIRED_DEPENDENCIES = Set.of(
            "br.com.portalmanager.platform.library:platform-testing-core",
            "br.com.portalmanager.platform.library:platform-authorization",
            "org.wiremock:wiremock-standalone"
    );

    @Test
    void shouldComposeGenericTestingSupportWithAuthorizationFixtures() throws Exception {
        var document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile());
        var dependencies = document.getElementsByTagName("dependency");
        var found = new HashSet<String>();

        for (int i = 0; i < dependencies.getLength(); i++) {
            var dependency = (Element) dependencies.item(i);
            var coordinate = childText(dependency, "groupId") + ":" + childText(dependency, "artifactId");
            if (REQUIRED_DEPENDENCIES.contains(coordinate)) {
                found.add(coordinate);
            }
        }

        assertEquals(REQUIRED_DEPENDENCIES, found);
    }

    private static String childText(Element element, String tagName) {
        var nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }
}
