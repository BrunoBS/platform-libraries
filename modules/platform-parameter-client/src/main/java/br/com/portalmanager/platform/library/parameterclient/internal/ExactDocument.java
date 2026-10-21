package br.com.portalmanager.platform.library.parameterclient.internal;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.json.JsonMapper;

/** Read through RedisJsonCodec without its application's floating-point tree defaults. */
@JsonDeserialize(using = ExactDocument.Deserializer.class)
public record ExactDocument(JsonNode root) {
    public static final class Deserializer extends ValueDeserializer<ExactDocument> {
        // Immutable, reusable reader; does not mutate the application's mapper or reparse JSON text.
        private static final ObjectReader EXACT_TREE = JsonMapper.builder()
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .build().readerFor(JsonNode.class);

        @Override
        public ExactDocument deserialize(JsonParser parser, DeserializationContext context) {
            return new ExactDocument(EXACT_TREE.readValue(parser));
        }
    }
}
