package br.com.portalmanager.platform.library.testing.authorization.api.annotation;

import br.com.portalmanager.platform.library.testing.authorization.internal.extension.UserContextIsolationExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(UserContextIsolationExtension.class)
public @interface WithUserContextIsolation {}
