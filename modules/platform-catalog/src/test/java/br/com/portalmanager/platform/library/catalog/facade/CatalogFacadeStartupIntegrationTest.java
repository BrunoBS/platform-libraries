package br.com.portalmanager.platform.library.catalog.facade;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.service.AbstractCatalogService;
import br.com.portalmanager.platform.library.catalog.service.IncludedCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CatalogFacadeStartupIntegrationTest {
    @Test
    void rejectsUnprotectedOverrideDuringSpringStartup() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            try (var context = new AnnotationConfigApplicationContext()) {
                context.register(CatalogFacadePolicyAutoConfiguration.class, UnprotectedConfig.class);
                context.refresh();
            }
        }).isInstanceOf(IllegalStateException.class)
          .hasStackTraceContaining("without @AuthorizationRequired");
    }

    @Test
    void allowsInheritedProtectedOperationsDuringSpringStartup() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(CatalogFacadePolicyAutoConfiguration.class, ProtectedConfig.class);
            context.refresh();
            assertThat(context.isActive()).isTrue();
        }
    }

    static class UnprotectedFacade extends AbstractCatalogFacade<CatalogEntity> {
        UnprotectedFacade(AbstractCatalogService<CatalogEntity> service) { super(service); }
        @Override
        public List<CatalogDTO> findAll(AuthorizationContext context, Map<String, String> filters) {
            return super.findAll(context, filters);
        }
    }

    static class ProtectedFacade extends AbstractCatalogFacade<CatalogEntity> {
        ProtectedFacade(AbstractCatalogService<CatalogEntity> service) { super(service); }
    }

    @Configuration(proxyBeanMethods = false)
    static class UnprotectedConfig {
        @Bean
        @SuppressWarnings("unchecked")
        AbstractCatalogService<CatalogEntity> service() { return mock(IncludedCatalogService.class); }
        @Bean UnprotectedFacade facade(AbstractCatalogService<CatalogEntity> service) {
            return new UnprotectedFacade(service);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProtectedConfig {
        @Bean
        @SuppressWarnings("unchecked")
        AbstractCatalogService<CatalogEntity> service() { return mock(IncludedCatalogService.class); }
        @Bean ProtectedFacade facade(AbstractCatalogService<CatalogEntity> service) {
            return new ProtectedFacade(service);
        }
    }
}
