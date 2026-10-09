package br.com.portalmanager.platform.library.authorization.web;

import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


class AuthorizationWebMvcAutoConfigurationIntegrationTest {
    @Test
    void registersResolverWithoutManualMvcConfiguration() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AuthorizationWebMvcAutoConfiguration.class))
                .withUserConfiguration(MvcConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    MockMvc mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
                    mvc.perform(get("/context").header("workspaceidentifier", "workspace-42")
                                    .header("correlation-id", "trace"))
                            .andDo(result -> {
                                assertThat(result.getResponse().getStatus()).isEqualTo(200);
                                assertThat(result.getResponse().getContentAsString()).isEqualTo("workspace-42:trace:/context");
                            });
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class MvcConfig {
        @Bean PlatformAuthorizationProperties properties() { return new PlatformAuthorizationProperties(); }
        @Bean ContextController contextController() { return new ContextController(); }
    }

    @RestController
    static class ContextController {
        @GetMapping("/context")
        ResponseEntity<String> context(AuthorizationContext context) {
            return ResponseEntity.ok(context.workspaceIdentifier() + ":" + context.correlationId() + ":" + context.uri());
        }
    }
}
