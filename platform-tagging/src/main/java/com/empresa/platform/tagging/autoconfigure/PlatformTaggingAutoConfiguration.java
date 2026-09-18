package com.empresa.platform.tagging.autoconfigure;

import com.empresa.platform.tagging.TagManager;
import com.empresa.platform.tagging.model.Tag;
import com.empresa.platform.tagging.repository.TagRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@EntityScan(basePackageClasses = Tag.class)
@EnableJpaRepositories(basePackageClasses = TagRepository.class)
public class PlatformTaggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TagManager tagManager(TagRepository repository) {
        return new TagManager(repository);
    }
}
