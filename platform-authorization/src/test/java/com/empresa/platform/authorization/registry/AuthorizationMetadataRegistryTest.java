package com.empresa.platform.authorization.registry;

import com.empresa.platform.authorization.annotation.AuthorizationRequired;
import com.empresa.platform.authorization.model.AuthorizationLevel;
import com.empresa.platform.authorization.model.AuthorizationPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RestController;
import java.lang.reflect.Method;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationMetadataRegistryTest {

    @RestController
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    static class SampleController {
        @AuthorizationRequired(level = AuthorizationLevel.ADM)
        public void securedMethod() {}
        public void inheritedMethod() {}
    }

    @Test
    void shouldRegistryAndResolveMethodsCorrectly() throws NoSuchMethodException {
        ApplicationContext context = mock(ApplicationContext.class);
        SampleController controller = new SampleController();
        
        when(context.getBeansWithAnnotation(RestController.class))
                .thenReturn(Map.of("sampleController", controller));

        AuthorizationMetadataRegistry registry = new AuthorizationMetadataRegistry(context);
        registry.afterSingletonsInstantiated();

        Method secured = SampleController.class.getMethod("securedMethod");
        Method inherited = SampleController.class.getMethod("inheritedMethod");

        AuthorizationPolicy p1 = registry.resolve(SampleController.class, secured);
        assertEquals(AuthorizationLevel.ADM, p1.level());
        assertEquals(AuthorizationPolicy.Source.METHOD, p1.source());

        AuthorizationPolicy p2 = registry.resolve(SampleController.class, inherited);
        assertEquals(AuthorizationLevel.DEV, p2.level());
        assertEquals(AuthorizationPolicy.Source.CLASS, p2.source());
    }
}
