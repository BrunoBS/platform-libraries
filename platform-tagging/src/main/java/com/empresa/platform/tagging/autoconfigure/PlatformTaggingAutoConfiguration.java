package com.empresa.platform.tagging.autoconfigure;

import com.empresa.platform.tagging.TagManager;
import com.empresa.platform.tagging.model.Tag;
import com.empresa.platform.tagging.repository.TagRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

@AutoConfiguration
@EntityScan(basePackageClasses = Tag.class)
public class PlatformTaggingAutoConfiguration {

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
