package br.com.portalmanager.platform.library.catalog.service;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.mapper.CatalogMapper;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.library.catalog.validation.AbstractCatalogValidator;
import br.com.portalmanager.platform.library.catalog.validation.CatalogSettingsValidator;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import tools.jackson.databind.ObjectMapper;

public abstract non-sealed class DynamicCatalogService<E extends CatalogEntity>
        extends AbstractCatalogService<E> {

    protected DynamicCatalogService(
            CatalogRepository<E> repository, ObjectMapper objectMapper, Class<E> entityClass) {
        this(repository, objectMapper, entityClass, CatalogSettingsValidator.none());
    }

    protected DynamicCatalogService(
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
