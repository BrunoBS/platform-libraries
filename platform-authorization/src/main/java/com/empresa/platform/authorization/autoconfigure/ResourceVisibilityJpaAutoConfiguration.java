package com.empresa.platform.authorization.autoconfigure;

import com.empresa.platform.authorization.resource.ResourceVisibilityFilterManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = PlatformAuthorizationAutoConfiguration.class)
@ConditionalOnClass({EntityManager.class, Session.class})
public class ResourceVisibilityJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(EntityManagerFactory.class)
    public ResourceVisibilityFilterManager resourceVisibilityFilterManager(EntityManager entityManager) {
        return new ResourceVisibilityFilterManager(entityManager);
    }
}
