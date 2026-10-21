package br.com.portalmanager.platform.library.parameterclient;

import static org.assertj.core.api.Assertions.*;
import br.com.portalmanager.platform.library.messaging.message.PlatformDefaultMessageProvider;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ParameterClientMessagesTest {
    @Test
    void resolvesBothLanguagesWithoutValuesInTheMessages() {
        var provider = new PlatformDefaultMessageProvider();
        for (Locale locale : new Locale[]{Locale.ENGLISH, Locale.forLanguageTag("pt-BR")}) {
            for (String key : new String[]{"validation", "read.failed", "value.invalid", "timeout", "missing", "expression.invalid", "operator.invalid", "input.incompatible"}) {
                var message = provider.find("parameter-client." + key, locale).orElseThrow();
                assertThat(message.code()).startsWith("PLT-PARAM-");
                assertThat(message.message()).doesNotContain("{0}");
            }
        }
    }
}
