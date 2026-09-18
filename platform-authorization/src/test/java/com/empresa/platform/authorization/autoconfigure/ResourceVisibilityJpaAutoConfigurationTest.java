package com.empresa.platform.authorization.autoconfigure;

import com.empresa.platform.authorization.resource.NativeResourceVisibilityContext;
import com.empresa.platform.authorization.resource.NativeResourceVisibilityStrategy;
import com.empresa.platform.authorization.resource.ResourceVisibilityFilterManager;
import com.empresa.platform.authorization.resource.ResourceVisibilityMetadata;
import com.empresa.platform.authorization.resource.ResourceVisibilityMetadataRegistry;
import com.empresa.platform.authorization.resource.ResourceVisibilityNativeQueryRewriter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ResourceVisibilityJpaAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ResourceVisibilityJpaAutoConfiguration.class))
            .withUserConfiguration(JpaInfrastructureConfiguration.class);

    @Test
    void shouldCreateVisibilityInfrastructureWhenJpaIsAvailable() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(NativeResourceVisibilityContext.class);
            assertThat(context).hasSingleBean(ResourceVisibilityFilterManager.class);
            assertThat(context).hasSingleBean(ResourceVisibilityMetadataRegistry.class);
            assertThat(context).hasSingleBean(NativeResourceVisibilityStrategy.class);
            assertThat(context).hasSingleBean(ResourceVisibilityNativeQueryRewriter.class);
        });
    }

    @Test
    void shouldCollectApplicationVisibilityMetadata() {
        contextRunner.withUserConfiguration(MetadataConfiguration.class).run(context -> {
            ResourceVisibilityMetadataRegistry registry =
                    context.getBean(ResourceVisibilityMetadataRegistry.class);

            assertThat(registry.findByTableName("accounts"))
                    .contains(new ResourceVisibilityMetadata("accounts", "authorizer_group"));
            assertThat(registry.findByTableName("applications"))
                    .contains(new ResourceVisibilityMetadata("applications", "authorizer_group"));
        });
    }

    @Test
    void shouldKeepApplicationProvidedStrategy() {
        contextRunner.withUserConfiguration(CustomStrategyConfiguration.class).run(context -> {
            NativeResourceVisibilityStrategy strategy =
                    context.getBean(NativeResourceVisibilityStrategy.class);

            assertThat(strategy).isSameAs(context.getBean("customNativeResourceVisibilityStrategy"));
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class JpaInfrastructureConfiguration {
        @Bean
        EntityManager entityManager() {
            return mock(EntityManager.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MetadataConfiguration {
        @Bean
        ResourceVisibilityMetadata accountVisibilityMetadata() {
            return new ResourceVisibilityMetadata("accounts", "authorizer_group");
        }

        @Bean
        ResourceVisibilityMetadata applicationVisibilityMetadata() {
            return new ResourceVisibilityMetadata("applications", "authorizer_group");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomStrategyConfiguration {
        @Bean
        NativeResourceVisibilityStrategy customNativeResourceVisibilityStrategy() {
            return sql -> sql;
        }
    }
}
