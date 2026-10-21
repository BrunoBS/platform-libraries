package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.portalmanager.platform.library.redis.annotation.RedisNamespace;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.redis.configuration.RedisNamespaceAutoConfiguration;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.NamespaceMode;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.core.AsyncRedisStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

class RedisNamespaceTest {
    ApplicationContextRunner context() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                RedisNamespaceAutoConfiguration.class, PlatformRedisAutoConfiguration.class,
                DataRedisAutoConfiguration.class, DataRedisReactiveAutoConfiguration.class))
                .withPropertyValues("spring.application.name=namespace-test", "platform.redis.namespace=default");
    }

    record ConstructorConsumer(RedisStore store) {
        ConstructorConsumer(@RedisNamespace(value="dynamic-parameter", mode=NamespaceMode.SHARED) RedisStore store) {
            this.store = store;
        }
    }
    static class FieldConsumer {
        @Autowired @RedisNamespace(value="dynamic-parameter", mode=NamespaceMode.SHARED) RedisStore store;
    }
    record BeanConsumer(RedisStore store) {}
    @Configuration(proxyBeanMethods=false)
    static class Consumers {

        @Bean FieldConsumer fieldConsumer() { return new FieldConsumer(); }
        @Bean BeanConsumer beanConsumer(@RedisNamespace("session") RedisStore store) { return new BeanConsumer(store); }
    }
    @Test void resolvesBeanArgumentsFieldsAndActualConstructorInjection() {
        context().withUserConfiguration(Consumers.class).withBean(ConstructorConsumer.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    RedisStore shared = ctx.getBean(ConstructorConsumer.class).store();
                    assertThat(ctx.getBean(FieldConsumer.class).store).isSameAs(shared);
                    assertThat(ctx.getBean(BeanConsumer.class).store()).isNotSameAs(shared);
                    assertThat(ctx.getBean(RedisStore.class)).isNotSameAs(shared);
                    assertThat(ctx.getBeansOfType(RedisStore.class)).hasSize(1);
                });
    }
    @Configuration(proxyBeanMethods=false)
    static class CustomStores {
        @Bean @Primary RedisStore primaryStore() { return mock(RedisStore.class); }
        @Bean RedisStore otherStore() { return mock(RedisStore.class); }
        @Bean BeanConsumer qualified(@Qualifier("otherStore") RedisStore store) { return new BeanConsumer(store); }
    }
    @Test void preservesPrimaryQualifiersAndConsumerOverrides() {
        context().withUserConfiguration(CustomStores.class, Consumers.class).withBean(ConstructorConsumer.class).run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(RedisStore.class)).isSameAs(ctx.getBean("primaryStore"));
            assertThat(((BeanConsumer)ctx.getBean("qualified")).store()).isSameAs(ctx.getBean("otherStore"));
            assertThat(ctx.getBean(ConstructorConsumer.class).store()).isNotSameAs(ctx.getBean(RedisStore.class));
        });
    }
    @Configuration(proxyBeanMethods=false)
    static class InvalidNamespace {
        @Bean BeanConsumer invalid(@RedisNamespace("bad:namespace") RedisStore store) { return new BeanConsumer(store); }
    }
    @Configuration(proxyBeanMethods=false)
    static class InvalidType {
        @Bean Object invalid(@RedisNamespace("session") AsyncRedisStore store) { return store; }
    }
    @Test void rejectsInvalidNamespaceTypeAndDisabledInfrastructure() {
        context().withUserConfiguration(InvalidNamespace.class).run(ctx -> assertThat(ctx).hasFailed());
        context().withUserConfiguration(InvalidType.class).run(ctx -> assertThat(ctx).hasFailed());
        context().withUserConfiguration(Consumers.class).withBean(ConstructorConsumer.class).withPropertyValues("platform.redis.enabled=false")
                .run(ctx -> assertThat(ctx).hasFailed());
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RedisNamespaceAutoConfiguration.class))
                .withUserConfiguration(Consumers.class).withBean(ConstructorConsumer.class).run(ctx -> assertThat(ctx).hasFailed());
    }
    @Test void requiresApplicationForIsolatedAndValidatesPrefixBudget() {
        context().withUserConfiguration(Consumers.class).withBean(ConstructorConsumer.class).withPropertyValues("spring.application.name=")
                .run(ctx -> assertThat(ctx).hasFailed());
        context().withUserConfiguration(Consumers.class).withBean(ConstructorConsumer.class).withPropertyValues("platform.redis.max-key-bytes=8")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
    @Test void contextCachesAreIndependent() {
        RedisStore[] first = new RedisStore[1];
        context().withUserConfiguration(Consumers.class).withBean(ConstructorConsumer.class).run(ctx -> first[0] = ctx.getBean(ConstructorConsumer.class).store());
        context().withUserConfiguration(Consumers.class).withBean(ConstructorConsumer.class).run(ctx -> assertThat(ctx.getBean(ConstructorConsumer.class).store()).isNotSameAs(first[0]));
    }
}
