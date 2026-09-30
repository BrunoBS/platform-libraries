package br.com.portalmanager.platform.library.authorization.autoconfigure;

import br.com.portalmanager.platform.library.authorization.aspect.ResourceVisibilityAspect;
import br.com.portalmanager.platform.library.authorization.resource.ResourceVisibilityFilterManager;
import br.com.portalmanager.platform.library.authorization.resource.ResourceVisibilityIdResolver;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = PlatformAuthorizationAutoConfiguration.class)
@ConditionalOnClass(name = {
        "jakarta.persistence.EntityManager",
        "org.hibernate.Session"
})
public class ResourceVisibilityJpaAutoConfiguration {

    @Bean
    @ConditionalOnBean(EntityManager.class)
    @ConditionalOnMissingBean
    public ResourceVisibilityFilterManager resourceVisibilityFilterManager(EntityManager entityManager) {
        return new ResourceVisibilityFilterManager(entityManager);
    }

    @Bean
    @ConditionalOnBean(EntityManager.class)
    @ConditionalOnMissingBean
    public ResourceVisibilityIdResolver resourceVisibilityIdResolver(EntityManager entityManager) {
        return new ResourceVisibilityIdResolver(entityManager);
    }

    @Bean
    @ConditionalOnBean({
            ResourceVisibilityFilterManager.class,
            ResourceVisibilityIdResolver.class
    })
    @ConditionalOnMissingBean
    public ResourceVisibilityAspect resourceVisibilityAspect(
            ResourceVisibilityFilterManager filterManager,
            ResourceVisibilityIdResolver idResolver
    ) {
        return new ResourceVisibilityAspect(filterManager, idResolver);
    }
}
