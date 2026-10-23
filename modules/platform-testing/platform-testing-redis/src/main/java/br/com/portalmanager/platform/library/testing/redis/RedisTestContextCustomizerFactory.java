package br.com.portalmanager.platform.library.testing.redis;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.container.PinnedDockerImage;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RedisTestContextCustomizerFactory implements ContextCustomizerFactory {
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass, List<ContextConfigurationAttributes> attributes) {
        var annotation = AnnotatedElementUtils.findMergedAnnotation(testClass, WithRedis.class);
        if (annotation == null) return null;
        String image;
        try {
            image = PinnedDockerImage.validatePinnedImage(annotation.image());
            DockerImageName.parse(image);
        } catch (IllegalArgumentException failure) {
            throw new PlatformConfigurationException(
                    RedisTestingTechnicalErrors.invalidConfiguration());
        }
        return new RedisContextCustomizer(image, annotation.cleanupBeforeEach());
    }

    private record RedisContextCustomizer(String image, boolean cleanupBeforeEach)
            implements ContextCustomizer {
        public void customizeContext(
                ConfigurableApplicationContext context, MergedContextConfiguration merged) {
            String namespace = "test-" + UUID.randomUUID().toString().replace("-", "");
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(
                            new MapPropertySource(
                                    "platformTestingRedis",
                                    Map.of(
                                            "platform.testing.redis.image",
                                            image,
                                            "platform.redis.namespace",
                                            namespace,
                                            "platform.redis.namespace-mode",
                                            "ISOLATED")));
        }
    }
}
