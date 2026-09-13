package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.message.CatalogMessageKeys;
import com.empresa.platform.catalog.model.CatalogEnum;
import com.empresa.platform.catalog.repository.BaseCatalogRepository;
import com.empresa.platform.messaging.validation.ValidationResult;

import java.util.Map;

/**
 * Optional validator specialization for managed catalogs whose allowed names
 * are constrained by a Java enum. Fully dynamic managed catalogs should use
 * BaseCatalogValidator directly.
 */
public abstract class EnumCatalogValidator<
        E extends Enum<E> & CatalogEnum<E>,
        D extends BaseCatalogDTO<D, Long>> extends BaseCatalogValidator<D> {

    private final Class<E> enumClass;

    protected EnumCatalogValidator(BaseCatalogRepository<?, Long> repository, Class<E> enumClass) {
        super(repository);
        this.enumClass = enumClass;
    }

    @Override
    protected void validateAdditionalCatalogFields(D dto, ValidationResult result) {
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

    protected void validateEnumCatalogFields(D dto, ValidationResult result) {
    }
}
