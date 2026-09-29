package br.com.portalmanager.platform.library.authorization.resource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * POC contract for platform-managed Hibernate resource visibility.
 *
 * This test intentionally starts disabled while the bootstrap mapping hook is implemented.
 * Acceptance criteria:
 * 1. entity has no Hibernate @Filter/@FilterDef annotations;
 * 2. collection query is restricted by the Session filter;
 * 3. OWNER executes unrestricted;
 * 4. direct load/find-by-id is explicitly validated (Hibernate filters do not protect load-by-key by default);
 * 5. concurrent Sessions never share filter parameters;
 * 6. filter is disabled/cleaned in finally.
 */
@Disabled("POC: enable when programmatic Hibernate filter mapping is registered")
class ResourceVisibilityHibernatePocTest {

    @Test
    void shouldFilterCollectionWithoutEntityFilterAnnotation() {
        // TODO POC: bootstrap clean test entity, register filter programmatically,
        // enable it on the current Session and assert only allowed rows are returned.
    }

    @Test
    void shouldNotRestrictOwner() {
        // TODO POC: owner must bypass Session filter activation.
    }

    @Test
    void shouldDefineSafeFindByIdBehavior() {
        // TODO POC: prove load-by-key behavior explicitly before choosing RESOURCE strategy.
    }

    @Test
    void shouldIsolateVisibilityBetweenConcurrentSessions() {
        // TODO POC: two Sessions, different allowed values, repeated concurrent queries.
    }

    @Test
    void shouldCleanupFilterAfterInvocation() {
        // TODO POC: verify filter is absent after the guarded execution finishes.
    }
}
