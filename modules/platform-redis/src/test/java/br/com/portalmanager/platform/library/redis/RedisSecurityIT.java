package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.message.PlatformDefaultMessageProvider;
import br.com.portalmanager.platform.library.redis.core.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.data.redis.core.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

class RedisSecurityIT extends RedisIntegrationSupport {
    private static byte[] bytes(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void tlsAndAuthenticationUseBootConnectionAndTrustConfiguration() {
        try (var secure =
                new GenericContainer<>(DockerImageName.parse(REDIS.getDockerImageName()))
                        .withExposedPorts(6379)
                        .withCopyFileToContainer(
                                MountableFile.forClasspathResource("tls/server.crt"),
                                "/tls/server.crt")
                        .withCopyFileToContainer(
                                MountableFile.forClasspathResource("tls/server.key", 0644),
                                "/tls/server.key")
                        .withCommand(
                                "redis-server",
                                "--port",
                                "0",
                                "--tls-port",
                                "6379",
                                "--tls-cert-file",
                                "/tls/server.crt",
                                "--tls-key-file",
                                "/tls/server.key",
                                "--tls-ca-cert-file",
                                "/tls/server.crt",
                                "--tls-auth-clients",
                                "no",
                                "--requirepass",
                                "test-only-password")
                        .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1))) {
            secure.start();
            var runner =
                    context()
                            .withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class))
                            .withPropertyValues(
                                    "spring.data.redis.host=localhost",
                                    "spring.data.redis.port=" + secure.getMappedPort(6379),
                                    "spring.data.redis.ssl.enabled=true",
                                    "spring.data.redis.ssl.bundle=redis-test",
                                    "spring.ssl.bundle.pem.redis-test.truststore.certificate=classpath:tls/server.crt",
                                    "spring.data.redis.password=test-only-password");
            runner.run(
                    ctx -> {
                        var store = ctx.getBean(RedisStore.class);
                        String key = key();
                        store.set(key, new Event("tls", 1));
                        assertThat(store.get(key, Event.class)).contains(new Event("tls", 1));
                        assertThat(
                                        ctx.getBean(AsyncRedisStore.class)
                                                .get(key, Event.class)
                                                .toCompletableFuture()
                                                .get(4, TimeUnit.SECONDS))
                                .contains(new Event("tls", 1));
                    });
            runner.withPropertyValues("spring.data.redis.password=wrong-test-password")
                    .run(
                            ctx -> {
                                Throwable failure =
                                        catchThrowable(
                                                () ->
                                                        ctx.getBean(RedisStore.class)
                                                                .get("absent", String.class));
                                assertThat(failure)
                                        .isInstanceOf(ApiException.class)
                                        .hasMessage("redis.authentication");
                                assertThat(stack(failure))
                                        .doesNotContain(
                                                "test-only-password", "wrong-test-password");
                            });
        }
    }

    @Test
    void timeoutsAreErrorsAndNotAbsenceForSyncAndAsync() {
        context()
                .withPropertyValues("spring.data.redis.timeout=100ms")
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var redis = ctx.getBean(StringRedisTemplate.class);
                            var async = ctx.getBean(AsyncRedisStore.class);
                            String key = key();
                            store.set(key, "warm");
                            async.get(key, String.class)
                                    .toCompletableFuture()
                                    .get(3, TimeUnit.SECONDS);
                            redis.execute(
                                    (RedisCallback<Object>)
                                            connection ->
                                                    connection.execute(
                                                            "CLIENT",
                                                            bytes("PAUSE"),
                                                            bytes("450"),
                                                            bytes("ALL")));
                            assertThatThrownBy(() -> store.get(key, String.class))
                                    .isInstanceOf(ApiException.class)
                                    .hasMessage("redis.timeout");
                            assertThatThrownBy(
                                            () ->
                                                    async.get(key, String.class)
                                                            .toCompletableFuture()
                                                            .join())
                                    .hasCauseInstanceOf(ApiException.class);
                            await().ignoreExceptionsInstanceOf(ApiException.class)
                                    .atMost(Duration.ofSeconds(3))
                                    .untilAsserted(
                                            () ->
                                                    assertThat(store.get(key, String.class))
                                                            .contains("warm"));
                        });
    }

    @Test
    void bothLocalesResolveAllModuleErrorCategories() {
        var provider = new PlatformDefaultMessageProvider("redis-test");
        for (String key :
                List.of(
                        "validation",
                        "unavailable",
                        "timeout",
                        "authentication",
                        "type",
                        "serialization",
                        "builder.used",
                        "configuration",
                        "listener",
                        "limit",
                        "saturation",
                        "cache.types.invalid",
                        "cache.sync.transaction")) {
            var en = provider.find("redis." + key, Locale.ENGLISH);
            var pt = provider.find("redis." + key, Locale.forLanguageTag("pt-BR"));
            assertThat(en).isPresent();
            assertThat(pt).isPresent();
            assertThat(en.get()).isNotEqualTo(pt.get());
        }
    }

    @Test
    void sizeLimitsCoverExternalValuesAndAsyncFailures() {
        context()
                .withPropertyValues(
                        "platform.redis.max-payload-bytes=32", "platform.redis.max-key-bytes=64")
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var redis = ctx.getBean(StringRedisTemplate.class);
                            String key = key();
                            assertThatThrownBy(() -> store.set(key, "x".repeat(40)))
                                    .isInstanceOf(ApiException.class)
                                    .hasMessage("redis.limit");
                            assertThatThrownBy(() -> store.set("x".repeat(64), 1))
                                    .isInstanceOf(ApiException.class)
                                    .hasMessage("redis.validation");
                            redis.opsForValue().set("redis-test:" + key, "x".repeat(40));
                            assertThatThrownBy(() -> store.get(key, String.class))
                                    .isInstanceOf(ApiException.class)
                                    .hasMessage("redis.limit");
                            assertThatThrownBy(
                                            () ->
                                                    ctx.getBean(AsyncRedisStore.class)
                                                            .get(key, String.class)
                                                            .toCompletableFuture()
                                                            .join())
                                    .hasCauseInstanceOf(ApiException.class);
                        });
    }

    @Test
    void namespaceIsolationAndExplicitSharingAcrossServices() {
        String key = key();
        context()
                .withPropertyValues("platform.redis.namespace=team")
                .run(
                        a -> {
                            a.getBean(RedisStore.class).set(key, "isolated");
                            context()
                                    .withPropertyValues(
                                            "spring.application.name=another",
                                            "platform.redis.namespace=team")
                                    .run(
                                            b ->
                                                    assertThat(
                                                                    b.getBean(RedisStore.class)
                                                                            .get(key, String.class))
                                                            .isEmpty());
                        });
        context()
                .withPropertyValues(
                        "platform.redis.namespace=team", "platform.redis.namespace-mode=SHARED")
                .run(
                        a -> {
                            a.getBean(RedisStore.class).set(key, "shared");
                            context()
                                    .withPropertyValues(
                                            "spring.application.name=another",
                                            "platform.redis.namespace=team",
                                            "platform.redis.namespace-mode=SHARED")
                                    .run(
                                            b ->
                                                    assertThat(
                                                                    b.getBean(RedisStore.class)
                                                                            .get(key, String.class))
                                                            .contains("shared"));
                        });
    }

    private static String stack(Throwable failure) {
        var result = new java.io.StringWriter();
        failure.printStackTrace(new java.io.PrintWriter(result));
        return result.toString();
    }
}
