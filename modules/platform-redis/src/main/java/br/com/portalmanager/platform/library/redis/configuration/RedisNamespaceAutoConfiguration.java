package br.com.portalmanager.platform.library.redis.configuration;

import br.com.portalmanager.platform.library.redis.internal.RedisErrors;
import br.com.portalmanager.platform.library.redis.internal.RedisNamespaceResolver;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/** Installs injection support even when Redis is disabled, preventing silent annotation fallback. */
@AutoConfiguration(before = PlatformRedisAutoConfiguration.class)
public class RedisNamespaceAutoConfiguration {
    @Bean
    static BeanFactoryPostProcessor redisNamespaceInjection(Environment environment) {
        return beans -> {
            if (!(beans instanceof DefaultListableBeanFactory factory)) throw RedisErrors.configuration();
            factory.setAutowireCandidateResolver(new RedisNamespaceResolver(
                    factory.getAutowireCandidateResolver(), environment));
        };
    }
}
