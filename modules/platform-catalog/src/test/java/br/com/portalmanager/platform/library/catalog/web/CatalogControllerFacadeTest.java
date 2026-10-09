package br.com.portalmanager.platform.library.catalog.web;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CatalogControllerFacadeTest {

    @Test
    void controllerDelegatesToFacadeWithRequestAuthorizationHeaders() {
        @SuppressWarnings("unchecked")
        AbstractCatalogFacade<CatalogEntity> facade = mock(AbstractCatalogFacade.class);
        CatalogController<CatalogEntity> controller = new CatalogController<>(facade) {};
        var headers = Map.of(
                "Authorization", "Bearer token",
                "Correlation-Id", "request-id",
                "workspace-identifier", "workspace-id",
                "environment-identifier", "environment-id",
                "application-identifier", "application-id");

        controller.findAll(headers, Map.of("active", "true"));
        controller.findByCode(headers, "CODE");
        controller.delete(headers, "CODE");
        controller.restore(headers, "CODE");

        var context = new AuthorizationContext("request-id", "Bearer token",
                "workspace-id", "environment-id", "application-id");
        verify(facade).findAll(eq(context), eq(Map.of("active", "true")));
        verify(facade).findByCode(eq(context), eq("CODE"));
        verify(facade).delete(eq(context), eq("CODE"));
        verify(facade).restore(eq(context), eq("CODE"));
    }
}
