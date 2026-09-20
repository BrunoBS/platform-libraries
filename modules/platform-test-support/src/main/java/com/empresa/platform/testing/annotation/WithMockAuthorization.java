package com.empresa.platform.testing.annotation;

import com.empresa.platform.testing.authorization.AuthorizationMockExtension;
import com.empresa.platform.testing.authorization.AuthorizationMockResult;
import com.empresa.platform.testing.authorization.AuthorizationMockTestConfiguration;
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
