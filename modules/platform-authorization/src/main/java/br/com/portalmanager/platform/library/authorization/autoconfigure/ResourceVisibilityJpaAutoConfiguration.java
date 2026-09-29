package br.com.portalmanager.platform.library.authorization.autoconfigure;

import br.com.portalmanager.platform.library.authorization.resource.ResourceVisibilityFilterManager;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = PlatformAuthorizationAutoConfiguration.class)
@ConditionalOnClass(name = "org.hibernate.Session")
@ConditionalOnBean(EntityManager.class)
public class ResourceVisibilityJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResourceVisibilityFilterManager resourceVisibilityFilterManager(EntityManager entityManager) {
        return new ResourceVisibilityFilterManager(entityManager);
    }
}
