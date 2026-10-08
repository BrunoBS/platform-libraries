package br.com.portalmanager.platform.library.testing;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PlatformTestingDependenciesTest {

    private static final Set<String> FEATURE_DEPENDENCIES = Set.of(
            "org.springframework.boot:spring-boot-starter-web",
            "org.springframework.boot:spring-boot-starter-kafka",
            "org.springframework.boot:spring-boot-starter-jdbc",
            "org.testcontainers:testcontainers-kafka",
            "org.testcontainers:testcontainers-localstack",
            "org.testcontainers:testcontainers-azure",
            "org.testcontainers:testcontainers-mysql",
            "software.amazon.awssdk:sqs",
            "software.amazon.awssdk:s3",
            "com.azure:azure-messaging-servicebus",
            "com.azure:azure-storage-blob",
            "com.mysql:mysql-connector-j",
            "br.com.portalmanager.platform.library:platform-authorization",
            "br.com.portalmanager.platform.library:platform-messaging"
    );

    @Test
    void coreDoesNotPullFeatureSpecificDependencies() throws Exception {
        var document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile());
        var dependencies = document.getElementsByTagName("dependency");
        var declared = new HashSet<String>();
        for (int i = 0; i < dependencies.getLength(); i++) {
            var dependency = (Element) dependencies.item(i);
            declared.add(text(dependency, "groupId") + ":" + text(dependency, "artifactId"));
        }
        FEATURE_DEPENDENCIES.forEach(coordinate -> assertFalse(declared.contains(coordinate), coordinate));
    }


    @Test
    void testingArtifactsDoNotDeclareH2() throws Exception {
        var artifacts = Set.of(
                "platform-testing-core",
                "platform-testing-http",
                "platform-testing-authorization",
                "platform-testing-kafka",
                "platform-testing-database",
                "platform-testing-cloud-aws",
                "platform-testing-cloud-azure"
        );
        for (String artifact : artifacts) {
            var pom = Path.of("..", artifact, "pom.xml");
            var document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(pom.toFile());
            var dependencies = document.getElementsByTagName("dependency");
            for (int i = 0; i < dependencies.getLength(); i++) {
                var dependency = (Element) dependencies.item(i);
                var coordinate = text(dependency, "groupId") + ":" + text(dependency, "artifactId");
                assertFalse("com.h2database:h2".equals(coordinate), artifact + " must not declare H2");
            }
        }
    }

    private static String text(Element element, String tagName) {
        var nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }
}
