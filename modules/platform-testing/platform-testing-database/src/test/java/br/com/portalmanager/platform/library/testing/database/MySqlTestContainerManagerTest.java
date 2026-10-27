package br.com.portalmanager.platform.library.testing.database;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlTestContainerManagerTest {

    @Test
    void startsOnlyOnceForConcurrentContextsInTheSameJvm() throws Exception {
        MySQLContainer<?> container = mock(MySQLContainer.class);
        when(container.isRunning()).thenReturn(true);
        AtomicInteger creations = new AtomicInteger();
        MySqlTestContainerManager manager = new MySqlTestContainerManager(
                image -> {
                    creations.incrementAndGet();
                    return container;
                },
                false);
        CountDownLatch ready = new CountDownLatch(12);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MySQLContainer<?>>> results = new ArrayList<>();

        try (var executor = Executors.newFixedThreadPool(12)) {
            for (int index = 0; index < 12; index++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return manager.getOrStart(MySqlContainerImages.MYSQL);
                }));
            }
            ready.await();
            start.countDown();

            for (Future<MySQLContainer<?>> result : results) {
                assertThat(result.get()).isSameAs(container);
            }
        }

        assertThat(creations).hasValue(1);
        verify(container).start();
    }

    @Test
    void rejectsASecondImageInsteadOfStartingAnotherContainer() {
        MySQLContainer<?> container = mock(MySQLContainer.class);
        when(container.isRunning()).thenReturn(true);
        AtomicInteger creations = new AtomicInteger();
        MySqlTestContainerManager manager = new MySqlTestContainerManager(
                image -> {
                    creations.incrementAndGet();
                    return container;
                },
                false);

        manager.getOrStart("mysql:8.4.11");

        assertThatThrownBy(() -> manager.getOrStart("mysql:9.7.2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("One MySQL container per test JVM is supported");
        assertThat(creations).hasValue(1);
    }

    @Test
    void failsIfTheSharedContainerWasStoppedBeforeJvmShutdown() {
        MySQLContainer<?> container = mock(MySQLContainer.class);
        AtomicBoolean running = new AtomicBoolean(true);
        when(container.isRunning()).thenAnswer(invocation -> running.get());
        AtomicInteger creations = new AtomicInteger();
        MySqlTestContainerManager manager = new MySqlTestContainerManager(
                image -> {
                    creations.incrementAndGet();
                    return container;
                },
                false);

        manager.getOrStart(MySqlContainerImages.MYSQL);
        running.set(false);

        assertThatThrownBy(() -> manager.getOrStart(MySqlContainerImages.MYSQL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stopped before the test JVM ended");
        assertThat(creations).hasValue(1);
    }
}
