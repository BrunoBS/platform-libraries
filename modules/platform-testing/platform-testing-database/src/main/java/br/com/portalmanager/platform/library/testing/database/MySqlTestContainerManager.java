package br.com.portalmanager.platform.library.testing.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;
import java.util.function.Function;

/** Owns the single MySQL test container for the lifetime of the test JVM. */
final class MySqlTestContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySqlTestContainerManager.class);
    private static final MySqlTestContainerManager SHARED = new MySqlTestContainerManager(
            MySqlTestContainerManager::createContainer,
            true);

    private final Function<String, MySQLContainer<?>> containerFactory;
    private final boolean registerShutdownHook;
    private MySQLContainer<?> container;
    private String image;
    private boolean shutdownHookRegistered;

    MySqlTestContainerManager() {
        this(MySqlTestContainerManager::createContainer, true);
    }

    MySqlTestContainerManager(
            Function<String, MySQLContainer<?>> containerFactory,
            boolean registerShutdownHook) {
        this.containerFactory = Objects.requireNonNull(containerFactory);
        this.registerShutdownHook = registerShutdownHook;
    }

    static MySQLContainer<?> sharedContainer(String requestedImage) {
        return SHARED.getOrStart(requestedImage);
    }

    synchronized MySQLContainer<?> getOrStart(String requestedImage) {
        String normalizedImage = Objects.requireNonNull(requestedImage).trim();
        if (container != null) {
            if (!image.equals(normalizedImage)) {
                throw new IllegalStateException(
                        "One MySQL container per test JVM is supported; requested image "
                                + normalizedImage + " after starting " + image + ".");
            }
            if (!container.isRunning()) {
                throw new IllegalStateException(
                        "The shared MySQL test container stopped before the test JVM ended.");
            }
            return container;
        }

        MySQLContainer<?> started = Objects.requireNonNull(containerFactory.apply(normalizedImage));
        try {
            started.start();
        } catch (RuntimeException failure) {
            try {
                started.stop();
            } catch (RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }

        container = started;
        image = normalizedImage;
        registerShutdownHook();
        return container;
    }

    private void registerShutdownHook() {
        if (!registerShutdownHook || shutdownHookRegistered) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "platform-testing-mysql-shutdown"));
        shutdownHookRegistered = true;
    }

    private synchronized void stop() {
        if (container != null && container.isRunning()) {
            LOGGER.info("Stopping shared MySQL test container at JVM shutdown: {}", container.getContainerId());
            container.stop();
            LOGGER.info("Stopped shared MySQL test container: {}", container.getContainerId());
        }
    }

    private static MySQLContainer<?> createContainer(String image) {
        return new MySQLContainer<>(DockerImageName.parse(image))
                .withDatabaseName("integration_test")
                .withUsername("test")
                .withPassword("test");
    }
}
