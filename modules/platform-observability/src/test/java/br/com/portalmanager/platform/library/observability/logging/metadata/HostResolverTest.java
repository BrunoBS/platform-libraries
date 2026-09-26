package br.com.portalmanager.platform.library.observability.logging.metadata;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class HostResolverTest {

    @Test
    void shouldPreferConfiguredHostname() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("HOSTNAME", "workspace-task-01");

        assertThat(HostResolver.resolve(environment, "local-machine"))
                .isEqualTo("workspace-task-01");
    }

    @Test
    void shouldUseLocalHostnameWhenHostnameIsMissing() {
        MockEnvironment environment = new MockEnvironment();

        assertThat(HostResolver.resolve(environment, "MacBook-Pro-de-Bruno.local"))
                .isEqualTo("MacBook-Pro-de-Bruno.local");
    }

    @Test
    void shouldUseLocalHostnameWhenConfiguredHostnameIsBlank() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("HOSTNAME", "   ");

        assertThat(HostResolver.resolve(environment, "ip-10-0-14-73.ec2.internal"))
                .isEqualTo("ip-10-0-14-73.ec2.internal");
    }

    @Test
    void shouldUseLocalHostnameWhenConfiguredHostnameIsLiteralNull() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("HOSTNAME", "null");

        assertThat(HostResolver.resolve(environment, "ip-10-0-14-73.ec2.internal"))
                .isEqualTo("ip-10-0-14-73.ec2.internal");
    }

    @Test
    void shouldReturnUnknownWhenNoHostnameCanBeResolved() {
        MockEnvironment environment = new MockEnvironment();

        assertThat(HostResolver.resolve(environment, "   "))
                .isEqualTo("unknown-host");
    }
}
