package br.com.portalmanager.platform.library.schemavalidation.aspect;

import br.com.portalmanager.platform.library.schemavalidation.annotation.SchemaPayload;
import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchema;
import br.com.portalmanager.platform.library.schemavalidation.validation.ResourceSchemaValidator;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ResourceSchemaValidationAspectTest {

    private final ResourceSchemaValidator validator = mock(ResourceSchemaValidator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResourceSchemaValidationAspect aspect =
            new ResourceSchemaValidationAspect(validator, objectMapper);

    @Test
    void shouldValidateMarkedUseCasePayloadBeforeProceeding() throws Throwable {
        Method method = SampleUseCase.class.getMethod("create", String.class, SampleInput.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, new Object[]{"workspace", new SampleInput("app")});
        ValidateResourceSchema binding = method.getAnnotation(ValidateResourceSchema.class);

        aspect.validate(joinPoint, binding);

        verify(validator).validate(
                eq("APPLICATION"),
                eq("application"),
                eq(objectMapper.valueToTree(new SampleInput("app")))
        );
        verify(joinPoint).proceed();
    }

    @Test
    void shouldResolveAndValidatePayloadAcrossRepeatedCalls() throws Throwable {
        Method method = SampleUseCase.class.getMethod("create", String.class, SampleInput.class);
        ValidateResourceSchema binding = method.getAnnotation(ValidateResourceSchema.class);

        aspect.validate(
                joinPoint(method, new Object[]{"workspace-1", new SampleInput("app-1")}),
                binding
        );
        aspect.validate(
                joinPoint(method, new Object[]{"workspace-2", new SampleInput("app-2")}),
                binding
        );

        verify(validator).validate(
                eq("APPLICATION"),
                eq("application"),
                eq(objectMapper.valueToTree(new SampleInput("app-1")))
        );
        verify(validator).validate(
                eq("APPLICATION"),
                eq("application"),
                eq(objectMapper.valueToTree(new SampleInput("app-2")))
        );
    }

    @Test
    void shouldRejectAnnotatedMethodWithoutExactlyOneSchemaPayload() throws Throwable {
        Method method = InvalidUseCase.class.getMethod("create", SampleInput.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, new Object[]{new SampleInput("app")});
        ValidateResourceSchema binding = method.getAnnotation(ValidateResourceSchema.class);

        assertThrows(PlatformConfigurationException.class, () -> aspect.validate(joinPoint, binding));
        verify(joinPoint, never()).proceed();
    }

    private ProceedingJoinPoint joinPoint(Method method, Object[] args) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    static class SampleUseCase {
        @ValidateResourceSchema(type = "APPLICATION", code = "application")
        public void create(String workspace, @SchemaPayload SampleInput input) {
        }
    }

    static class InvalidUseCase {
        @ValidateResourceSchema(type = "APPLICATION", code = "application")
        public void create(SampleInput input) {
        }
    }

    record SampleInput(String name) {
    }
}
