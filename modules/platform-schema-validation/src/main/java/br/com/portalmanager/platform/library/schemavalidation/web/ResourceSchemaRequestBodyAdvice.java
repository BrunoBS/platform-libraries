package br.com.portalmanager.platform.library.schemavalidation.web;

import br.com.portalmanager.platform.library.schemavalidation.validation.ResourceSchemaValidator;
import br.com.portalmanager.platform.library.schemavalidation.web.annotation.ValidateResourceSchema;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Type;

@ControllerAdvice
public class ResourceSchemaRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final ResourceSchemaValidator validator;
    private final ObjectMapper objectMapper;

    public ResourceSchemaRequestBodyAdvice(
            ResourceSchemaValidator validator,
            ObjectMapper objectMapper
    ) {
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return methodParameter.hasMethodAnnotation(ValidateResourceSchema.class);
    }

    @Override
    public HttpInputMessage beforeBodyRead(
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) throws IOException {
        byte[] body = inputMessage.getBody().readAllBytes();

        if (body.length > 0) {
            ValidateResourceSchema binding = parameter.getMethodAnnotation(ValidateResourceSchema.class);
            JsonNode payload = objectMapper.readTree(body);
            validator.validate(binding.type(), binding.code(), payload);
        }

        return new CachedBodyHttpInputMessage(inputMessage.getHeaders(), body);
    }
}
