package br.com.portalmanager.platform.library.catalog.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.service.AbstractCatalogService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AbstractCatalogFacadeContractTest {

    @Test
    void allPublicOperationsRequireOwnerAndExplicitAction() {
        var actions = Map.of(
                "findAll", AuthorizationAction.READ,
                "findByCode", AuthorizationAction.READ,
                "create", AuthorizationAction.CREATE,
                "update", AuthorizationAction.UPDATE,
                "delete", AuthorizationAction.DELETE,
                "restore", AuthorizationAction.RESTORE);
        var publicMethods = Arrays.stream(AbstractCatalogFacade.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();
        assertThat(publicMethods).hasSize(actions.size());
        publicMethods.forEach(method -> {
            var required = method.getDeclaredAnnotation(AuthorizationRequired.class);
            assertThat(required).as(method.getName()).isNotNull();
            assertThat(required.level()).isEqualTo(AuthorizationLevel.OWNER);
            assertThat(required.action()).isEqualTo(actions.get(method.getName()));
            assertThat(method.getParameterTypes()[0]).isEqualTo(AuthorizationContext.class);
        });
    }

    @Test
    void facadeDelegatesToExistingService() {
        @SuppressWarnings("unchecked")
        AbstractCatalogService<CatalogEntity> service = mock(AbstractCatalogService.class);
        var facade = new SampleFacade(service);
        var context = new AuthorizationContext("correlation", "Bearer token", null, null, null);
        facade.findAll(context, Map.of("active", "true"));
        facade.findByCode(context, "A");
        facade.delete(context, "A");
        facade.restore(context, "A");
        verify(service).findAll(Map.of("active", "true"));
        verify(service).findByCode("A");
        verify(service).delete("A");
        verify(service).restore("A");
    }

    @Test
    void overriddenMethodsCanDeclareTheirOwnPolicy() throws Exception {
        var method = CustomizedFacade.class.getDeclaredMethod(
                "findAll", AuthorizationContext.class, Map.class);
        var required = method.getDeclaredAnnotation(AuthorizationRequired.class);
        assertThat(required).isNotNull();
        assertThat(required.level()).isEqualTo(AuthorizationLevel.DEV);
    }

    static class SampleFacade extends AbstractCatalogFacade<CatalogEntity> {
        SampleFacade(AbstractCatalogService<CatalogEntity> service) {
            super(service);
        }
    }

    static class CustomizedFacade extends SampleFacade {
        CustomizedFacade(AbstractCatalogService<CatalogEntity> service) {
            super(service);
        }

        @Override
        @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
        public List<CatalogDTO> findAll(AuthorizationContext context, Map<String, String> filters) {
            return super.findAll(context, filters);
        }
    }
}
