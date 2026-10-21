package br.com.portalmanager.platform.library.parameterclient.internal;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.node.JsonNodeFactory;

/** Read through RedisJsonCodec without its application's floating-point tree defaults. */
@JsonDeserialize(using = ExactDocument.Deserializer.class)
public record ExactDocument(JsonNode root) {
    public static final class Deserializer extends ValueDeserializer<ExactDocument> {
        private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

        @Override
        public ExactDocument deserialize(JsonParser parser, DeserializationContext context) {
            return new ExactDocument(read(parser));
        }

        private JsonNode read(JsonParser parser) {
            return switch (parser.currentToken()) {
                case START_OBJECT -> {
                    var node = NODES.objectNode();
                    while (parser.nextToken() != tools.jackson.core.JsonToken.END_OBJECT) {
                        String name = parser.currentName();
                        parser.nextToken();
                        node.set(name, read(parser));
                    }
                    yield node;
                }
                case START_ARRAY -> {
                    var node = NODES.arrayNode();
                    while (parser.nextToken() != tools.jackson.core.JsonToken.END_ARRAY) node.add(read(parser));
                    yield node;
                }
                case VALUE_STRING -> NODES.stringNode(parser.getString());
                case VALUE_NUMBER_INT -> NODES.numberNode(parser.getBigIntegerValue());
                case VALUE_NUMBER_FLOAT -> NODES.numberNode(parser.getDecimalValue());
                case VALUE_TRUE -> NODES.booleanNode(true);
                case VALUE_FALSE -> NODES.booleanNode(false);
                case VALUE_NULL -> NODES.nullNode();
                default -> throw ParameterErrors.expressionInvalid();
            };
        }
    }
}
