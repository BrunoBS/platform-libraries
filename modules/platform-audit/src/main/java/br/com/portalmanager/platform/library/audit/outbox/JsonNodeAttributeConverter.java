package br.com.portalmanager.platform.library.audit.outbox;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Converts JSON tree values without relying on Hibernate's selected JSON mapper. */
@Converter
public class JsonNodeAttributeConverter implements AttributeConverter<JsonNode, String> {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        return attribute == null ? null : JSON_MAPPER.writeValueAsString(attribute);
    }

    @Override
    public JsonNode convertToEntityAttribute(String databaseValue) {
        return databaseValue == null ? null : JSON_MAPPER.readTree(databaseValue);
    }
}
