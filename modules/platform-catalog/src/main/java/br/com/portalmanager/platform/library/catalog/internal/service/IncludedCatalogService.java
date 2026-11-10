package br.com.portalmanager.platform.library.catalog.internal.service;

import br.com.portalmanager.platform.library.catalog.api.contract.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.internal.mapping.CatalogMapper;
import br.com.portalmanager.platform.library.catalog.api.contract.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.internal.repository.CatalogRepository;
import br.com.portalmanager.platform.library.catalog.internal.validation.AbstractCatalogValidator;
import br.com.portalmanager.platform.library.catalog.internal.validation.CatalogSettingsValidator;
import br.com.portalmanager.platform.library.messaging.api.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.api.contract.validation.SchemaValidator;
import tools.jackson.databind.ObjectMapper;

public abstract non-sealed class IncludedCatalogService<E extends CatalogEntity>
        extends AbstractCatalogService<E> {

    protected IncludedCatalogService(
            CatalogRepository<E> repository, ObjectMapper objectMapper, Class<E> entityClass) {
        this(repository, objectMapper, entityClass, CatalogSettingsValidator.none());
    }

    protected IncludedCatalogService(
            CatalogRepository<E> repository, ObjectMapper objectMapper, Class<E> entityClass,
            String schemaResourceCode, SchemaValidator schemaValidator) {
        this(repository, objectMapper, entityClass,
                CatalogSettingsValidator.schema(schemaResourceCode, schemaValidator));
    }

    protected IncludedCatalogService(
            CatalogRepository<E> repository, ObjectMapper objectMapper, Class<E> entityClass,
            CatalogSettingsValidator settingsValidator) {
        super(repository, new CatalogMapper<>(entityClass, objectMapper),
                validator(repository, entityClass, settingsValidator));
    }

    private static <E extends CatalogEntity> AbstractCatalogValidator validator(
            CatalogRepository<E> repository, Class<E> entityClass,
            CatalogSettingsValidator settingsValidator) {
        CatalogSettingsValidator resolved =
                settingsValidator == null ? CatalogSettingsValidator.none() : settingsValidator;
        return new AbstractCatalogValidator(repository) {
            @Override
            protected void validateSettings(CatalogDTO dto, ValidationResult result) {
                resolved.validate(dto, result);
            }

            @Override
            public String entityName() {
                return entityClass.getSimpleName();
            }
        };
    }
}
