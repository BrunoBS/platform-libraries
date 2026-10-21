package br.com.portalmanager.platform.library.parameterclient;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.parameterclient.internal.serialization.ExactDocument;
import br.com.portalmanager.platform.library.parameterclient.internal.RedisExpressionProvider;
import br.com.portalmanager.platform.library.parameterclient.internal.RedisParameterReader;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

class ExpressionProviderTest {
    final RedisStore store = mock(RedisStore.class);
    final RedisJsonCodec codec = new RedisJsonCodec(JsonMapper.builder().build(), 1048576);
    final ExpressionProvider provider = new RedisExpressionProvider(new RedisParameterReader(store, codec));

    void document(String json) {
        // Exercise the actual Redis codec and exact deserializer on every read, including failures.
        doAnswer(call -> Optional.of(codec.decode(json, ExactDocument.class)))
                .when(store).get("rules.value", ExactDocument.class);
    }
    void expression(String operator, String type, String value) {
        document("{\"type\":\"EXPRESSION\",\"value\":{\"operator\":\"" + operator
                + "\",\"valueType\":\"" + type + "\",\"value\":" + value + "}}");
    }
    static Stream<Arguments> operators() {
        return Stream.of(
                Arguments.of("EQUALS", 17, false, true, false),
                Arguments.of("NOT_EQUALS", 17, true, false, true),
                Arguments.of("GREATER_THAN", 17, false, false, true),
                Arguments.of("GREATER_THAN_OR_EQUALS", 17, false, true, true),
                Arguments.of("LESS_THAN", 17, true, false, false),
                Arguments.of("LESS_THAN_OR_EQUALS", 17, true, true, false),
                Arguments.of("CONTAINS", 17, false, true, false),
                Arguments.of("NOT_CONTAINS", 17, true, false, true));
    }
    @ParameterizedTest @MethodSource("operators")
    void allOperatorsUseInputOnLeftAndCoverEquality(String operator, int lower,
            boolean below, boolean equal, boolean above) {
        expression(operator, "INTEGER", operator.contains("CONTAINS") ? "[18]" : "18");
        assertThat(provider.matches("rules.value", lower)).isEqualTo(below);
        assertThat(provider.matches("rules.value", 18)).isEqualTo(equal);
        assertThat(provider.matches("rules.value", 19)).isEqualTo(above);
        // A successful false result must not be replaced by a true default.
        assertThat(provider.matches("rules.value", lower, !below)).isEqualTo(below);
    }
    static Stream<Arguments> numericTypes() {
        return Stream.of("INTEGER", "LONG", "DECIMAL").flatMap(type -> operators().map(row -> {
            Object[] values = row.get();
            return Arguments.of(type, values[0], values[2], values[3], values[4]);
        }));
    }
    @ParameterizedTest @MethodSource("numericTypes")
    void everyNumericTypeSupportsEveryOperator(String type, String operator,
            boolean below, boolean equal, boolean above) {
        expression(operator, type, operator.contains("CONTAINS") ? "[18]" : "18");
        Object low = type.equals("DECIMAL") ? new BigDecimal("17.99") : 17L;
        Object eq = type.equals("DECIMAL") ? new BigDecimal("18.00") : 18L;
        Object high = type.equals("DECIMAL") ? new BigDecimal("18.01") : 19L;
        assertThat(provider.matches("rules.value", low)).isEqualTo(below);
        assertThat(provider.matches("rules.value", eq)).isEqualTo(equal);
        assertThat(provider.matches("rules.value", high)).isEqualTo(above);
    }
    @Test void allOrderingOperatorsRejectStringAndArgumentValidationPrecedesRedis() {
        for (String op : List.of("GREATER_THAN", "GREATER_THAN_OR_EQUALS", "LESS_THAN", "LESS_THAN_OR_EQUALS")) {
            expression(op, "STRING", "\"B\"");
            assertThatThrownBy(() -> provider.matches("rules.value", "A")).hasMessage("parameter-client.expression.invalid");
        }
        for (String key : new String[]{null, "", " ", "\t\n"}) {
            assertThatThrownBy(() -> provider.matches(key, 1)).hasMessage("parameter-client.validation");
            assertThatThrownBy(() -> provider.matches(key, 1, false)).hasMessage("parameter-client.validation");
        }
    }
    @Test void evaluatesNonBlankKeysWithoutRewriting() {
        String json = "{\"type\":\"EXPRESSION\",\"value\":{\"operator\":\"GREATER_THAN_OR_EQUALS\",\"valueType\":\"INTEGER\",\"value\":18}}";
        ExactDocument document = codec.decode(json, ExactDocument.class);
        for (String key : List.of("single", "rules..value", "config:rules.value",
                "dynamic-parameter:rules.value", " rules.value ")) {
            when(store.get(key, ExactDocument.class)).thenReturn(Optional.of(document));
            assertThat(provider.matches(key, 21)).isTrue();
            assertThat(provider.matches(key, 21, false)).isTrue();
            verify(store, times(2)).get(key, ExactDocument.class);
        }
    }
    @Test void stringAndAllNumericMembershipTypesAreExact() {
        expression("EQUALS", "STRING", "\"RETAIL\"");
        assertThat(provider.matches("rules.value", "RETAIL")).isTrue();
        assertThat(provider.matches("rules.value", "retail")).isFalse();
        expression("NOT_EQUALS", "STRING", "\"RETAIL\"");
        assertThat(provider.matches("rules.value", "OTHER")).isTrue();
        for (String operator : List.of("CONTAINS", "NOT_CONTAINS")) {
            boolean expected = operator.equals("CONTAINS");
            expression(operator, "STRING", "[\"RETAIL\",\"VIP\"]");
            assertThat(provider.matches("rules.value", "VIP")).isEqualTo(expected);
            assertThat(provider.matches("rules.value", "OTHER")).isEqualTo(!expected);
            expression(operator, "LONG", "[9223372036854775807,-9223372036854775808]");
            assertThat(provider.matches("rules.value", Long.MAX_VALUE)).isEqualTo(expected);
            assertThat(provider.matches("rules.value", 0L)).isEqualTo(!expected);
            expression(operator, "DECIMAL", "[123456789012345678901234567890.12,0.10]");
            assertThat(provider.matches("rules.value", new BigDecimal("123456789012345678901234567890.12"))).isEqualTo(expected);
            assertThat(provider.matches("rules.value", new BigDecimal("0.100"))).isEqualTo(expected);
            assertThat(provider.matches("rules.value", new BigDecimal("0.11"))).isEqualTo(!expected);
        }
    }
    @Test void nativeTreeReaderPreservesNestedValuesAndDoesNotChangeSharedCodecDefaults() {
        ExactDocument document = codec.decode("{\"nested\":[9223372036854775807,123456789012345678901234567890.12,"
                + "{\"flag\":true,\"empty\":null,\"text\":\"18\"}]}", ExactDocument.class);
        tools.jackson.databind.JsonNode values = document.root().get("nested");
        assertThat(values.get(0).isBigInteger()).isTrue();
        assertThat(values.get(0).bigIntegerValue()).isEqualTo(BigInteger.valueOf(Long.MAX_VALUE));
        assertThat(values.get(1).isBigDecimal()).isTrue();
        assertThat(values.get(1).decimalValue()).isEqualByComparingTo("123456789012345678901234567890.12");
        assertThat(values.get(2).get("flag").asBoolean()).isTrue();
        assertThat(values.get(2).get("empty").isNull()).isTrue();
        assertThat(values.get(2).get("text").asString()).isEqualTo("18");
        assertThat(codec.decode("18", tools.jackson.databind.JsonNode.class).isBigInteger()).isFalse();
        assertThatThrownBy(() -> codec.decode("{} {}", ExactDocument.class)).hasMessage("redis.serialization");
    }
    @Test void numericExtremesFractionsAndScaleDoNotLosePrecision() {
        expression("EQUALS", "LONG", "9223372036854775807");
        assertThat(provider.matches("rules.value", Long.MAX_VALUE)).isTrue();
        assertThat(provider.matches("rules.value", Long.MAX_VALUE - 1)).isFalse();
        expression("LESS_THAN", "LONG", "-9223372036854775807");
        assertThat(provider.matches("rules.value", Long.MIN_VALUE)).isTrue();
        expression("EQUALS", "INTEGER", "-2147483648");
        assertThat(provider.matches("rules.value", Integer.MIN_VALUE)).isTrue();
        expression("EQUALS", "INTEGER", "2147483647");
        assertThat(provider.matches("rules.value", Integer.MAX_VALUE)).isTrue();
        expression("GREATER_THAN", "DECIMAL", "123456789012345678901234567890.12");
        assertThat(provider.matches("rules.value", new BigDecimal("123456789012345678901234567890.13"))).isTrue();
        assertThat(provider.matches("rules.value", new BigDecimal("123456789012345678901234567890.1200"))).isFalse();
        expression("LESS_THAN", "DECIMAL", "0.01");
        assertThat(provider.matches("rules.value", new BigDecimal("0.00999999999999999999"))).isTrue();
    }
    static Stream<Arguments> invalidExpressions() {
        return Stream.of(
            Arguments.of("GREATER_THAN", "STRING", "\"x\""),
            Arguments.of("EQUALS", "UNKNOWN", "1"),
            Arguments.of("CONTAINS", "INTEGER", "[1,\"2\"]"),
            Arguments.of("CONTAINS", "INTEGER", "[]"),
            Arguments.of("CONTAINS", "DECIMAL", "[1.0,1.00]"),
            Arguments.of("CONTAINS", "INTEGER", "1"),
            Arguments.of("EQUALS", "INTEGER", "[1]"),
            Arguments.of("EQUALS", "INTEGER", "null"),
            Arguments.of("EQUALS", "INTEGER", "true"),
            Arguments.of("EQUALS", "INTEGER", "{}"),
            Arguments.of("EQUALS", "INTEGER", "\"18\""),
            Arguments.of("EQUALS", "INTEGER", "1.5"),
            Arguments.of("EQUALS", "INTEGER", "2147483648"),
            Arguments.of("EQUALS", "LONG", "9223372036854775808"),
            Arguments.of("EQUALS", "LONG", "-9223372036854775809"),
            Arguments.of("EQUALS", "DECIMAL", "0.001"));
    }
    @ParameterizedTest @MethodSource("invalidExpressions")
    void rejectsSchemaViolationsAndSupportsLoggedFallback(String operator, String type, String value) {
        expression(operator, type, value);
        assertThatThrownBy(() -> provider.matches("rules.value", 18)).hasMessage("parameter-client.expression.invalid");
        assertThat(provider.matches("rules.value", 18, true)).isTrue();
        assertThat(provider.matches("rules.value", 18, false)).isFalse();
    }
    @Test void rejectsIncompleteDocumentsUnknownOperatorAndMalformedJson() {
        for (String json : List.of("{}", "[]", "null", "{\"type\":\"CONFIGURATION\",\"value\":18}",
                "{\"type\":\"EXPRESSION\",\"value\":{}}",
                "{\"type\":\"EXPRESSION\",\"value\":{\"operator\":\"EQUALS\",\"valueType\":\"INTEGER\",\"value\":18,\"extra\":true}}")) {
            document(json);
            assertThatThrownBy(() -> provider.matches("rules.value", 18)).isInstanceOf(ApiException.class);
            assertThat(provider.matches("rules.value", 18, false)).isFalse();
        }
        expression("AND", "INTEGER", "18");
        assertThatThrownBy(() -> provider.matches("rules.value", 18)).hasMessage("parameter-client.operator.invalid");
        for (String json : List.of("not-json", "{", "{} {}", "[", "{\"type\":")) {
            document(json);
            assertThatThrownBy(() -> provider.matches("rules.value", 18)).hasMessage("parameter-client.value.invalid");
            assertThat(provider.matches("rules.value", 18, true)).isTrue();
        }
    }
    @Test void validatesTheEntireConfigurationBeforeEvaluatingTheInput() {
        for (String values : List.of("[18,\"21\"]", "[18,18]", "[18,null]")) {
            expression("CONTAINS", "INTEGER", values);
            // A matching first element cannot hide an invalid element later in the array.
            assertThatThrownBy(() -> provider.matches("rules.value", 18))
                    .hasMessage("parameter-client.expression.invalid");
            assertThat(provider.matches("rules.value", 18, false)).isFalse();
        }
        expression("EQUALS", "INTEGER", "18.5");
        assertThatThrownBy(() -> provider.matches("rules.value", "invalid input"))
                .hasMessage("parameter-client.expression.invalid");
        expression("UNKNOWN", "INTEGER", "18");
        assertThatThrownBy(() -> provider.matches("rules.value", "invalid input"))
                .hasMessage("parameter-client.operator.invalid");
    }
    @Test void incompatibleInputNeverCoercesStringsFloatsOrContainers() {
        expression("EQUALS", "INTEGER", "18");
        for (Object value : List.of("18", 18.0, new BigDecimal("18"), true, List.of(18),
                BigInteger.valueOf(Long.MAX_VALUE))) {
            assertThatThrownBy(() -> provider.matches("rules.value", value)).hasMessage("parameter-client.input.incompatible");
            assertThat(provider.matches("rules.value", value, false)).isFalse();
        }
        expression("EQUALS", "STRING", "\"18\"");
        assertThatThrownBy(() -> provider.matches("rules.value", 18)).hasMessage("parameter-client.input.incompatible");
        expression("EQUALS", "DECIMAL", "18");
        assertThatThrownBy(() -> provider.matches("rules.value", Double.NaN)).hasMessage("parameter-client.input.incompatible");
    }
    @Test void absenceInfrastructureFallbackAndSafeLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(RedisExpressionProvider.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>(); appender.start(); logger.addAppender(appender);
        try {
            assertThatThrownBy(() -> provider.matches("rules.value", 18)).hasMessage("parameter-client.missing");
            assertThat(provider.matches("rules.value", 18, true)).isTrue();
            for (String error : List.of("redis.unavailable", "redis.timeout", "redis.serialization", "redis.type")) {
                doThrow(new ApiException(error, new RuntimeException("SECRET_JSON")))
                        .when(store).get("rules.value", ExactDocument.class);
                String expected = error.equals("redis.timeout") ? "parameter-client.timeout"
                        : error.equals("redis.unavailable") ? "parameter-client.read.failed" : "parameter-client.value.invalid";
                assertThatThrownBy(() -> provider.matches("rules.value", 18)).hasMessage(expected).hasCause(null);
                assertThat(provider.matches("rules.value", 18, false)).isFalse();
            }
            expression("AND", "INTEGER", "18");
            assertThat(provider.matches("rules.value", 18, false)).isFalse();
            expression("EQUALS", "INTEGER", "null");
            assertThat(provider.matches("rules.value", 18, false)).isFalse();
            expression("EQUALS", "INTEGER", "18");
            assertThat(provider.matches("rules.value", "SECRET_INPUT", false)).isFalse();
            assertThat(appender.list).hasSize(8).allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).contains("parameter-client.").doesNotContain("SECRET_JSON", "SECRET_INPUT", "rules.value");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
    @Test void argumentAndProgrammingErrorsAreNotHiddenByFallback() {
        assertThatThrownBy(() -> provider.matches("", 18, false)).hasMessage("parameter-client.validation");
        assertThatThrownBy(() -> provider.matches("rules.value", null, true)).hasMessage("parameter-client.validation");
        verifyNoInteractions(store);
        doThrow(new IllegalStateException("programming error")).when(store).get("rules.value", ExactDocument.class);
        assertThatThrownBy(() -> provider.matches("rules.value", 18, false)).isInstanceOf(IllegalStateException.class);
        doThrow(new AssertionError("fatal")).when(store).get("rules.value", ExactDocument.class);
        assertThatThrownBy(() -> provider.matches("rules.value", 18, false)).isInstanceOf(AssertionError.class);
    }
}
