package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.message.CatalogMessageKeys;
import com.empresa.platform.catalog.model.CatalogEnum;
import com.empresa.platform.catalog.model.CatalogManagementMode;
import com.empresa.platform.catalog.repository.BaseCatalogRepository;
import com.empresa.platform.crud.validation.CrudValidationResult;

import java.util.Map;

/**
 * Validator specialization for MANAGED_CONSTRAINED catalogs whose allowed
 * semantic names are constrained by a Java enum.
 */
public abstract class EnumCatalogValidator<
        E extends Enum<E> & CatalogEnum<E>,
        D extends BaseCatalogDTO<D>> extends BaseCatalogValidator<D> {

    private final Class<E> enumClass;

    protected EnumCatalogValidator(BaseCatalogRepository<?> repository, Class<E> enumClass) {
        super(repository);
        this.enumClass = enumClass;
    }

    @Override
    public CatalogManagementMode managementMode() {
        return CatalogManagementMode.MANAGED_CONSTRAINED;
    }

    @Override
    protected void validateAdditionalCatalogFields(D dto, CrudValidationResult result) {
        super.validateAdditionalCatalogFields(dto, result);
        if (dto.name() != null && !dto.name().isBlank() && CatalogEnum.from(enumClass, dto.name()) == null) {
            result.addError(
                    "name",
                    CatalogMessageKeys.NAME_NOT_ALLOWED,
                    Map.of("0", entityName(), "1", CatalogEnum.getOptionsValid(enumClass))
            );
        }
        validateEnumCatalogFields(dto, result);
    }

    protected void validateEnumCatalogFields(D dto, CrudValidationResult result) {
    }
}
