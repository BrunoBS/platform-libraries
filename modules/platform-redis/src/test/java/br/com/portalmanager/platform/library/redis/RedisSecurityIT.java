package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.message.PlatformDefaultMessageProvider;
import br.com.portalmanager.platform.library.redis.store.*;
import br.com.portalmanager.platform.library.redis.operations.*;

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
    void namespaceIsolationAndExplicitSharingAcrossServices() {
        String key = key();
        context().run(a -> {
            a.getBean(RedisStore.class).set(key, "application-isolated");
            a.getBean(RedisStore.class).withNamespace("team").set(key, "explicitly-shared");
            context().withPropertyValues("spring.application.name=another")
                    .run(b -> {
                        RedisStore store = b.getBean(RedisStore.class);
                        assertThat(store.get(key, String.class)).isEmpty();
                        assertThat(store.withNamespace("team").get(key, String.class)).contains("explicitly-shared");
                    });
        });
    }

    private static String stack(Throwable failure) {
        var result = new java.io.StringWriter();
        failure.printStackTrace(new java.io.PrintWriter(result));
        return result.toString();
    }
}
