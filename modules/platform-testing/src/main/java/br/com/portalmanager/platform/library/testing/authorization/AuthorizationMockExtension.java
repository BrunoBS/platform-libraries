package br.com.portalmanager.platform.library.testing.authorization;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public final class AuthorizationMockExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        WithMockAuthorization configuration = AnnotationSupport
                .findAnnotation(context.getRequiredTestClass(), WithMockAuthorization.class)
                .orElseThrow(() -> new IllegalStateException("@WithMockAuthorization configuration was not found"));

        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        AuthorizationMock authorizationMock = applicationContext.getBean(AuthorizationMock.class);
        authorizationMock.reset();

        switch (configuration.defaultResult()) {
            case ALLOWED -> authorizationMock.allow(session -> applicationContext
                    .getBeanProvider(AuthorizationSessionCustomizer.class)
                    .orderedStream()
                    .forEach(customizer -> customizer.customize(session)));
            case DENIED -> authorizationMock.deny();
            case FORBIDDEN -> authorizationMock.forbidden();
            case INTERNAL_ERROR -> authorizationMock.internalError();
        }
    }
}
