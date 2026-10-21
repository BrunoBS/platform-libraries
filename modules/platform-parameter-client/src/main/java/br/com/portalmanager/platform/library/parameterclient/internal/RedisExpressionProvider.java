package br.com.portalmanager.platform.library.parameterclient.internal;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.parameterclient.ExpressionProvider;
import br.com.portalmanager.platform.library.parameterclient.internal.expression.ExpressionEvaluator;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Boolean evaluation of EXPRESSION values, with explicit controlled fallback. */
public final class RedisExpressionProvider implements ExpressionProvider {
    private static final Logger LOG = LoggerFactory.getLogger(RedisExpressionProvider.class);
    private final RedisParameterReader reader;

    public RedisExpressionProvider(RedisParameterReader reader) {
        this.reader = Objects.requireNonNull(reader);
    }

    @Override
    public boolean matches(String key, Object input) {
        validateExpressionArguments(key, input);
        return evaluate(key, input);
    }

    @Override
    public boolean matches(String key, Object input, boolean defaultValue) {
        validateExpressionArguments(key, input);
        try {
            return evaluate(key, input);
        } catch (ApiException failure) {
            LOG.warn("Parameter expression fallback: error={}", failure.getMessageKey());
            return defaultValue;
        }
    }

    private void validateExpressionArguments(String key, Object input) {
        if (input == null) throw ParameterErrors.invalid();
        RedisParameterReader.validateKey(key);
    }

    private boolean evaluate(String key, Object input) {
        return ExpressionEvaluator.evaluate(reader.expression(key), input);
    }
}
