package br.com.portalmanager.platform.library.redis.cache;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.core.ResolvableType;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/** Only code-declared types are trusted; no class name is read from Redis. */
public final class CacheReturnTypes implements BeanPostProcessor {
    private final Map<String, JavaType> regions = new ConcurrentHashMap<>();
    private final AnnotationCacheOperationSource operations = new AnnotationCacheOperationSource();

    public Object postProcessAfterInitialization(Object bean, String name) {
        Class<?> target = AopUtils.getTargetClass(bean);
        if (!operations.isCandidateClass(target)) return bean;
        for (var method : target.getMethods()) {
            var declared = operations.getCacheOperations(method, target);
            if (declared == null) continue;
            for (var operation : declared) {
                if (!(operation instanceof CacheableOperation)
                        && !(operation instanceof CachePutOperation)) continue;
                if (!operation.getCacheManager().isEmpty()
                        && !operation.getCacheManager().equals("platformRedisCacheManager"))
                    continue;
                if (!operation.getCacheResolver().isEmpty()
                        && !operation.getCacheResolver().equals("platformRedisCacheResolver"))
                    continue;
                var type = ResolvableType.forMethodReturnType(method, target);
                if (type.resolve() == Optional.class) type = type.getGeneric(0);
                Class<?> raw = type.resolve();
                if (raw == null
                        || raw == Object.class
                        || raw == void.class
                        || CompletionStage.class.isAssignableFrom(raw)
                        || Future.class.isAssignableFrom(raw)
                        || org.reactivestreams.Publisher.class.isAssignableFrom(raw))
                    throw CacheErrors.types();
                JavaType valueType = resolve(type);
                for (String region : operation.getCacheNames()) {
                    PlatformRedisCacheProperties.name(region);
                    JavaType previous = regions.putIfAbsent(region, valueType);
                    if (previous != null && !previous.equals(valueType)) throw CacheErrors.types();
                }
            }
        }
        return bean;
    }

    private JavaType resolve(ResolvableType type) {
        if (type.resolve() == null || type.hasUnresolvableGenerics()) throw CacheErrors.types();
        var factory = TypeFactory.createDefaultInstance();
        if (type.isArray()) return factory.constructArrayType(resolve(type.getComponentType()));
        if (!type.hasGenerics()) return factory.constructType(type.resolve());
        return factory.constructParametricType(
                type.resolve(),
                Arrays.stream(type.getGenerics()).map(this::resolve).toArray(JavaType[]::new));
    }

    JavaType required(String region) {
        var type = regions.get(region);
        if (type == null) throw CacheErrors.types();
        return type;
    }
}
