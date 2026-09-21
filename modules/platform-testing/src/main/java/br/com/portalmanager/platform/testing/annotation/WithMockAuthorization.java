package br.com.portalmanager.platform.testing.annotation;

import br.com.portalmanager.platform.testing.authorization.AuthorizationMockExtension;
import br.com.portalmanager.platform.testing.authorization.AuthorizationMockResult;
import br.com.portalmanager.platform.testing.authorization.AuthorizationMockTestConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
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
public @interface WithMockAuthorization {

    AuthorizationMockResult defaultResult() default AuthorizationMockResult.ALLOWED;
}
