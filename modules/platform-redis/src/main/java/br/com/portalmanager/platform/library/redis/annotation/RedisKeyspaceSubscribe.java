package br.com.portalmanager.platform.library.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Native Redis notifications. Requires platform.redis.keyspace-enabled and server configuration.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RedisKeyspaceSubscribe {
    String[] patterns();
}
