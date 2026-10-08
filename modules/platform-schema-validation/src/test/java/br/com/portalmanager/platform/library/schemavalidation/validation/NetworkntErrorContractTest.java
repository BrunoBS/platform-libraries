package br.com.portalmanager.platform.library.schemavalidation.validation;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkntErrorContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SchemaRegistry schemaRegistry =
            SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

    @Test
    void shouldCharacterizeNetworkntErrorsUsedByPlatformMessages() throws Exception {
        List<Case> cases = List.of(
                new Case("required", "{\"type\":\"object\",\"required\":[\"name\"]}", "{}", "required"),
                new Case("type", "{\"type\":\"string\"}", "1", "type"),
                new Case("minLength", "{\"type\":\"string\",\"minLength\":3}", "\"a\"", "minLength"),
                new Case("maxLength", "{\"type\":\"string\",\"maxLength\":3}", "\"abcd\"", "maxLength"),
                new Case("minimum", "{\"type\":\"number\",\"minimum\":10}", "5", "minimum"),
                new Case("maximum", "{\"type\":\"number\",\"maximum\":10}", "11", "maximum"),
                new Case("pattern", "{\"type\":\"string\",\"pattern\":\"^[A-Z]+$\"}", "\"abc\"", "pattern"),
                new Case("enum", "{\"enum\":[\"A\",\"B\"]}", "\"C\"", "enum"),
                new Case("additionalProperties", "{\"type\":\"object\",\"additionalProperties\":false}", "{\"x\":1}", "additionalProperties"),
                new Case("const", "{\"const\":\"A\"}", "\"B\"", "const"),
                new Case("exclusiveMinimum", "{\"type\":\"number\",\"exclusiveMinimum\":10}", "10", "exclusiveMinimum"),
                new Case("exclusiveMaximum", "{\"type\":\"number\",\"exclusiveMaximum\":10}", "10", "exclusiveMaximum"),
                new Case("multipleOf", "{\"type\":\"number\",\"multipleOf\":2}", "3", "multipleOf"),
                new Case("minItems", "{\"type\":\"array\",\"minItems\":2}", "[1]", "minItems"),
                new Case("maxItems", "{\"type\":\"array\",\"maxItems\":1}", "[1,2]", "maxItems"),
                new Case("uniqueItems", "{\"type\":\"array\",\"uniqueItems\":true}", "[1,1]", "uniqueItems"),
                new Case("containsDefaultMinimum", "{\"type\":\"array\",\"contains\":{\"const\":1}}", "[2]", "minContains"),
                new Case("minContains", "{\"type\":\"array\",\"contains\":{\"const\":1},\"minContains\":2}", "[1]", "minContains"),
                new Case("maxContains", "{\"type\":\"array\",\"contains\":{\"const\":1},\"maxContains\":1}", "[1,1]", "maxContains"),
                new Case("minProperties", "{\"type\":\"object\",\"minProperties\":2}", "{\"a\":1}", "minProperties"),
                new Case("maxProperties", "{\"type\":\"object\",\"maxProperties\":1}", "{\"a\":1,\"b\":2}", "maxProperties"),
                new Case("dependentRequired", "{\"type\":\"object\",\"dependentRequired\":{\"a\":[\"b\"]}}", "{\"a\":1}", "dependentRequired"),
                new Case("propertyNames", "{\"type\":\"object\",\"propertyNames\":{\"pattern\":\"^[a-z]+$\"}}", "{\"BAD\":1}", "propertyNames"),
                new Case("oneOf", "{\"oneOf\":[{\"type\":\"string\"},{\"type\":\"integer\"}]}", "true", "oneOf"),
                new Case("not", "{\"not\":{\"type\":\"string\"}}", "\"blocked\"", "not"),
                new Case("unevaluatedProperties", "{\"type\":\"object\",\"properties\":{\"a\":{}},\"unevaluatedProperties\":false}", "{\"a\":1,\"b\":2}", "unevaluatedProperties"),
                new Case("unevaluatedItems", "{\"type\":\"array\",\"prefixItems\":[{}],\"unevaluatedItems\":false}", "[1,2]", "unevaluatedItems")
        );

        for (Case testCase : cases) {
            List<Error> errors = validate(testCase.schema(), testCase.payload());
            assertThat(errors.isEmpty())
                    .as(testCase.name())
                    .isFalse();
            Error error = find(errors, testCase.expectedKeyword());
            printContract(testCase.name(), error);
            assertThat(errors.stream().anyMatch(candidate -> testCase.expectedKeyword().equals(candidate.getKeyword())))
                    .as(() -> testCase.name() + " expected keyword " + testCase.expectedKeyword()
                            + " but got " + errors.stream().map(Error::getKeyword).toList())
                    .isTrue();
        }
    }

    @Test
    void shouldExposeExpectedArgumentsForRepresentativeKeywords() throws Exception {
        assertArguments("{\"type\":\"string\",\"minLength\":3}", "\"a\"", "minLength", "3");
        assertArguments("{\"type\":\"number\",\"minimum\":10}", "5", "minimum", "10");
        assertArguments("{\"type\":\"array\",\"minItems\":2}", "[1]", "minItems", "2", "1");
        assertArguments("{\"const\":\"EXPECTED\"}", "\"OTHER\"", "const", "EXPECTED", "OTHER");
        assertArguments("{\"enum\":[\"A\",\"B\"]}", "\"C\"", "enum", "[\"A\", \"B\"]");
        assertArguments("{\"type\":\"array\",\"contains\":{\"const\":1},\"minContains\":2}", "[1]",
                "minContains", "2", "{\"const\":1}");
    }

    @Test
    void shouldConfirmKeywordsWithoutUsefulArguments() throws Exception {
        Error uniqueItems = find(validate(
                "{\"type\":\"array\",\"uniqueItems\":true}",
                "[1,1]"
        ), "uniqueItems");

        assertThat(uniqueItems.getArguments())
                .isNull();
    }

    @Test
    void shouldKeepFormatNonAssertingWithCurrentRegistryConfiguration() throws Exception {
        List<Error> errors = validate(
                "{\"type\":\"string\",\"format\":\"email\"}",
                "\"not-an-email\""
        );

        assertThat(errors.isEmpty())
                .isTrue();
    }

    @Test
    void shouldCharacterizeContainsKeywordBehavior() throws Exception {
        List<Error> errors = validate(
                "{\"type\":\"array\",\"contains\":{\"const\":1}}",
                "[2,3]"
        );

        assertThat(errors.isEmpty())
                .isFalse();
        errors.forEach(error -> printContract("containsCharacterization", error));
    }

    @Test
    void shouldCharacterizeFormatWhenAssertionsAreEnabled() throws Exception {
        JsonNode schemaNode = objectMapper.readTree(
                "{\"type\":\"string\",\"format\":\"email\"}"
        );
        JsonNode payloadNode = objectMapper.readTree("\"not-an-email\"");
        Schema schema = schemaRegistry.getSchema(schemaNode);

        List<Error> errors = schema.validate(
                payloadNode,
                executionContext -> executionContext.executionConfig(
                        executionConfig -> executionConfig.formatAssertionsEnabled(true)
                )
        );

        assertThat(errors.isEmpty())
                .isFalse();
        errors.forEach(error -> printContract("formatAssertionEnabled", error));
        assertThat(errors.stream().anyMatch(error -> "format".equals(error.getKeyword())))
                .isTrue();
    }

    @Test
    void shouldCharacterizeAllOfAggregateBehavior() throws Exception {
        List<Error> errors = validate(
                "{\"allOf\":[{\"type\":\"string\"},{\"minLength\":3}]}",
                "1"
        );

        assertThat(errors.isEmpty())
                .isFalse();
        errors.forEach(error -> printContract("allOfAggregate", error));
    }

    @Test
    void shouldCharacterizeAnyOfAggregateBehavior() throws Exception {
        List<Error> errors = validate(
                "{\"anyOf\":[{\"type\":\"string\"},{\"type\":\"integer\"}]}",
                "true"
        );

        assertThat(errors.isEmpty())
                .isFalse();
        errors.forEach(error -> printContract("anyOfAggregate", error));
    }

    @Test
    void shouldCharacterizeCompositionBehaviorWithoutInventingAggregateErrors() throws Exception {
        assertControlledComposition("{\"allOf\":[{\"type\":\"string\"},{\"minLength\":3}]}", "1");
        assertControlledComposition("{\"anyOf\":[{\"type\":\"string\"},{\"type\":\"integer\"}]}", "true");
    }

    private void assertControlledComposition(String definition, String payload) throws Exception {
        List<Error> errors = validate(definition, payload);
        assertThat(errors.isEmpty())
                .isFalse();
        assertThat(errors.stream().allMatch(error -> error.getKeyword() != null && !error.getKeyword().isBlank()))
                .isTrue();
    }

    private void printContract(String scenario, Error error) {
        String arguments = error.getArguments() == null
                ? "null"
                : Arrays.toString(error.getArguments());

        System.out.printf(
                "NETWORKNT_CONTRACT scenario=%s keyword=%s property=%s arguments=%s%n",
                scenario,
                error.getKeyword(),
                error.getProperty(),
                arguments
        );
    }

    private void assertArguments(
            String definition,
            String payload,
            String keyword,
            String... expected
    ) throws Exception {
        Error error = find(validate(definition, payload), keyword);
        List<String> actual = Arrays.stream(error.getArguments())
                .map(String::valueOf)
                .toList();
        assertThat(actual)
                .as(keyword)
                .isEqualTo(List.of(expected));
    }

    private Error find(List<Error> errors, String keyword) {
        return errors.stream()
                .filter(error -> keyword.equals(error.getKeyword()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected " + keyword + " but got " + errors.stream().map(Error::getKeyword).toList()
                ));
    }

    private List<Error> validate(String definition, String payload) throws Exception {
        JsonNode schemaNode = objectMapper.readTree(definition);
        JsonNode payloadNode = objectMapper.readTree(payload);
        Schema schema = schemaRegistry.getSchema(schemaNode);
        return schema.validate(payloadNode);
    }

    private record Case(String name, String schema, String payload, String expectedKeyword) {
    }
}
