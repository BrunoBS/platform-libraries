package br.com.portalmanager.platform.library.schemavalidation.normalization;

import tools.jackson.databind.JsonNode;

/** Normalizes known values without removing unknown properties or coercing JSON types. */
public interface SchemaPayloadNormalizer {
    JsonNode normalizePayload(JsonNode payload);
}
