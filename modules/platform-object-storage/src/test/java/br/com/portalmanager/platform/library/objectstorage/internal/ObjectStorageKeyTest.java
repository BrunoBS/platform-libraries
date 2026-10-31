package br.com.portalmanager.platform.library.objectstorage.internal;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectStorageKeyTest {
    @Test
    void shouldApplyAndRemoveDestinationPrefixAndSuffix() {
        String physical = ObjectStorageKey.resolve("audit/", ".json", "feature-config");

        assertEquals("audit/feature-config.json", physical);
        assertEquals("feature-config", ObjectStorageKey.logical("audit/", ".json", physical));
    }

    @Test
    void shouldRejectTraversalAndDuplicatedDestinationAffixes() {
        assertThrows(ValidationException.class, () -> ObjectStorageKey.resolve("audit", ".json", "../secret"));
        assertThrows(ValidationException.class, () -> ObjectStorageKey.resolve("audit", ".json", "audit/item"));
        assertThrows(ValidationException.class, () -> ObjectStorageKey.resolve("audit", ".json", "item.json"));
        assertThrows(ValidationException.class, () -> ObjectStorageKey.resolve("audit", ".json", "/item"));
    }

    @Test
    void shouldIgnoreObjectsOutsideConfiguredSuffix() {
        assertNull(ObjectStorageKey.logical("audit", ".json", "audit/other.bin"));
    }
}
