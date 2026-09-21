package br.com.portalmanager.platform.authorization.registry;

import br.com.portalmanager.platform.authorization.annotation.AuthorizationAccessPolicy;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.authorization.model.AuthorizationPolicy;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Locale;

@Component
public class AuthorizationMetadataRegistry {

    public AuthorizationPolicy resolve(
            Class<?> targetClass,
            Method method,
            String httpMethod) {

        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

        AuthorizationRequired methodAnn =
                AnnotatedElementUtils.findMergedAnnotation(
                        specificMethod,
                        AuthorizationRequired.class
                );

        if (methodAnn == null && specificMethod != method) {
            methodAnn = AnnotatedElementUtils.findMergedAnnotation(
                    method,
                    AuthorizationRequired.class
            );
        }

        if (methodAnn != null) {
            return new AuthorizationPolicy(
                    methodAnn.level(),
                    AuthorizationPolicy.Source.METHOD
            );
        }

        AuthorizationAccessPolicy classPolicy =
                AnnotatedElementUtils.findMergedAnnotation(
                        targetClass,
                        AuthorizationAccessPolicy.class
                );

        if (classPolicy != null) {
            return new AuthorizationPolicy(
                    resolveClassPolicy(classPolicy, httpMethod),
                    AuthorizationPolicy.Source.CLASS_POLICY
            );
        }

        AuthorizationRequired classAnn =
                AnnotatedElementUtils.findMergedAnnotation(
                        targetClass,
                        AuthorizationRequired.class
                );

        if (classAnn != null) {
            return new AuthorizationPolicy(
                    classAnn.level(),
                    AuthorizationPolicy.Source.CLASS
            );
        }

        return AuthorizationPolicy.open();
    }

    private AuthorizationLevel resolveClassPolicy(
            AuthorizationAccessPolicy policy,
            String httpMethod) {

        if (httpMethod == null) {
            return policy.read();
        }

        return switch (httpMethod.toUpperCase(Locale.ROOT)) {
            case "GET", "HEAD", "OPTIONS" -> policy.read();
            default -> policy.write();
        };
    }
}
