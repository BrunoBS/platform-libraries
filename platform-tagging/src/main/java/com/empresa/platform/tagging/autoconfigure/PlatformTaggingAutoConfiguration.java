package com.empresa.platform.tagging.autoconfigure;

import com.empresa.platform.tagging.TagManager;
import com.empresa.platform.tagging.model.Tag;
import com.empresa.platform.tagging.repository.TagRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

@AutoConfiguration
public class PlatformTaggingAutoConfiguration {

    @Bean
    PersistenceUnitPostProcessor tagPersistenceUnitPostProcessor() {
        return persistenceUnitInfo -> persistenceUnitInfo.addManagedClassName(Tag.class.getName());
    }

    @Bean
    EntityManagerFactoryBuilderCustomizer taggingEntityManagerFactoryBuilderCustomizer(
            PersistenceUnitPostProcessor tagPersistenceUnitPostProcessor) {
        return builder -> builder.addPersistenceUnitPostProcessors(tagPersistenceUnitPostProcessor);
    }

    @Bean
    @ConditionalOnMissingBean(TagRepository.class)
    TagRepository tagRepository(EntityManager entityManager) {
        return new JpaRepositoryFactory(entityManager).getRepository(TagRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean
    TagManager tagManager(TagRepository repository) {
        return new TagManager(repository);
    }
}
