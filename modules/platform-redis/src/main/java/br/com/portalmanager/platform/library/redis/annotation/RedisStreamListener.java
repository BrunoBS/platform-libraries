package br.com.portalmanager.platform.library.redis.annotation;

import br.com.portalmanager.platform.library.redis.stream.GroupStart;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RedisStreamListener {
    String stream();

    String group();

    String consumer() default "";

    int concurrency() default 1;

    int batchSize() default 10;

    long blockMillis() default 250;

    GroupStart start() default GroupStart.BEGINNING;

    long minIdleMillis() default 60000;

    long recoveryIntervalMillis() default 10000;

    long maxLength() default 0;
}
