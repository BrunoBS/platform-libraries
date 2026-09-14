package com.empresa.platform.authorization.message;

import com.empresa.platform.messaging.model.ApiMessage;
import com.empresa.platform.messaging.provider.ApiMessageProvider;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class AuthorizationMessageProvider implements ApiMessageProvider {

    private static final String PT_BR = "pt-BR";
    private static final String EN = "en";

    private static final Map<String, MessageDefinition> PT_BR_MESSAGES = Map.of(
            AuthorizationMessageKeys.CORRELATION_ID_MISSING,
            new MessageDefinition("AUTH-401-001", "Correlation ID não informado.", "Informe o cabeçalho X-Correlation-Id.", 401),
            AuthorizationMessageKeys.TOKEN_MISSING,
            new MessageDefinition("AUTH-401-002", "Token de autorização não informado.", "Informe um token Bearer válido.", 401),
            AuthorizationMessageKeys.SESSION_NOT_FOUND,
            new MessageDefinition("AUTH-401-003", "Sessão de usuário não encontrada.", "Autentique-se novamente e repita a operação.", 401),
            AuthorizationMessageKeys.GROUPS_NOT_FOUND,
            new MessageDefinition("AUTH-403-001", "Grupos de autorização não encontrados.", "Verifique os grupos associados ao usuário.", 403),
            AuthorizationMessageKeys.GROUP_MISSING,
            new MessageDefinition("AUTH-403-002", "Grupo de autorização obrigatório não encontrado.", "Solicite acesso ao grupo necessário para a operação.", 403),
            AuthorizationMessageKeys.OWNER_REQUIRED,
            new MessageDefinition("AUTH-403-003", "Permissão de OWNER é obrigatória.", "Solicite permissão de OWNER para executar esta operação.", 403),
            AuthorizationMessageKeys.RESOURCE_ACCESS_DENIED,
            new MessageDefinition("AUTH-403-004", "Acesso ao recurso não permitido.", "Verifique suas permissões para o recurso solicitado.", 403),
            AuthorizationMessageKeys.PLATFORM_ACCESS_DENIED,
            new MessageDefinition("AUTH-401-004", "Acesso não permitido", "Verifique suas credenciais e tente novamente.", 401)
    );

    private static final Map<String, MessageDefinition> EN_MESSAGES = Map.of(
            AuthorizationMessageKeys.CORRELATION_ID_MISSING,
            new MessageDefinition("AUTH-401-001", "Correlation ID was not provided.", "Provide the X-Correlation-Id header.", 401),
            AuthorizationMessageKeys.TOKEN_MISSING,
            new MessageDefinition("AUTH-401-002", "Authorization token was not provided.", "Provide a valid Bearer token.", 401),
            AuthorizationMessageKeys.SESSION_NOT_FOUND,
            new MessageDefinition("AUTH-401-003", "User session was not found.", "Authenticate again and retry the operation.", 401),
            AuthorizationMessageKeys.GROUPS_NOT_FOUND,
            new MessageDefinition("AUTH-403-001", "Authorization groups were not found.", "Check the groups assigned to the user.", 403),
            AuthorizationMessageKeys.GROUP_MISSING,
            new MessageDefinition("AUTH-403-002", "Required authorization group was not found.", "Request access to the group required for this operation.", 403),
            AuthorizationMessageKeys.OWNER_REQUIRED,
            new MessageDefinition("AUTH-403-003", "OWNER permission is required.", "Request OWNER permission to perform this operation.", 403),
            AuthorizationMessageKeys.RESOURCE_ACCESS_DENIED,
            new MessageDefinition("AUTH-403-004", "Access to the resource is not allowed.", "Check your permissions for the requested resource.", 403),
            AuthorizationMessageKeys.PLATFORM_ACCESS_DENIED,
            new MessageDefinition("AUTH-401-004", "Access denied", "Check your credentials and try again.", 401)
    );

    @Override
    public Optional<ApiMessage> find(String key, Locale locale) {
        String languageTag = locale == null ? PT_BR : locale.toLanguageTag();
        boolean english = languageTag.equalsIgnoreCase(EN) || languageTag.toLowerCase(Locale.ROOT).startsWith("en-");
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
