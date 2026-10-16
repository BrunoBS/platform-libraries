package br.com.portalmanager.platform.library.testing.redis.annotation;

import br.com.portalmanager.platform.library.testing.redis.RedisContainerImages;
import br.com.portalmanager.platform.library.testing.redis.RedisTestConfiguration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(RedisTestConfiguration.class)
@org.junit.jupiter.api.extension.ExtendWith(
        br.com.portalmanager.platform.library.testing.redis.RedisTestExtension.class)
public @interface WithRedis {
    String image() default RedisContainerImages.REDIS;

    boolean cleanupBeforeEach() default true;
}
