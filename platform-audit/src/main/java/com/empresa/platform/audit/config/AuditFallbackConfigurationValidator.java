package com.empresa.platform.audit.config;

import com.empresa.platform.audit.fallback.AuditFallbackStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;

public final class AuditFallbackConfigurationValidator implements SmartInitializingSingleton {

    private final ObjectProvider<AuditFallbackStore> fallbackStoreProvider;

    public AuditFallbackConfigurationValidator(
            ObjectProvider<AuditFallbackStore> fallbackStoreProvider
    ) {
        this.fallbackStoreProvider = fallbackStoreProvider;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (fallbackStoreProvider.getIfAvailable() == null) {
            throw new IllegalStateException(
                    "platform.audit.fallback.enabled=true requires an AuditFallbackStore. "
                            + "Configure Redis support or provide a custom AuditFallbackStore bean."
            );
        }
    }
}
