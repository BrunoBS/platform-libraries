package br.com.portalmanager.platform.library.testing.kafka;

import org.testcontainers.utility.DockerImageName;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.container.PinnedDockerImage;
import br.com.portalmanager.platform.library.testing.kafka.annotation.WithKafka;
import br.com.portalmanager.platform.library.testing.kafka.KafkaTestingTechnicalErrors;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class KafkaTestContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        WithKafka annotation = AnnotatedElementUtils.findMergedAnnotation(testClass, WithKafka.class);
        if (annotation == null) {
            return null;
        }

        String image = validateImage(annotation.image());
        String[] topics = validateTopics(annotation.topics());
        return new KafkaTestContextCustomizer(image, List.of(topics));
    }

    private String validateImage(String image) {
        if (image == null || image.isBlank()) {
            throw configurationException("Kafka image must not be blank");
        }
        try {
            DockerImageName.parse(PinnedDockerImage.validatePinnedImage(image));
            return image.trim();
        } catch (IllegalArgumentException exception) {
            throw configurationException("Kafka image must be a versioned Docker image name");
        }
    }

    private String[] validateTopics(WithKafka.Topic[] topics) {
        Set<String> names = new HashSet<>();
        return Arrays.stream(topics)
                .map(topic -> {
                    String name = topic.name();
                    if (name == null || !name.matches("[a-zA-Z0-9._-]{1,249}")) {
                        throw configurationException(
                                "Kafka topic name must contain 1 to 249 letters, digits, '.', '_' or '-'");
                    }
                    if (!names.add(name)) {
                        throw configurationException("Duplicate Kafka topic name: " + name);
                    }
                    if (topic.partitions() < 1) {
                        throw configurationException("Kafka topic partitions must be a positive integer");
                    }
                    return name + "|" + topic.partitions();
                })
                .toArray(String[]::new);
    }

    private PlatformConfigurationException configurationException(String detail) {
        return new PlatformConfigurationException(KafkaTestingTechnicalErrors.invalidKafkaConfiguration(detail));
    }

    private record KafkaTestContextCustomizer(String image, List<String> topics) implements ContextCustomizer {

        @Override
        public void customizeContext(
                ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(KafkaTestConfiguration.IMAGE_PROPERTY, image);
            properties.put(KafkaTestConfiguration.TOPICS_PROPERTY, topics.toArray(String[]::new));
            context.getEnvironment().getPropertySources()
                    .addFirst(new MapPropertySource("platformTestingKafka", properties));
        }
    }
}
