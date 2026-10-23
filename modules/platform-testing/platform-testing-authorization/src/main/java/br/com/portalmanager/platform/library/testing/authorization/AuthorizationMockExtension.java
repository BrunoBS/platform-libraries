package br.com.portalmanager.platform.library.testing.authorization;

import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public final class AuthorizationMockExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(AuthorizationMockExtension.class);
    private static final String PREVIOUS_USER_CONTEXT = "previous-user-context";

    @Override
    public void beforeEach(ExtensionContext context) {
        WithMockAuthorization configuration = AnnotationSupport
                .findAnnotation(context.getRequiredTestClass(), WithMockAuthorization.class)
                .orElseThrow(() -> new IllegalStateException("@WithMockAuthorization configuration was not found"));

        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        AuthorizationMock authorizationMock = applicationContext.getBean(AuthorizationMock.class);
        authorizationMock.reset();
        UserSession userSession = configuredSession(applicationContext);

        context.getStore(NAMESPACE).put(
                PREVIOUS_USER_CONTEXT,
                new PreviousUserContext(UserContext.get().orElse(null)));

        switch (configuration.defaultResult()) {
            case ALLOWED -> authorizationMock.allow(userSession);
            case DENIED -> authorizationMock.deny();
            case FORBIDDEN -> authorizationMock.forbidden();
            case INTERNAL_ERROR -> authorizationMock.internalError();
        }

        UserContext.set(userSession);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        PreviousUserContext previous = context.getStore(NAMESPACE)
                .remove(PREVIOUS_USER_CONTEXT, PreviousUserContext.class);
        if (previous != null) {
            UserContext.set(previous.session());
        }
    }

    private UserSession configuredSession(ApplicationContext applicationContext) {
        AuthorizationSessionBuilder builder = AuthorizationSessionBuilder.builder();
        applicationContext.getBeanProvider(AuthorizationSessionCustomizer.class)
                .orderedStream()
                .forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    private record PreviousUserContext(UserSession session) {}
}
