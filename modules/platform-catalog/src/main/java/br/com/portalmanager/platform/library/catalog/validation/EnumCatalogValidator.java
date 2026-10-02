package br.com.portalmanager.platform.library.catalog.validation;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTOContract;
import br.com.portalmanager.platform.library.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.library.catalog.model.CatalogEnum;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;

import java.util.Map;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;

/**
 * Validator specialization for catalogs whose allowed codes are defined by a
 * Java enum implementing CatalogEnum.
 */
public abstract class EnumCatalogValidator<
        E extends Enum<E> & CatalogEnum<E>,
        D extends CatalogDTOContract<D>> extends AbstractCatalogValidator<D> {

    private final Class<E> enumClass;

    protected EnumCatalogValidator(CatalogRepository<?> repository, Class<E> enumClass) {
        super(repository);
        this.enumClass = enumClass;
    }

    @Override
    protected void validateAdditionalCatalogFields(D dto, ValidationResult result) {
        super.validateAdditionalCatalogFields(dto, result);
        if (dto.code() != null
                && !dto.code().isBlank()
                && CatalogEnum.from(enumClass, dto.code()) == null) {
            result.addError(
                    "code",
                    CatalogMessageKeys.CODE_NOT_ALLOWED,
                    Map.of("0", entityName(), "1", CatalogEnum.getOptionsValid(enumClass))
            );
        }
        validateEnumCatalogFields(dto, result);
    }

    protected void validateEnumCatalogFields(D dto, ValidationResult result) {
    }
}
