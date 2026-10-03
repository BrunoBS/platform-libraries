package br.com.portalmanager.platform.library.messagequeue.message;

import br.com.portalmanager.platform.library.messaging.message.PlatformDefaultMessageProvider;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MessageQueueMessageBundleTest {

    private final PlatformDefaultMessageProvider provider = new PlatformDefaultMessageProvider();

    @Test
    void shouldResolvePortuguesePublishFailure() {
        var message = provider.find(MessageQueueMessageKeys.PUBLISH_FAILED, Locale.forLanguageTag("pt-BR"))
                .orElseThrow();

        assertThat(message.code()).isEqualTo("PLT-MQ-002");
        assertThat(message.message()).contains("Falha ao publicar mensagem");
        assertThat(message.solution()).contains("Verifique a disponibilidade do broker");
        assertThat(message.httpStatus()).isEqualTo(500);
    }

    @Test
    void shouldResolveEnglishPublishFailure() {
        var message = provider.find(MessageQueueMessageKeys.PUBLISH_FAILED, Locale.ENGLISH).orElseThrow();

        assertThat(message.message()).contains("Failed to publish a message");
        assertThat(message.locale()).isEqualTo("en");
    }
}
