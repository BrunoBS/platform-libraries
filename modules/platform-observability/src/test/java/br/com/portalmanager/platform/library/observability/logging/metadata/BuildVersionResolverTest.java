package br.com.portalmanager.platform.library.observability.logging.metadata;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BuildVersionResolverTest {

    @Test
    void shouldPreferExplicitInfoBuildVersion() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("info.build.version", "2.4.1-RELEASE");

        ByteArrayResource buildInfo = resource("build.version=1.2.3");

        assertThat(BuildVersionResolver.resolve(environment, buildInfo))
                .isEqualTo("2.4.1-RELEASE");
    }

    @Test
    void shouldReadVersionFromBuildInfoWhenEnvironmentPropertyIsMissing() {
        MockEnvironment environment = new MockEnvironment();

        ByteArrayResource buildInfo = resource("""
                build.artifact=workspace-service
                build.name=workspace-service
                build.version=0.1.0-SNAPSHOT
                """);

        assertThat(BuildVersionResolver.resolve(environment, buildInfo))
                .isEqualTo("0.1.0-SNAPSHOT");
    }

    @Test
    void shouldReturnUnknownWhenNoVersionSourceIsAvailable() {
        MockEnvironment environment = new MockEnvironment();

        ByteArrayResource missingBuildVersion = resource("build.name=workspace-service");

        assertThat(BuildVersionResolver.resolve(environment, missingBuildVersion))
                .isEqualTo("unknown");
    }

    @Test
    void shouldIgnoreBlankConfiguredVersionAndUseBuildInfo() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("info.build.version", "   ");

        ByteArrayResource buildInfo = resource("build.version=1.0.1");

        assertThat(BuildVersionResolver.resolve(environment, buildInfo))
                .isEqualTo("1.0.1");
    }

    private ByteArrayResource resource(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
    }
}
