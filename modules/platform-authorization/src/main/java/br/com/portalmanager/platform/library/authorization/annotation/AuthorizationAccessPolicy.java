package br.com.portalmanager.platform.library.authorization.annotation;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorizationAccessPolicy {

    AuthorizationLevel read() default AuthorizationLevel.OPEN;

    AuthorizationLevel write() default AuthorizationLevel.OPEN;
}
