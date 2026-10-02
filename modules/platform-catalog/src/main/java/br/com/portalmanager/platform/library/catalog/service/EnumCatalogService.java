package br.com.portalmanager.platform.library.catalog.service;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.mapper.CatalogMapper;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.model.CatalogEnum;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.library.catalog.validation.CatalogSettingsValidator;
import br.com.portalmanager.platform.library.catalog.validation.EnumCatalogValidator;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
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
