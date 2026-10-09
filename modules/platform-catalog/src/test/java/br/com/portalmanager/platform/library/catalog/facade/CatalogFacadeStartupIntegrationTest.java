package br.com.portalmanager.platform.library.catalog.facade;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.service.AbstractCatalogService;
import br.com.portalmanager.platform.library.catalog.service.IncludedCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CatalogFacadeStartupIntegrationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CatalogFacadePolicyAutoConfiguration.class));

    @Test
    void rejectsUnprotectedOverrideDuringSpringStartup() {
        runner.withUserConfiguration(UnprotectedConfig.class).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class);
            assertThat(context.getStartupFailure()).hasStackTraceContaining("without @AuthorizationRequired");
        });
    }

    @Test
    void allowsInheritedProtectedOperationsDuringSpringStartup() {
        runner.withUserConfiguration(ProtectedConfig.class).run(context -> assertThat(context).hasNotFailed());
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
