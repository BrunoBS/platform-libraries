package br.com.portalmanager.platform.library.authorization.visibility;

import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationAutoConfiguration;

import br.com.portalmanager.platform.library.authorization.visibility.ResourceVisibilityAspect;
import br.com.portalmanager.platform.library.authorization.visibility.ResourceVisibilityFilterManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

@AutoConfiguration(
        after = PlatformAuthorizationAutoConfiguration.class,
        afterName = "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
)
@ConditionalOnClass(name = {
        "jakarta.persistence.EntityManager",
        "jakarta.persistence.EntityManagerFactory",
        "org.hibernate.Session",
        "org.springframework.orm.jpa.SharedEntityManagerCreator"
})
public class ResourceVisibilityJpaAutoConfiguration {

    @Bean
    @ConditionalOnBean(EntityManagerFactory.class)
    @ConditionalOnMissingBean
    public ResourceVisibilityFilterManager resourceVisibilityFilterManager(
            EntityManagerFactory entityManagerFactory
    ) {
        return new ResourceVisibilityFilterManager(
                SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory)
        );
    }

    @Bean
    @ConditionalOnBean(ResourceVisibilityFilterManager.class)
    @ConditionalOnMissingBean
    public ResourceVisibilityAspect resourceVisibilityAspect(
            ResourceVisibilityFilterManager filterManager
    ) {
        return new ResourceVisibilityAspect(filterManager);
    }
}
