package br.com.portalmanager.platform.library.testing.database;

import org.testcontainers.utility.DockerImageName;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.container.PinnedDockerImage;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.message.PlatformTestingTechnicalErrors;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.List;
import java.util.Map;

public final class MySqlTestContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        WithMySql annotation = AnnotatedElementUtils.findMergedAnnotation(testClass, WithMySql.class);
        if (annotation == null) {
            return null;
        }

        return new MySqlTestContextCustomizer(validateImage(annotation.image()));
    }

    private String validateImage(String image) {
        try {
            DockerImageName.parse(PinnedDockerImage.validatePinnedImage(image));
            return image.trim();
        } catch (IllegalArgumentException exception) {
            throw new PlatformConfigurationException(
                    PlatformTestingTechnicalErrors.invalidMySqlConfiguration(
                            "MySQL image must be a versioned Docker image name"),
                    exception
            );
        }
    }

    private record MySqlTestContextCustomizer(String image) implements ContextCustomizer {

        @Override
        public void customizeContext(
                ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource(
                            "platformTestingMySql",
                            Map.of(MySqlTestConfiguration.IMAGE_PROPERTY, image)
                    )
            );
        }
    }
}
