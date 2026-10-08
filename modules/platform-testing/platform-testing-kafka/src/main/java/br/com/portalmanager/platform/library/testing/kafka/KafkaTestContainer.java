package br.com.portalmanager.platform.library.testing.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class KafkaTestContainer extends ConfluentKafkaContainer {

    private final List<NewTopic> topics;

    public KafkaTestContainer(DockerImageName image, String[] topicDefinitions) {
        super(image);
        this.topics = Arrays.stream(topicDefinitions)
                .map(KafkaTestContainer::topic)
                .toList();
    }

    @Override
    public void start() {
        super.start();
        if (topics.isEmpty()) {
            return;
        }

        try (Admin admin = Admin.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                getBootstrapServers()
        ))) {
            admin.createTopics(topics).all().get(30, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            super.stop();
            throw new IllegalStateException("Kafka topic provisioning was interrupted", exception);
        } catch (Exception exception) {
            super.stop();
            throw new IllegalStateException("Failed to provision configured Kafka topics", exception);
        }
    }

    private static NewTopic topic(String definition) {
        String[] parts = definition.split("\\|", -1);
        return new NewTopic(parts[0], Integer.parseInt(parts[1]), (short) 1);
    }
}
