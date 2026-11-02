package br.com.portalmanager.platform.library.messaging.web;

import br.com.portalmanager.platform.library.messaging.message.PlatformDefaultMessageProvider;
import br.com.portalmanager.platform.library.messaging.resolver.ApiMessageResolver;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HttpMethodNotAllowedHandlerTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final PlatformDefaultMessageProvider provider = new PlatformDefaultMessageProvider();
    private final ApiMessageResolver resolver = (key, locale) -> provider.find(key,
            locale == null ? Locale.forLanguageTag("pt-BR") : locale).orElseThrow();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ReadOnlyController())
            .setControllerAdvice(new ApiExceptionHandler(resolver)).build();

    @Test
    void unsupportedMethodsReturn405AndAllowInsteadOfTheUnexpectedErrorHandler() throws Exception {
        MvcResult result = mvc.perform(delete("/resources/identifier").header("Accept-Language", "en"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andReturn();

        JsonNode response = JSON.readTree(result.getResponse().getContentAsString());
        assertThat(response.path("code").asString()).isEqualTo("GLOBAL-0010");
        assertThat(response.path("message").asString())
                .isEqualTo("HTTP method DELETE is not allowed for this resource.");
        assertThat(response.path("path").asString()).isEqualTo("/resources/identifier");
    }

    @Test
    void the405MessageIsLocalizedInPortuguese() throws Exception {
        MvcResult result = mvc.perform(delete("/resources/identifier").header("Accept-Language", "pt-BR"))
                .andExpect(status().isMethodNotAllowed())
                .andReturn();

        JsonNode response = JSON.readTree(result.getResponse().getContentAsString());
        assertThat(response.path("message").asString())
                .isEqualTo("O método HTTP DELETE não é permitido para este recurso.");
    }

    @RestController
    static class ReadOnlyController {
        @GetMapping("/resources/{identifier}")
        public String find() { return "resource"; }
    }
}
