package br.com.portalmanager.platform.library.testing.authorization;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformTestingAuthorizationDependenciesTest {

    private static final Set<String> REQUIRED_DEPENDENCIES = Set.of(
            "br.com.portalmanager.platform.library:platform-testing-core",
            "br.com.portalmanager.platform.library:platform-authorization",
            "br.com.portalmanager.platform.library:platform-testing-http"
    );

    @Test
    void shouldComposeGenericTestingSupportWithAuthorizationFixtures() throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile());
        NodeList dependencies = document.getElementsByTagName("dependency");
        Set<String> found = new HashSet<>();

        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            String coordinate = childText(dependency, "groupId") + ":" + childText(dependency, "artifactId");
            if (REQUIRED_DEPENDENCIES.contains(coordinate)) {
                found.add(coordinate);
            }
        }

        assertEquals(REQUIRED_DEPENDENCIES, found);
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            String coordinate = childText(dependency, "groupId") + ":" + childText(dependency, "artifactId");
            org.junit.jupiter.api.Assertions.assertNotEquals("org.wiremock:wiremock-standalone", coordinate);
        }
    }

    private static String childText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }
}
