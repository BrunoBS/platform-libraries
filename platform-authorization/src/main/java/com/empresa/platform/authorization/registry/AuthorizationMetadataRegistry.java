package com.empresa.platform.authorization.registry;

import com.empresa.platform.authorization.annotation.AuthorizationRequired;
import com.empresa.platform.authorization.model.AuthorizationPolicy;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class AuthorizationMetadataRegistry implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;
    private final Map<String, AuthorizationPolicy> registry = new ConcurrentHashMap<>();

    public AuthorizationMetadataRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);
        controllers.values().forEach(this::register);
    }

    private void register(Object bean) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        if (targetClass.getPackageName().startsWith("org.springframework")) {
            return;
        }

        // Obtém a anotação declarada no nível do Controller (classe)
        AuthorizationRequired classAnn = AnnotatedElementUtils.findMergedAnnotation(targetClass, AuthorizationRequired.class);

        // Usa getDeclaredMethods para varrer apenas os métodos do próprio Controller, ignorando heranças do Object
        for (Method method : targetClass.getDeclaredMethods()) {
            Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
            AuthorizationRequired methodAnn = AnnotatedElementUtils.findMergedAnnotation(specificMethod, AuthorizationRequired.class);

            // Se não houver anotação no método E nem na classe, ignora o método para poupar memória e processamento
            if (methodAnn == null && classAnn == null) {
                continue;
            }

            AuthorizationPolicy policy = buildPolicy(methodAnn, classAnn);
            String key = buildKey(targetClass, specificMethod);
            registry.put(key, policy);
        }
    }

    public AuthorizationPolicy resolve(Class<?> targetClass, Method method) {
        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        String key = buildKey(targetClass, specificMethod);
        return registry.getOrDefault(key, AuthorizationPolicy.open());
    }

    private String buildKey(Class<?> clazz, Method method) {
        String params = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(","));
        return String.format("%s#%s(%s)", clazz.getName(), method.getName(), params);
    }

    private AuthorizationPolicy buildPolicy(AuthorizationRequired methodAnn, AuthorizationRequired classAnn) {
        if (methodAnn != null) {
            return new AuthorizationPolicy(methodAnn.level(), AuthorizationPolicy.Source.METHOD);
        }
        if (classAnn != null) {
            return new AuthorizationPolicy(classAnn.level(), AuthorizationPolicy.Source.CLASS);
        }
        return AuthorizationPolicy.open();
    }
}
