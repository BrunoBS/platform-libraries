package com.empresa.platform.authorization.annotation;

import com.empresa.platform.authorization.model.AuthorizationLevel;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorizationAccessPolicy {

    AuthorizationLevel read() default AuthorizationLevel.OPEN;

    AuthorizationLevel write() default AuthorizationLevel.OPEN;
}
