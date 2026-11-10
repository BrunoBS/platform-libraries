package br.com.portalmanager.platform.library.testing.authorization.api.annotation;

import br.com.portalmanager.platform.library.testing.authorization.internal.extension.AuthorizationMockExtension;
import br.com.portalmanager.platform.library.testing.authorization.api.AuthorizationMockResult;
import br.com.portalmanager.platform.library.testing.authorization.internal.configuration.AuthorizationMockTestConfiguration;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(AuthorizationMockTestConfiguration.class)
@ExtendWith(AuthorizationMockExtension.class)
@ResourceLock("platform-http-mock")
public @interface WithMockAuthorization {

    AuthorizationMockResult defaultResult() default AuthorizationMockResult.ALLOWED;
}
