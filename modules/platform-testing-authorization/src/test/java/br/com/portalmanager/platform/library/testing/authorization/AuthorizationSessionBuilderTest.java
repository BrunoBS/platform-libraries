package br.com.portalmanager.platform.library.testing.authorization;

import br.com.portalmanager.platform.library.authorization.model.UserSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationSessionBuilderTest {

    @Test
    void shouldCreateDefaultSession() {
        UserSession session = AuthorizationSessionBuilder.builder().build();

        assertThat(session.getUserName()).isEqualTo("integration-test");
        assertThat(session.getAccountId()).isEqualTo("account-test");
        assertThat(session.getGroups()).containsExactly("USER");
        assertThat(session.getExpirationTime()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void shouldCustomizeSessionForMicroserviceScenario() {
        UserSession session = AuthorizationSessionBuilder.builder()
                .userName("bruno.barbosa")
                .accountId("account-123")
                .applicationId("application-456")
                .environmentId("environment-789")
                .groups("PM5_OWNER", "ADMIN")
                .addAuthorizerGroup("GRP_APP_DEV_ADMIN", "ADMIN", "DEV", "APP")
                .build();

        assertThat(session.getUserName()).isEqualTo("bruno.barbosa");
        assertThat(session.getAccountId()).isEqualTo("account-123");
        assertThat(session.getApplicationId()).isEqualTo("application-456");
        assertThat(session.getEnvironmentId()).isEqualTo("environment-789");
        assertThat(session.getGroups()).containsExactlyInAnyOrder("PM5_OWNER", "ADMIN");
        assertThat(session.getAuthorizerGroups()).hasSize(1);
    }
}
