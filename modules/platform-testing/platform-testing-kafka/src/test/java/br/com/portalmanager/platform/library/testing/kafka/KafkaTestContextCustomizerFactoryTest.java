package br.com.portalmanager.platform.library.testing.kafka;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.kafka.annotation.WithKafka;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaTestContextCustomizerFactoryTest {

    private final KafkaTestContextCustomizerFactory factory = new KafkaTestContextCustomizerFactory();

    @Test
    void shouldPassImageAndTopicsFromAnnotationToSpringContext() {
        var customizer = factory.createContextCustomizer(ConfiguredKafkaTest.class, List.of());
        var context = new AnnotationConfigApplicationContext();

        customizer.customizeContext(context, null);

        assertThat(context.getEnvironment().getProperty(KafkaTestConfiguration.IMAGE_PROPERTY))
                .isEqualTo("registry.example/kafka:3.8.1");
        assertThat(context.getEnvironment().getProperty(
                KafkaTestConfiguration.TOPICS_PROPERTY,
                String[].class))
                .containsExactly("product-events|3");
        context.close();
    }

    @Test
    void shouldIncludeImageAndTopicConfigurationInContextCacheKey() {
        var configured = factory.createContextCustomizer(ConfiguredKafkaTest.class, List.of());
        var differentImage = factory.createContextCustomizer(DifferentKafkaTest.class, List.of());
        var differentTopics = factory.createContextCustomizer(DifferentTopicKafkaTest.class, List.of());

        assertThat(configured).isNotEqualTo(differentImage);
        assertThat(configured).isNotEqualTo(differentTopics);
    }

    @Test
    void shouldRejectFloatingImageTagWithPlatformError() {
        assertThatThrownBy(() -> factory.createContextCustomizer(FloatingKafkaImageTest.class, List.of()))
                .isInstanceOf(PlatformConfigurationException.class)
                .satisfies(exception -> assertThat(
                        ((PlatformConfigurationException) exception).getErrorResponse().code())
                        .isEqualTo("PLT-TST-003"));
    }

    @Test
    void shouldRejectInvalidTopicConfigurationWithPlatformError() {
        assertThatThrownBy(() -> factory.createContextCustomizer(InvalidKafkaTest.class, List.of()))
                .isInstanceOf(PlatformConfigurationException.class)
                .satisfies(exception -> assertThat(
                        ((PlatformConfigurationException) exception).getErrorResponse().code())
                        .isEqualTo("PLT-TST-003"));
    }

    @WithKafka(
            image = "registry.example/kafka:3.8.1",
            topics = @WithKafka.Topic(name = "product-events", partitions = 3)
    )
    private static final class ConfiguredKafkaTest {
    }

    @WithKafka(
            image = "registry.example/kafka:3.8.2",
            topics = @WithKafka.Topic(name = "product-events", partitions = 3)
    )
    private static final class DifferentKafkaTest {
    }

    @WithKafka(
            image = "registry.example/kafka:3.8.1",
            topics = @WithKafka.Topic(name = "other-events", partitions = 3)
    )
    private static final class DifferentTopicKafkaTest {
    }

    @WithKafka(image = "confluentinc/cp-kafka:latest")
    private static final class FloatingKafkaImageTest {
    }

    @WithKafka(topics = @WithKafka.Topic(name = "invalid", partitions = 0))
    private static final class InvalidKafkaTest {
    }
}
