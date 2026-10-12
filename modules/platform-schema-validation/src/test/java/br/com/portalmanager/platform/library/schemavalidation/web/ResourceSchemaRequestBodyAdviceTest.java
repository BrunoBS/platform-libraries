package br.com.portalmanager.platform.library.schemavalidation.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import br.com.portalmanager.platform.library.schemavalidation.normalization.SchemaPayloadNormalizer;
import br.com.portalmanager.platform.library.schemavalidation.validation.ResourceSchemaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

class ResourceSchemaRequestBodyAdviceTest {

    private static final String DEFINITION = """
    {"type":"object","additionalProperties":false,"required":["name","version"],
     "properties":{"name":{"type":"string","minLength":3},"version":{"type":"integer"},
      "settings":{"type":"object","additionalProperties":false,
        "properties":{"enabled":{"type":"boolean"}}}}}
    """;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        tools.jackson.databind.json.JsonMapper objectMapper = tools.jackson.databind.json.JsonMapper.builder().build();
        ResourceSchemaValidator validator = new ResourceSchemaValidator(
            (type, code) -> new ResourceSchema(type, code, 1, DEFINITION),
            objectMapper
        );
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("nameNormalizer", new NameNormalizer());
        mvc = MockMvcBuilders.standaloneSetup(new SampleController())
            .setControllerAdvice(
                new ResourceSchemaRequestBodyAdvice(objectMapper, validator, beans),
                new ErrorHandler()
            )
            .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
            .build();
    }

    @Test
    void normalizesAndValidatesBeforeBindingTypedRequest() throws Exception {
        mvc.perform(
            post("/normalized")
                .contentType("application/json")
                .content("{\"name\":\"  Normalized name  \",\"version\":0}")
        )
            .andExpect(status().isOk())
            .andDo(result ->
                org.assertj.core.api.Assertions.assertThat(
                    new ObjectMapper().readTree(result.getResponse().getContentAsString()).get("name").asString()
                ).isEqualTo("Normalized name")
            );
    }

    @Test
    void rejectsOriginalTypesAndUnknownFieldsBeforeTheyCanBeCoercedOrDiscarded() throws Exception {
        for (String payload : java.util.List.of(
            "{\"name\":123,\"version\":0}",
            "{\"name\":\"Application\",\"version\":\"0\"}",
            "{\"name\":\"Application\",\"version\":0,\"extra\":true}",
            "{\"name\":\"Application\",\"version\":0,\"settings\":{\"enabled\":true,\"extra\":true}}",
            "{\"name\":\"Application\",\"version\":0,\"settings\":{\"enabled\":\"true\"}}",
            "{\"name\":\"Application\"}",
            "null"
        )) {
            mvc.perform(post("/normalized").contentType("application/json").content(payload)).andExpect(
                status().isBadRequest()
            );
        }
    }

    @Test
    void supportsValidationWithoutNormalizerAndDoesNotAffectUnannotatedRequests() throws Exception {
        mvc.perform(
            post("/original").contentType("application/json").content("{\"name\":\"  Original name  \",\"version\":0}")
        )
            .andExpect(status().isOk())
            .andDo(result ->
                org.assertj.core.api.Assertions.assertThat(
                    new ObjectMapper().readTree(result.getResponse().getContentAsString()).get("name").asString()
                ).isEqualTo("  Original name  ")
            );
        mvc.perform(post("/plain").contentType("application/json").content("{\"name\":\"Plain\"}")).andExpect(
            status().isOk()
        );
    }

    @Test
    void reportsMissingNormalizerAsPlatformConfigurationError() throws Exception {
        mvc.perform(
            post("/missing-normalizer")
                .contentType("application/json")
                .content("{\"name\":\"Application\",\"version\":0}")
        ).andExpect(status().isInternalServerError());
    }

    @Test
    void rejectsMalformedJsonAndEmptyBody() throws Exception {
        mvc.perform(post("/normalized").contentType("application/json").content("{")).andExpect(
            status().isBadRequest()
        );
        mvc.perform(post("/normalized").contentType("application/json")).andExpect(status().isBadRequest());
    }

    public static class NameNormalizer implements SchemaPayloadNormalizer {

        @Override
        public JsonNode normalizePayload(JsonNode payload) {
            if (!(payload instanceof ObjectNode original)) return payload;
            ObjectNode normalized = original.deepCopy();
            JsonNode name = normalized.get("name");
            if (name != null && name.isString()) normalized.put("name", name.asString().trim());
            return normalized;
        }
    }

    public static class MissingNormalizer implements SchemaPayloadNormalizer {

        @Override
        public JsonNode normalizePayload(JsonNode payload) {
            return payload;
        }
    }

    @ValidateResourceSchemaRequest(type = "APPLICATION", code = "application", normalizer = NameNormalizer.class)
    public record NormalizedRequest(String name, Long version, Settings settings) {}

    @ValidateResourceSchemaRequest(type = "APPLICATION", code = "application")
    public record OriginalRequest(String name, Long version) {}

    @ValidateResourceSchemaRequest(type = "APPLICATION", code = "application", normalizer = MissingNormalizer.class)
    public record MissingNormalizerRequest(String name, Long version) {}

    public record PlainRequest(String name) {}

    public record Settings(boolean enabled) {}

    @RestController
    static class SampleController {

        @PostMapping("/normalized")
        public NormalizedRequest normalized(@RequestBody NormalizedRequest request) {
            return request;
        }

        @PostMapping("/original")
        public OriginalRequest original(@RequestBody OriginalRequest request) {
            return request;
        }

        @PostMapping("/plain")
        public PlainRequest plain(@RequestBody PlainRequest request) {
            return request;
        }

        @PostMapping("/missing-normalizer")
        public MissingNormalizerRequest missing(@RequestBody MissingNormalizerRequest request) {
            return request;
        }
    }

    @ControllerAdvice
    static class ErrorHandler {

        @ExceptionHandler(ValidationException.class)
        ResponseEntity<Void> invalid(ValidationException exception) {
            return ResponseEntity.badRequest().build();
        }

        @ExceptionHandler(PlatformConfigurationException.class)
        ResponseEntity<Void> configuration(PlatformConfigurationException exception) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
