package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.message.AuthorizationTechnicalErrors;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import java.lang.reflect.Method;
import java.util.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;

/** Validates allowlists and protected methods before consumers start. */
public final class EntryPolicyConfigurationValidator implements BeanPostProcessor, SmartInitializingSingleton {
    private final PlatformAuthorizationProperties properties;
    private final Map<String, List<Method>> protectedMethods = new LinkedHashMap<>();
    public EntryPolicyConfigurationValidator(PlatformAuthorizationProperties properties) { this.properties = properties; }
    @Override public Object postProcessAfterInitialization(Object bean, String name) {
        for (Method method : AopUtils.getTargetClass(bean).getMethods()) {
            AuthorizationRequired required = method.getAnnotation(AuthorizationRequired.class);
            if (required != null && required.mechanism() == AuthorizationMechanism.POLICY) {
                if (blank(required.policy())) throw invalid();
                protectedMethods.computeIfAbsent(required.policy(), ignored -> new ArrayList<>()).add(method);
            }
        }
        return bean;
    }
    @Override public void afterSingletonsInstantiated() {
        if (properties.getTechnicalPrincipals().stream().anyMatch(this::blank)) throw invalid();
        protectedMethods.forEach((id, methods) -> {
            if (!properties.getEntryPolicies().containsKey(id) || methods.size() != 1) throw invalid();
        });
        properties.getEntryPolicies().forEach((id, policy) -> {
            List<Method> methods = protectedMethods.getOrDefault(id, List.of());
            if (blank(id) || policy == null || methods.size() != 1 || blank(policy.entryPoint()) || blank(policy.facade())
                    || blank(policy.destination()) || blank(policy.queueUrl()) || blank(policy.principal())
                    || !properties.getTechnicalPrincipals().contains(policy.principal()) || policy.action() == null
                    || policy.level() == null || policy.scopes().isEmpty()) throw invalid();
            Method method = methods.getFirst();
            AuthorizationRequired required = method.getAnnotation(AuthorizationRequired.class);
            if (!policy.facade().equals(method.getDeclaringClass().getName() + "#" + method.getName())
                    || required.action() != policy.action() || required.level() != policy.level()) throw invalid();
            for (AuthorizationEntryPolicy.Scope scope : policy.scopes()) {
                if (scope == null || blank(scope.accountIdentifier())
                        || (scope.applicationIdentifier() != null && blank(scope.applicationIdentifier()))
                        || (scope.environmentIdentifier() != null && blank(scope.environmentIdentifier()))) throw invalid();
            }
        });
    }
    private boolean blank(String value) { return value == null || value.isBlank() || value.contains("*") || value.chars().anyMatch(Character::isISOControl); }
    private PlatformConfigurationException invalid() { return new PlatformConfigurationException(AuthorizationTechnicalErrors.ENTRY_POLICY_INVALID); }
}
