package br.com.portalmanager.platform.library.redis.annotation;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.NamespaceMode;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Selects a synchronous RedisStore prefix at an injection point; not an authorization boundary. */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisNamespace {
    String value();
    NamespaceMode mode() default NamespaceMode.ISOLATED;
}
