package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationPolicy;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

public class AuthorizationMetadataRegistry {

    public AuthorizationPolicy resolve(Class<?> targetClass, Method method) {
        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

        AuthorizationRequired methodAnn = AnnotatedElementUtils.findMergedAnnotation(
                specificMethod, AuthorizationRequired.class);
        if (methodAnn == null && specificMethod != method) {
            methodAnn = AnnotatedElementUtils.findMergedAnnotation(method, AuthorizationRequired.class);
        }
        if (methodAnn != null) {
            return policy(methodAnn, AuthorizationPolicy.Source.METHOD);
        }

        AuthorizationRequired classAnn = AnnotatedElementUtils.findMergedAnnotation(
                targetClass, AuthorizationRequired.class);
        if (classAnn != null) {
            return policy(classAnn, AuthorizationPolicy.Source.CLASS);
        }

        return AuthorizationPolicy.open();
    }

    private AuthorizationPolicy policy(AuthorizationRequired annotation, AuthorizationPolicy.Source source) {
        return new AuthorizationPolicy(
                annotation.level(), source,
                annotation.workspacePathVariable(),
                annotation.applicationPathVariable(),
                annotation.environmentPathVariable());
    }
}
