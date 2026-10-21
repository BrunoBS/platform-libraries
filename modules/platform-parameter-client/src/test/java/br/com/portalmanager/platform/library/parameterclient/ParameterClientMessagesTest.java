package br.com.portalmanager.platform.library.parameterclient;

import static org.assertj.core.api.Assertions.*;
import br.com.portalmanager.platform.library.messaging.message.PlatformDefaultMessageProvider;
import br.com.portalmanager.platform.library.messaging.model.ApiMessage;
import br.com.portalmanager.platform.library.parameterclient.message.ParameterClientMessageKeys;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ParameterClientMessagesTest {
    @Test
    void resolvesBothLanguagesWithoutChangingErrorCodesOrStatus() {
        PlatformDefaultMessageProvider provider = new PlatformDefaultMessageProvider();
        Map<String, ExpectedError> errors = Map.of(
                ParameterClientMessageKeys.VALIDATION, new ExpectedError("PLT-PARAM-001", 400),
                ParameterClientMessageKeys.READ_FAILED, new ExpectedError("PLT-PARAM-002", 503),
                ParameterClientMessageKeys.VALUE_INVALID, new ExpectedError("PLT-PARAM-003", 422),
                ParameterClientMessageKeys.TIMEOUT, new ExpectedError("PLT-PARAM-004", 504),
                ParameterClientMessageKeys.MISSING, new ExpectedError("PLT-PARAM-006", 404),
                ParameterClientMessageKeys.EXPRESSION_INVALID, new ExpectedError("PLT-PARAM-007", 422),
                ParameterClientMessageKeys.OPERATOR_INVALID, new ExpectedError("PLT-PARAM-008", 422),
                ParameterClientMessageKeys.INPUT_INCOMPATIBLE, new ExpectedError("PLT-PARAM-009", 422));
        for (Locale locale : new Locale[]{Locale.ENGLISH, Locale.forLanguageTag("pt-BR")}) {
            for (Map.Entry<String, ExpectedError> entry : errors.entrySet()) {
                String key = entry.getKey();
                ApiMessage message = provider.find(key, locale).orElseThrow();
                assertThat(key).startsWith(ParameterClientMessageKeys.PREFIX);
                assertThat(message.code()).isEqualTo(entry.getValue().code());
                assertThat(message.httpStatus()).isEqualTo(entry.getValue().status());
                assertThat(message.message()).doesNotContain("{0}");
            }
        }
    }

    private record ExpectedError(String code, int status) {}
}
