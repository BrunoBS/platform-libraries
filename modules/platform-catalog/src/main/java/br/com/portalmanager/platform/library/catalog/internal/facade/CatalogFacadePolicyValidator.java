package br.com.portalmanager.platform.library.catalog.internal.facade;

import br.com.portalmanager.platform.library.authorization.api.annotation.AuthorizationRequired;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/** Fails startup when a consumer overrides a protected catalog operation without a policy. */
public final class CatalogFacadePolicyValidator {
    private CatalogFacadePolicyValidator() {}

    public static void validate(Object facade) {
        Class<?> type = AopUtils.getTargetClass(facade);
        for (Method operation : AbstractCatalogFacade.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(operation.getModifiers())) {
                continue;
            }
            try {
                Method effective = type.getMethod(operation.getName(), operation.getParameterTypes());
                if (effective.getDeclaringClass() != AbstractCatalogFacade.class
                        && effective.getAnnotation(AuthorizationRequired.class) == null) {
                    throw new IllegalStateException("Catalog facade " + type.getName()
                            + " overrides " + operation.getName() + " without @AuthorizationRequired");
                }
            } catch (NoSuchMethodException failure) {
                throw new IllegalStateException("Missing catalog facade operation: " + operation.getName(), failure);
            }
        }
    }
}
