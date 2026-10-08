package br.com.portalmanager.platform.library.testing.kafka;

import org.testcontainers.utility.DockerImageName;
import br.com.portalmanager.platform.library.testing.container.PinnedDockerImage;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@TestConfiguration(proxyBeanMethods = false)
public class KafkaTestConfiguration {

    static final String IMAGE_PROPERTY = "platform.testing.kafka.image";
    static final String TOPICS_PROPERTY = "platform.testing.kafka.topics";

    @Bean(destroyMethod = "stop")
    @ServiceConnection
    KafkaTestContainer kafkaContainer(Environment environment) {
        String image = environment.getProperty(
                IMAGE_PROPERTY,
                KafkaContainerImages.KAFKA);
        String[] topics = environment.getProperty(
                TOPICS_PROPERTY,
                String[].class,
                new String[0]);
        return new KafkaTestContainer(DockerImageName.parse(PinnedDockerImage.validatePinnedImage(image)), topics);
    }
}
