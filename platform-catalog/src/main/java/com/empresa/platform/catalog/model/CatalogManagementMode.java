package com.empresa.platform.catalog.model;

/**
 * Defines how a catalog is governed.
 *
 * MANAGED catalogs are database-driven and may evolve without rebuilding the consumer.
 * MANAGED_CONSTRAINED catalogs are administratively managed but their allowed semantic
 * names are constrained by code, usually through a CatalogEnum.
 */
public enum CatalogManagementMode {
    MANAGED,
    MANAGED_CONSTRAINED
}
