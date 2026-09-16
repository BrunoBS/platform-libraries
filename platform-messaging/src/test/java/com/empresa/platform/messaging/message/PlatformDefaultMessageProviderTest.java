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
    void shouldResolveMessageUsingBundleNameAsNamespace() {
        var message = provider.find(
                PlatformMessageKeys.VALIDATION_FAILED,
                Locale.forLanguageTag("pt-BR")
        );

        assertThat(message).isPresent();
        assertThat(message.orElseThrow().messageKey())
                .isEqualTo("platform.global.validation.failed");
        assertThat(message.orElseThrow().code()).isEqualTo("GLOBAL-0001");
        assertThat(message.orElseThrow().httpStatus()).isEqualTo(400);
    }

    @Test
    void shouldNotFallbackInternallyToAnotherLocale() {
        assertThat(provider.find(
                PlatformMessageKeys.VALIDATION_FAILED,
                Locale.forLanguageTag("fr-FR")
        )).isEmpty();

        assertThat(provider.find(
                PlatformMessageKeys.VALIDATION_FAILED,
                Locale.forLanguageTag("en-US")
        )).isEmpty();
    }

    @Test
    void shouldAllowSameLocalKeyForDifferentServices() throws Exception {
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
                "crud.event.not.found",
                Locale.forLanguageTag("pt-BR")
        )).isPresent();
    }

    @Test
    void shouldFailStartupWhenSameGlobalKeyExistsTwice() throws Exception {
        PathMatchingResourcePatternResolver resolver =
                mock(PathMatchingResourcePatternResolver.class);

        Resource first = new NamedByteArrayResource(
                "catalog_pt_BR.properties",
                "not.found=CAT-404|404|Catálogo não encontrado.|Verifique o identificador."
        );

        Resource second = new NamedByteArrayResource(
                "catalog_pt_BR.properties",
                "not.found=CAT-404-2|404|Outro catálogo.|Verifique."
        );

        when(resolver.getResources(anyString()))
                .thenReturn(new Resource[]{first, second});

        assertThatThrownBy(() ->
                new PlatformDefaultMessageProvider(
                        resolver,
                        new ApiMessageDefinitionParser()
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate platform message key detected")
                .hasMessageContaining("catalog.not.found")
                .hasMessageContaining("pt_BR");
    }

    @Test
    void shouldRejectAlreadyPrefixedLocalKey() throws Exception {
        PathMatchingResourcePatternResolver resolver =
                mock(PathMatchingResourcePatternResolver.class);

        Resource catalog = new NamedByteArrayResource(
                "catalog_pt_BR.properties",
                "catalog.not.found=CAT-404|404|Catálogo não encontrado.|Verifique."
        );

        when(resolver.getResources(anyString()))
                .thenReturn(new Resource[]{catalog});

        assertThatThrownBy(() ->
                new PlatformDefaultMessageProvider(
                        resolver,
                        new ApiMessageDefinitionParser()
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be local to its bundle")
                .hasMessageContaining("catalog.not.found");
    }

    @Test
    void shouldRejectInvalidBundleName() throws Exception {
        PathMatchingResourcePatternResolver resolver =
                mock(PathMatchingResourcePatternResolver.class);

        Resource invalid = new NamedByteArrayResource(
                "catalog.properties",
                "not.found=CAT-404|404|Catálogo não encontrado.|Verifique."
        );

        when(resolver.getResources(anyString()))
                .thenReturn(new Resource[]{invalid});

        assertThatThrownBy(() ->
                new PlatformDefaultMessageProvider(
                        resolver,
                        new ApiMessageDefinitionParser()
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid platform message bundle name");
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
