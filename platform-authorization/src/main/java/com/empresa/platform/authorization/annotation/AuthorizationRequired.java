package com.empresa.platform.authorization.annotation;

import com.empresa.platform.authorization.model.AuthorizationLevel;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorizationRequired {

    AuthorizationLevel level() default AuthorizationLevel.OPEN;
}
