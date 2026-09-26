package br.com.portalmanager.platform.library.authorization.annotation;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorizationRequired {

    AuthorizationLevel level() default AuthorizationLevel.OPEN;
}
