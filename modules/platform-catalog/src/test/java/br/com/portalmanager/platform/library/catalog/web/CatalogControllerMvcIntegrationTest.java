package br.com.portalmanager.platform.library.catalog.web;

import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.web.AuthorizationContextResolver;
import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


class CatalogControllerMvcIntegrationTest {
    @RestController
    @RequestMapping("/catalog")
    static class Controller extends CatalogController<CatalogEntity> {
        Controller(AbstractCatalogFacade<CatalogEntity> facade) { super(facade); }
    }

    @Test
    @SuppressWarnings("unchecked")
    void resolvesConfiguredHeaderAliasFromActualMvcRequest() throws Exception {
        AbstractCatalogFacade<CatalogEntity> facade = mock(AbstractCatalogFacade.class);
        when(facade.findAll(any(), anyMap())).thenReturn(List.of());
        var properties = new PlatformAuthorizationProperties();
        properties.getHeaders().setWorkspaceIdentifier(List.of("workspace-identifier", "X-Workspace-Id"));
        var mvc = MockMvcBuilders.standaloneSetup(new Controller(facade))
                .setCustomArgumentResolvers(new AuthorizationContextResolver(properties))
                .build();

        mvc.perform(get("/catalog").header("correlation-id", "trace")
                        .header("authorization", "Bearer token")
                        .header("X-Workspace-Id", "workspace-42"))
                .andDo(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(200));

        verify(facade).findAll(argThat((AuthorizationContext context) ->
                "workspace-42".equals(context.workspaceIdentifier())
                        && "trace".equals(context.correlationId())
                        && "/catalog".equals(context.uri())), eq(Map.of()));
    }
}
