package br.com.portalmanager.platform.library.authorization.autoconfigure;

import br.com.portalmanager.platform.library.authorization.visibility.ResourceVisibilityAspect;
import br.com.portalmanager.platform.library.authorization.visibility.ResourceVisibilityFilterManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ResourceVisibilityJpaAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ResourceVisibilityJpaAutoConfiguration.class));

    @Test
    void shouldCreateVisibilityBeansFromEntityManagerFactoryWithoutEntityManagerBean() {
        contextRunner
                .withUserConfiguration(JpaInfrastructureConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(EntityManagerFactory.class);
                    assertThat(context).doesNotHaveBean(EntityManager.class);
                    assertThat(context).hasSingleBean(ResourceVisibilityFilterManager.class);
                    assertThat(context).hasSingleBean(ResourceVisibilityAspect.class);
                });
    }

    @Test
    void shouldNotCreateVisibilityBeansWithoutJpaInfrastructure() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ResourceVisibilityFilterManager.class);
            assertThat(context).doesNotHaveBean(ResourceVisibilityAspect.class);
        });
    }

    @Configuration
    static class JpaInfrastructureConfiguration {

        @Bean
        EntityManagerFactory entityManagerFactory() {
            return mock(EntityManagerFactory.class);
        }
    }
}
