package com.empresa.platform.authorization.autoconfigure;

import com.empresa.platform.authorization.resource.JSqlParserNativeResourceVisibilityStrategy;
import com.empresa.platform.authorization.resource.NativeResourceVisibilityContext;
import com.empresa.platform.authorization.resource.NativeResourceVisibilityStrategy;
import com.empresa.platform.authorization.resource.ResourceVisibilityFilterManager;
import com.empresa.platform.authorization.resource.ResourceVisibilityMetadata;
import com.empresa.platform.authorization.resource.ResourceVisibilityMetadataRegistry;
import com.empresa.platform.authorization.resource.ResourceVisibilityNativeQueryRewriter;
import com.empresa.platform.authorization.resource.ResourceVisibilityRepositoryFactoryBeanPostProcessor;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

import java.util.List;

@AutoConfiguration(after = PlatformAuthorizationAutoConfiguration.class,
        afterName = "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration")
@ConditionalOnClass({EntityManager.class, Session.class})
public class ResourceVisibilityJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NativeResourceVisibilityContext nativeResourceVisibilityContext() {
        return new NativeResourceVisibilityContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public ResourceVisibilityFilterManager resourceVisibilityFilterManager(
            EntityManager entityManager,
            NativeResourceVisibilityContext nativeContext) {
        return new ResourceVisibilityFilterManager(entityManager, nativeContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResourceVisibilityMetadataRegistry resourceVisibilityMetadataRegistry() {
        return new ResourceVisibilityMetadataRegistry(List.of(
                new ResourceVisibilityMetadata("accounts", "authorizer_group")
        ));
    }

    @Bean
    @ConditionalOnMissingBean
    public NativeResourceVisibilityStrategy nativeResourceVisibilityStrategy(
            ResourceVisibilityMetadataRegistry metadataRegistry) {
        return new JSqlParserNativeResourceVisibilityStrategy(metadataRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(JpaRepositoryFactoryBean.class)
    public ResourceVisibilityNativeQueryRewriter resourceVisibilityNativeQueryRewriter(
            NativeResourceVisibilityContext nativeContext,
            NativeResourceVisibilityStrategy strategy) {
        return new ResourceVisibilityNativeQueryRewriter(nativeContext, strategy);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(JpaRepositoryFactoryBean.class)
    public ResourceVisibilityRepositoryFactoryBeanPostProcessor resourceVisibilityRepositoryFactoryBeanPostProcessor(
            ResourceVisibilityNativeQueryRewriter rewriter) {
        return new ResourceVisibilityRepositoryFactoryBeanPostProcessor(rewriter);
    }
}
