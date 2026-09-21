package br.com.portalmanager.platform.authorization.annotation;

import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorizationAccessPolicy {

    AuthorizationLevel read() default AuthorizationLevel.OPEN;

    AuthorizationLevel write() default AuthorizationLevel.OPEN;
}
