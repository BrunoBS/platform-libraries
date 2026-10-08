package br.com.portalmanager.platform.library.testing.integration;

import br.com.portalmanager.platform.library.testing.kafka.annotation.WithKafka;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = KafkaTestConfigurationIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "platform.authorization.enabled=false",
                "platform.messaging.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
        }
)
@WithKafka(
        image = "confluentinc/cp-kafka:7.8.0",
        topics = @WithKafka.Topic(name = "platform-testing-events", partitions = 2)
)
class KafkaTestConfigurationIntegrationTest {

    @Autowired
    private ConfluentKafkaContainer kafka;

    @Autowired
    private KafkaConnectionDetails connectionDetails;

    @Test
    void shouldStartKafkaAndExposeConnectionDetails() {
        assertThat(kafka.isRunning()).isTrue();
        assertThat(connectionDetails.getBootstrapServers())
                .containsExactly(kafka.getBootstrapServers());
        try (Admin admin = Admin.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers()))) {
            assertThat(admin.listTopics().names().get())
                    .contains("platform-testing-events");
        } catch (Exception exception) {
            throw new AssertionError("Configured Kafka topic should be available", exception);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
