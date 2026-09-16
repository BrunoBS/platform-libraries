package com.empresa.platform.messaging.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PlatformDefaultMessageProvider")
class PlatformDefaultMessageProviderTest {

    private final PlatformDefaultMessageProvider provider =
            new PlatformDefaultMessageProvider();

    @Test
    void deveFornecerMensagensGlobaisSemBanco() {
        var message = provider.find(
                PlatformMessageKeys.VALIDATION_FAILED,
                Locale.forLanguageTag("pt-BR")
        );

        assertThat(message).isPresent();
        assertThat(message.orElseThrow().code()).isEqualTo("GLOBAL-0001");
        assertThat(message.orElseThrow().httpStatus()).isEqualTo(400);
        assertThat(message.orElseThrow().message())
                .isEqualTo("Um ou mais campos informados são inválidos. Verifique os detalhes.");
    }

    @Test
    void deveFornecerMensagensBaseDoCrud() {
        var ptBr = provider.find(
                "validation.id.must-be-absent",
                Locale.forLanguageTag("pt-BR")
        );

        var en = provider.find(
                "validation.id.required",
                Locale.ENGLISH
        );

        assertThat(ptBr).isPresent();
        assertThat(ptBr.orElseThrow().code()).isEqualTo("VALIDATION-0003");

        assertThat(en).isPresent();
        assertThat(en.orElseThrow().message()).isEqualTo("The identifier is required.");
        assertThat(en.orElseThrow().httpStatus()).isEqualTo(400);
    }

    @Test
    void naoDeveFazerFallbackInternoParaOutroBundle() {
        assertThat(provider.find(
                "validation.id.required",
                Locale.forLanguageTag("fr-FR")
        )).isEmpty();

        assertThat(provider.find(
                "validation.id.required",
                Locale.forLanguageTag("en-US")
        )).isEmpty();
    }

    @Test
    void deveIgnorarMensagemQueNaoPertenceAoProvider() {
        assertThat(provider.find(
                "account.name.invalid",
                Locale.forLanguageTag("pt-BR")
        )).isEmpty();
    }

    @Test
    void deveFalharNaInicializacaoQuandoExistirChaveDuplicadaNoMesmoLocale() throws Exception {
        PathMatchingResourcePatternResolver resolver =
                mock(PathMatchingResourcePatternResolver.class);

        Resource audit = new NamedByteArrayResource(
                "audit_pt_BR.properties",
                "event.not.found=AUDIT-404|404|Evento não encontrado.|Verifique o evento."
        );

        Resource crud = new NamedByteArrayResource(
                "crud_pt_BR.properties",
                "event.not.found=CRUD-404|404|Registro não encontrado.|Verifique o registro."
        );

        when(resolver.getResources(anyString()))
                .thenReturn(new Resource[]{audit, crud});

        assertThatThrownBy(() ->
                new PlatformDefaultMessageProvider(
                        resolver,
                        new ApiMessageDefinitionParser()
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate platform message key detected")
                .hasMessageContaining("event.not.found")
                .hasMessageContaining("pt_BR")
                .hasMessageContaining("audit_pt_BR.properties")
                .hasMessageContaining("crud_pt_BR.properties");
    }

    @Test
    void devePermitirMesmaChaveEmLocalesDiferentes() throws Exception {
        PathMatchingResourcePatternResolver resolver =
                mock(PathMatchingResourcePatternResolver.class);

        Resource ptBr = new NamedByteArrayResource(
                "audit_pt_BR.properties",
                "audit.event.not.found=AUDIT-404|404|Evento não encontrado.|Verifique o evento."
        );

        Resource en = new NamedByteArrayResource(
                "audit_en.properties",
                "audit.event.not.found=AUDIT-404|404|Event not found.|Check the event."
        );

        when(resolver.getResources(anyString()))
                .thenReturn(new Resource[]{ptBr, en});

        PlatformDefaultMessageProvider customProvider =
                new PlatformDefaultMessageProvider(
                        resolver,
                        new ApiMessageDefinitionParser()
                );

        assertThat(customProvider.find(
                "audit.event.not.found",
                Locale.forLanguageTag("pt-BR")
        )).isPresent();

        assertThat(customProvider.find(
                "audit.event.not.found",
                Locale.ENGLISH
        )).isPresent();
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(String filename, String content) {
            super(content.getBytes(StandardCharsets.UTF_8));
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
