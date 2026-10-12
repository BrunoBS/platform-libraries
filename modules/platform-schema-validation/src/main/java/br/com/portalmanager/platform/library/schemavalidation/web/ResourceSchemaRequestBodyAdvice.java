package br.com.portalmanager.platform.library.schemavalidation.web;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationTechnicalErrors;
import br.com.portalmanager.platform.library.schemavalidation.normalization.SchemaPayloadNormalizer;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ControllerAdvice
public class ResourceSchemaRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final ObjectMapper objectMapper;
    private final SchemaValidator schemaValidator;
    private final BeanFactory beanFactory;

    public ResourceSchemaRequestBodyAdvice(
        ObjectMapper objectMapper,
        SchemaValidator schemaValidator,
        BeanFactory beanFactory
    ) {
        this.objectMapper = objectMapper;
        this.schemaValidator = schemaValidator;
        this.beanFactory = beanFactory;
    }

    @Override
    public boolean supports(
        MethodParameter parameter,
        Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return parameter.getParameterType().isAnnotationPresent(ValidateResourceSchemaRequest.class);
    }

    @Override
    public HttpInputMessage beforeBodyRead(
        HttpInputMessage inputMessage,
        MethodParameter parameter,
        Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) throws IOException {
        ValidateResourceSchemaRequest binding = parameter
            .getParameterType()
            .getAnnotation(ValidateResourceSchemaRequest.class);
        JsonNode originalPayload;
        try {
            originalPayload = objectMapper.readTree(inputMessage.getBody());
        } catch (JacksonException exception) {
            throw new HttpMessageNotReadableException("Invalid JSON request body", exception, inputMessage);
        }
        // Normalize only known text values; preserve unknown fields and original JSON types for schema validation.
        JsonNode normalizedPayload = normalizePayload(binding, originalPayload);
        schemaValidator.validate(binding.type(), binding.code(), normalizedPayload);
        byte[] validatedBody = objectMapper.writeValueAsBytes(normalizedPayload);
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(inputMessage.getHeaders());
        headers.setContentLength(validatedBody.length);
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(validatedBody);
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        };
    }

    private JsonNode normalizePayload(ValidateResourceSchemaRequest binding, JsonNode payload) {
        if (binding.normalizer() == SchemaPayloadNormalizer.class) return payload;
        SchemaPayloadNormalizer normalizer;
        try {
            normalizer = beanFactory.getBean(binding.normalizer());
        } catch (BeansException exception) {
            throw new PlatformConfigurationException(SchemaValidationTechnicalErrors.REQUEST_NORMALIZER_INVALID);
        }
        return normalizer.normalizePayload(payload);
    }
}
