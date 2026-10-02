package br.com.portalmanager.platform.library.catalog.validation;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.library.catalog.model.CatalogEnum;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;

import java.util.Map;

public abstract class EnumCatalogValidator<E extends Enum<E> & CatalogEnum<E>>
        extends AbstractCatalogValidator {

    private final Class<E> enumClass;

    protected EnumCatalogValidator(CatalogRepository<?> repository, Class<E> enumClass) {
        super(repository);
        this.enumClass = enumClass;
    }

    @Override
    protected final void validateCatalogFields(CatalogDTO dto, ValidationResult result) {
        if (dto.code() != null && !dto.code().isBlank()
                && CatalogEnum.from(enumClass, dto.code()) == null) {
            result.addError("code", CatalogMessageKeys.CODE_NOT_ALLOWED,
                    Map.of("0", entityName(), "1", CatalogEnum.getOptionsValid(enumClass)));
        }
    }
}
