package com.empresa.platform.authorization.autoconfigure;

import com.empresa.platform.authorization.resource.ResourceVisibilityFilterManager;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {
        PlatformAuthorizationAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ConditionalOnClass({EntityManager.class, Session.class})
public class ResourceVisibilityJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResourceVisibilityFilterManager resourceVisibilityFilterManager(EntityManager entityManager) {
        return new ResourceVisibilityFilterManager(entityManager);
    }
}
