package br.com.portalmanager.platform.library.tagging.autoconfigure;

import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.repository.TagRepository;
import br.com.portalmanager.platform.library.tagging.storage.JpaTagStorage;
import br.com.portalmanager.platform.library.tagging.storage.TagStorage;
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
    @ConditionalOnMissingBean(TagStorage.class)
    TagStorage tagStorage(TagRepository repository) {
        return new JpaTagStorage(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    TagManager tagManager(TagStorage storage) {
        return new TagManager(storage);
    }
}
