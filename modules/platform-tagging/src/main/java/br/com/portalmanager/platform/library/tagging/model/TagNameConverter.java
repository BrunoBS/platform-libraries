package br.com.portalmanager.platform.library.tagging.model;

import br.com.portalmanager.platform.library.tagging.exception.TaggingTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class TagNameConverter implements AttributeConverter<TagName, String> {

    @Override
    public String convertToDatabaseColumn(TagName attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public TagName convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TagName.of(value);
        } catch (ValidationException exception) {
            throw new PlatformConfigurationException(
                    TaggingTechnicalErrors.PERSISTED_NAME_INVALID,
                    exception
            );
        }
    }
}
