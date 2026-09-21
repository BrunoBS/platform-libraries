package br.com.portalmanager.platform.starter;

import br.com.portalmanager.platform.authorization.registry.AuthorizationMetadataRegistry;
import br.com.portalmanager.platform.messaging.repository.ApiMessageRepository;
import br.com.portalmanager.platform.messaging.repository.NoOpApiMessageRepository;
import br.com.portalmanager.platform.messaging.resolver.ApiMessageResolver;
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
                "platform.authorization.enabled", "false"
        ));

        try (ConfigurableApplicationContext context = application.run()) {
            assertThat(context.isActive()).isTrue();
            assertThat(context.getBeansOfType(AuthorizationMetadataRegistry.class)).hasSize(1);
            assertThat(context.getBeansOfType(ApiMessageRepository.class)).hasSize(1);
            assertThat(context.getBean(ApiMessageRepository.class))
                    .isInstanceOf(NoOpApiMessageRepository.class);
            assertThat(context.getBeansOfType(ApiMessageResolver.class)).hasSize(1);

            assertThat(isPresent("br.com.portalmanager.platform.catalog.autoconfigure.PlatformCatalogAutoConfiguration")).isFalse();
            assertThat(isPresent("br.com.portalmanager.platform.audit.autoconfigure.PlatformAuditAutoConfiguration")).isFalse();
            assertThat(isPresent("br.com.portalmanager.platform.tagging.autoconfigure.PlatformTaggingAutoConfiguration")).isFalse();
            assertThat(isPresent("br.com.portalmanager.platform.crud.autoconfigure.PlatformCrudAutoConfiguration")).isFalse();
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
