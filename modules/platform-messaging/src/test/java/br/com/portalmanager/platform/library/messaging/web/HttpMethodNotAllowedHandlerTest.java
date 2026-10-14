package br.com.portalmanager.platform.library.messaging.web;

import br.com.portalmanager.platform.library.messaging.message.PlatformDefaultMessageProvider;
import br.com.portalmanager.platform.library.messaging.resolver.ApiMessageResolver;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

class HttpMethodNotAllowedHandlerTest {
    private final PlatformDefaultMessageProvider provider = new PlatformDefaultMessageProvider();
    private final ApiMessageResolver resolver = (key, locale) -> provider.find(key,
            locale == null ? Locale.forLanguageTag("pt-BR") : locale).orElseThrow();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ReadOnlyController())
            .setControllerAdvice(new ApiExceptionHandler(resolver)).build();

    @Test
    void unsupportedMethodsReturn405AndAllowInsteadOfTheUnexpectedErrorHandler() throws Exception {
        mvc.perform(delete("/resources/identifier").header("Accept-Language", "en"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.code").value("GLOBAL-0010"))
                .andExpect(jsonPath("$.message").value("HTTP method DELETE is not allowed for this resource."))
                .andExpect(jsonPath("$.path").value("/resources/identifier"));
    }

    @Test
    void the405MessageIsLocalizedInPortuguese() throws Exception {
        mvc.perform(delete("/resources/identifier").header("Accept-Language", "pt-BR"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("O método HTTP DELETE não é permitido para este recurso."));
    }

    @RestController
    static class ReadOnlyController {
        @GetMapping("/resources/{identifier}")
        public String find() { return "resource"; }
    }
}
