package br.com.portalmanager.platform.library.catalog.internal.service;

import br.com.portalmanager.platform.library.catalog.api.contract.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.internal.mapping.CatalogMapper;
import br.com.portalmanager.platform.library.catalog.api.contract.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.api.contract.model.CatalogEnum;
import br.com.portalmanager.platform.library.catalog.internal.repository.CatalogRepository;
import br.com.portalmanager.platform.library.catalog.internal.validation.CatalogSettingsValidator;
import br.com.portalmanager.platform.library.catalog.internal.validation.EnumCatalogValidator;
import br.com.portalmanager.platform.library.messaging.api.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.api.contract.validation.SchemaValidator;
import tools.jackson.databind.ObjectMapper;

public abstract non-sealed class EnumCatalogService<
        E extends CatalogEntity,
        C extends Enum<C> & CatalogEnum<C>>
        extends AbstractCatalogService<E> {

    protected EnumCatalogService(
            CatalogRepository<E> repository, ObjectMapper objectMapper,
            Class<E> entityClass, Class<C> enumClass) {
        this(repository, objectMapper, entityClass, enumClass, CatalogSettingsValidator.none());
    }

    protected EnumCatalogService(
            CatalogRepository<E> repository, ObjectMapper objectMapper,
            Class<E> entityClass, Class<C> enumClass,
            String schemaResourceCode, SchemaValidator schemaValidator) {
        this(repository, objectMapper, entityClass, enumClass,
                CatalogSettingsValidator.schema(schemaResourceCode, schemaValidator));
    }

    protected EnumCatalogService(
            CatalogRepository<E> repository, ObjectMapper objectMapper,
            Class<E> entityClass, Class<C> enumClass,
            CatalogSettingsValidator settingsValidator) {
        super(repository, new CatalogMapper<>(entityClass, objectMapper),
                validator(repository, entityClass, enumClass, settingsValidator));
    }

    private static <E extends CatalogEntity, C extends Enum<C> & CatalogEnum<C>>
    EnumCatalogValidator<C> validator(
            CatalogRepository<E> repository, Class<E> entityClass,
            Class<C> enumClass, CatalogSettingsValidator settingsValidator) {
        CatalogSettingsValidator resolved =
                settingsValidator == null ? CatalogSettingsValidator.none() : settingsValidator;
        return new EnumCatalogValidator<>(repository, enumClass) {
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
