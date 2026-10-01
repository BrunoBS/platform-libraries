package br.com.portalmanager.platform.library.starter;

import br.com.portalmanager.platform.library.authorization.config.AuthorizationMetadataRegistry;
import br.com.portalmanager.platform.library.messaging.repository.ApiMessageRepository;
import br.com.portalmanager.platform.library.messaging.resolver.ApiMessageResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformStarterMinimalConsumerTest {

    @Test
    void shouldStartMinimalConsumerWithoutOptionalCapabilities() {
        SpringApplication application = new SpringApplication(MinimalConsumerApplication.class);
        application.setDefaultProperties(java.util.Map.of(
                "spring.main.web-application-type", "none",
                "platform.authorization.mode", "MOCK"
        ));

        try (ConfigurableApplicationContext context = application.run(
                "--platform.messaging.enabled=false"
        )) {
            assertThat(context.isActive()).isTrue();
            assertThat(context.getBeansOfType(AuthorizationMetadataRegistry.class)).hasSize(1);
            assertThat(context.getBeansOfType(ApiMessageRepository.class)).isEmpty();
            assertThat(context.getBeansOfType(ApiMessageResolver.class)).isEmpty();

            assertThat(isPresent("br.com.portalmanager.platform.library.catalog.autoconfigure.PlatformCatalogAutoConfiguration")).isFalse();
            assertThat(isPresent("br.com.portalmanager.platform.library.audit.autoconfigure.PlatformAuditAutoConfiguration")).isFalse();
            assertThat(isPresent("br.com.portalmanager.platform.library.tagging.autoconfigure.PlatformTaggingAutoConfiguration")).isFalse();
            assertThat(isPresent("br.com.portalmanager.platform.library.crud.autoconfigure.PlatformCrudAutoConfiguration")).isFalse();
        }
    }

    private boolean isPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @SpringBootApplication
    static class MinimalConsumerApplication {
    }
}
