package br.com.portalmanager.platform.library.testing.authorization;

import br.com.portalmanager.platform.library.authorization.model.UserSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationSessionCustomizerTest {

    @Test
    void shouldCustomizeOnlySessionDataWithoutInheritance() {
        AuthorizationSessionCustomizer customizer = session -> session
                .userName("bruno.barbosa")
                .accountId("account-123")
                .groups("PM5_OWNER");

        AuthorizationSessionBuilder builder = AuthorizationSessionBuilder.builder();
        customizer.customize(builder);
        UserSession session = builder.build();

        assertThat(session.getUserName()).isEqualTo("bruno.barbosa");
        assertThat(session.getAccountId()).isEqualTo("account-123");
        assertThat(session.getGroups()).containsExactly("PM5_OWNER");
    }
}
