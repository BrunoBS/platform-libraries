package com.empresa.platform.catalog.message;

import com.empresa.platform.messaging.model.ApiMessage;
import com.empresa.platform.messaging.provider.ApiMessageProvider;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CatalogMessageProvider implements ApiMessageProvider {

    private static final String PT_BR = "pt-BR";
    private static final String EN = "en";

    private static final Map<String, MessageDefinition> PT_BR_MESSAGES = Map.ofEntries(
            Map.entry(CatalogMessageKeys.NOT_FOUND,
                    new MessageDefinition("CAT-404-001", "Catálogo não encontrado.", "Verifique o identificador informado.", 404)),
            Map.entry(CatalogMessageKeys.RESTORE_INVALID,
                    new MessageDefinition("CAT-400-001", "Não foi possível restaurar o catálogo.", "Verifique se o registro existe e está inativo.", 400)),
            Map.entry(CatalogMessageKeys.REQUIRED,
                    new MessageDefinition("CAT-400-002", "Catálogo obrigatório.", "Informe os dados do catálogo.", 400)),
            Map.entry(CatalogMessageKeys.NAME_REQUIRED,
                    new MessageDefinition("CAT-400-003", "Nome obrigatório.", "Informe o nome do catálogo.", 400)),
            Map.entry(CatalogMessageKeys.NAME_DUPLICATE,
                    new MessageDefinition("CAT-409-001", "Já existe um catálogo com este nome.", "Informe um nome diferente.", 409)),
            Map.entry(CatalogMessageKeys.NAME_NOT_ALLOWED,
                    new MessageDefinition("CAT-400-004", "Nome de catálogo inválido.", "Informe um dos valores permitidos para este catálogo.", 400)),
            Map.entry(CatalogMessageKeys.NAME_IMMUTABLE,
                    new MessageDefinition("CAT-409-002", "O nome do catálogo é imutável.", "Mantenha o nome original do catálogo.", 409)),
            Map.entry(CatalogMessageKeys.LABEL_REQUIRED,
                    new MessageDefinition("CAT-400-005", "Label obrigatório.", "Informe o label do catálogo.", 400)),
            Map.entry(CatalogMessageKeys.DESCRIPTION_REQUIRED,
                    new MessageDefinition("CAT-400-006", "Descrição obrigatória.", "Informe a descrição do catálogo.", 400)),
            Map.entry(CatalogMessageKeys.DESCRIPTION_INVALID_LENGTH,
                    new MessageDefinition("CAT-400-007", "Descrição com tamanho inválido.", "Informe uma descrição dentro do tamanho permitido.", 400))
    );

    private static final Map<String, MessageDefinition> EN_MESSAGES = Map.ofEntries(
            Map.entry(CatalogMessageKeys.NOT_FOUND,
                    new MessageDefinition("CAT-404-001", "Catalog was not found.", "Check the provided identifier.", 404)),
            Map.entry(CatalogMessageKeys.RESTORE_INVALID,
                    new MessageDefinition("CAT-400-001", "Catalog could not be restored.", "Check whether the record exists and is inactive.", 400)),
            Map.entry(CatalogMessageKeys.REQUIRED,
                    new MessageDefinition("CAT-400-002", "Catalog is required.", "Provide the catalog data.", 400)),
            Map.entry(CatalogMessageKeys.NAME_REQUIRED,
                    new MessageDefinition("CAT-400-003", "Name is required.", "Provide the catalog name.", 400)),
            Map.entry(CatalogMessageKeys.NAME_DUPLICATE,
                    new MessageDefinition("CAT-409-001", "A catalog with this name already exists.", "Provide a different name.", 409)),
            Map.entry(CatalogMessageKeys.NAME_NOT_ALLOWED,
                    new MessageDefinition("CAT-400-004", "Catalog name is invalid.", "Provide one of the allowed values for this catalog.", 400)),
            Map.entry(CatalogMessageKeys.NAME_IMMUTABLE,
                    new MessageDefinition("CAT-409-002", "Catalog name is immutable.", "Keep the original catalog name.", 409)),
            Map.entry(CatalogMessageKeys.LABEL_REQUIRED,
                    new MessageDefinition("CAT-400-005", "Label is required.", "Provide the catalog label.", 400)),
            Map.entry(CatalogMessageKeys.DESCRIPTION_REQUIRED,
                    new MessageDefinition("CAT-400-006", "Description is required.", "Provide the catalog description.", 400)),
            Map.entry(CatalogMessageKeys.DESCRIPTION_INVALID_LENGTH,
                    new MessageDefinition("CAT-400-007", "Description length is invalid.", "Provide a description within the allowed length.", 400))
    );

    @Override
    public Optional<ApiMessage> find(String key, Locale locale) {
        String languageTag = locale == null ? PT_BR : locale.toLanguageTag();
        boolean english = languageTag.equalsIgnoreCase(EN)
                || languageTag.toLowerCase(Locale.ROOT).startsWith("en-");
        String resolvedLocale = english ? EN : PT_BR;
        MessageDefinition definition = (english ? EN_MESSAGES : PT_BR_MESSAGES).get(key);

        if (definition == null) {
            return Optional.empty();
        }

        return Optional.of(new ApiMessage(
                definition.code(),
                key,
                resolvedLocale,
                definition.message(),
                definition.solution(),
                definition.httpStatus()
        ));
    }

    private record MessageDefinition(String code, String message, String solution, int httpStatus) {
    }
}
