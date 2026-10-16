package br.com.portalmanager.platform.library.testing.redis;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

/** Cleans only the namespace allocated to the current Spring test context. */
public final class RedisTestExtension implements BeforeEachCallback {
    public void beforeEach(ExtensionContext context) {
        var annotation =
                AnnotatedElementUtils.findMergedAnnotation(
                        context.getRequiredTestClass(), WithRedis.class);
        if (annotation == null || !annotation.cleanupBeforeEach()) return;
        var application = SpringExtension.getApplicationContext(context);
        var redis = application.getBeanProvider(StringRedisTemplate.class).getIfUnique();
        if (redis == null) return;
        String namespace = application.getEnvironment().getProperty("platform.redis.namespace");
        String service = application.getEnvironment().getProperty("spring.application.name");
        if (namespace == null
                || !namespace.matches("test-[A-Za-z0-9-]+")
                || service == null
                || !service.matches("[A-Za-z0-9._-]+"))
            throw new PlatformConfigurationException(
                    RedisTestingTechnicalErrors.invalidConfiguration());
        String prefix = namespace + "." + service + ":";
        try (var keys =
                redis.scan(ScanOptions.scanOptions().match(prefix + "*").count(1000).build())) {
            List<String> batch = new ArrayList<>();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!key.startsWith(prefix)) continue;
                batch.add(key);
                if (batch.size() == 1000) {
                    redis.delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) redis.delete(batch);
        } catch (RuntimeException failure) {
            throw new PlatformConfigurationException(
                    RedisTestingTechnicalErrors.cleanupFailed(),
                    new Exception(failure.getClass().getSimpleName()));
        }
    }
}
