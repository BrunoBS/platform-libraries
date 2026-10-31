package br.com.portalmanager.platform.library.authorization.annotation;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationMechanism;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorizationRequired {
    AuthorizationLevel level();
    AuthorizationAction action();
    AuthorizationMechanism mechanism() default AuthorizationMechanism.TOKEN;
    String policy() default "";
}
