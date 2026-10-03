package br.com.portalmanager.platform.library.tagging.model;

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
        return value == null ? null : TagName.of(value);
    }
}
