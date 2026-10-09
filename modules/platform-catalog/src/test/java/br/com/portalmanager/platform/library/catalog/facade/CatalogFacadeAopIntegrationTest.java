package br.com.portalmanager.platform.library.catalog.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.aop.AuthorizationFacadeAspect;
import br.com.portalmanager.platform.library.authorization.client.AuthorizationClient;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.service.AbstractCatalogService;
import br.com.portalmanager.platform.library.catalog.service.IncludedCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class CatalogFacadeAopIntegrationTest {

    private static AuthorizationContext valid() {
        return new AuthorizationContext("correlation", "Bearer token", null, null, null);
    }

    @Test
    void inheritedFacadeMethodRequiresOwnerThroughRealProxy() {
        try (var spring = new AnnotationConfigApplicationContext(Config.class)) {
            var facade = spring.getBean(StandardFacade.class);
            facade.findAll(valid(), Map.of());
            verify(spring.getBean(AuthorizationClient.class)).authorize(argThat(request ->
                    request.policy() == AuthorizationLevel.OWNER &&
                    request.action() == AuthorizationAction.READ));
            verify(spring.getBean("catalogService", AbstractCatalogService.class)).findAll(Map.of());
            assertThat(UserContext.get()).isEmpty();
        }
    }

    @Test
    void overriddenMethodUsesConcretePolicyThroughRealProxy() {
        try (var spring = new AnnotationConfigApplicationContext(Config.class)) {
            var facade = spring.getBean(CustomFacade.class);
            facade.findAll(valid(), Map.of());
            verify(spring.getBean(AuthorizationClient.class)).authorize(argThat(request ->
                    request.policy() == AuthorizationLevel.DEV &&
                    request.action() == AuthorizationAction.READ));
            verify(spring.getBean("catalogService", AbstractCatalogService.class)).findAll(Map.of());
        }
    }

    @Test
    void missingTokenFailsBeforeBusinessExecution() {
        try (var spring = new AnnotationConfigApplicationContext(Config.class)) {
            var facade = spring.getBean(StandardFacade.class);
            assertThatThrownBy(() -> facade.findAll(
                    new AuthorizationContext("correlation", null, null, null, null), Map.of()))
                    .isInstanceOf(RuntimeException.class);
            verifyNoInteractions(spring.getBean("catalogService", AbstractCatalogService.class));
        }
    }

    static class StandardFacade extends AbstractCatalogFacade<CatalogEntity> {
        StandardFacade(AbstractCatalogService<CatalogEntity> service) { super(service); }
    }

    static class CustomFacade extends AbstractCatalogFacade<CatalogEntity> {
        CustomFacade(AbstractCatalogService<CatalogEntity> service) { super(service); }

        @Override
        @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
        public List<CatalogDTO> findAll(AuthorizationContext context, Map<String, String> filters) {
            return super.findAll(context, filters);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class Config {
        @Bean AuthorizationClient authorizationClient() {
            var client = mock(AuthorizationClient.class);
            var session = new UserSession();
            session.setUserName("catalog-user");
            when(client.authorize(any())).thenReturn(session);
            return client;
        }

        @Bean AuthorizationFacadeAspect authorizationFacadeAspect(AuthorizationClient client) {
            return new AuthorizationFacadeAspect(client);
        }

        @Bean
        @SuppressWarnings("unchecked")
        AbstractCatalogService<CatalogEntity> catalogService() {
            return mock(IncludedCatalogService.class);
        }

        @Bean StandardFacade standardFacade(AbstractCatalogService<CatalogEntity> service) {
            return new StandardFacade(service);
        }

        @Bean CustomFacade customFacade(AbstractCatalogService<CatalogEntity> service) {
            return new CustomFacade(service);
        }
    }
}
