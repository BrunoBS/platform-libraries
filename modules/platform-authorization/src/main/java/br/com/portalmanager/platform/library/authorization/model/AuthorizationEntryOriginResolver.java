package br.com.portalmanager.platform.library.authorization.model;

import java.util.Optional;

/** Infrastructure SPI. Application bootstrap is trusted; external request data cannot register a resolver. */
@FunctionalInterface
public interface AuthorizationEntryOriginResolver {
    Optional<AuthorizationEntryOrigin> current();
}
