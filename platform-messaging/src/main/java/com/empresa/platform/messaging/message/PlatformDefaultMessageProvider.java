package com.empresa.platform.messaging.message;

import com.empresa.platform.messaging.model.ApiMessage;
import com.empresa.platform.messaging.provider.ApiMessageProvider;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class PlatformDefaultMessageProvider implements ApiMessageProvider {

    private static final String PT_BR = "pt-BR";
    private static final String EN = "en";

    private static final Map<String, MessageDefinition> PT_BR_MESSAGES = Map.ofEntries(
            Map.entry(PlatformMessageKeys.VALIDATION_FAILED,
                    new MessageDefinition("GLOBAL-0001", "Um ou mais campos informados são inválidos. Verifique os detalhes.", "Corrija os campos indicados e tente novamente.", 400)),
            Map.entry(PlatformMessageKeys.REQUEST_NOT_READABLE,
                    new MessageDefinition("GLOBAL-0002", "Não foi possível ler o corpo da requisição.", "Verifique o JSON enviado.", 400)),
            Map.entry(PlatformMessageKeys.REQUEST_FORMAT_INVALID,
                    new MessageDefinition("GLOBAL-0003", "O formato de um ou mais campos é incompatível com o esperado.", "Revise os tipos e formatos enviados.", 400)),
            Map.entry(PlatformMessageKeys.DATA_INTEGRITY,
                    new MessageDefinition("GLOBAL-0004", "A operação não pôde ser concluída devido a uma violação de integridade de dados.", "Revise os dados relacionados.", 409)),
            Map.entry(PlatformMessageKeys.RESOURCE_NOT_FOUND,
                    new MessageDefinition("GLOBAL-0005", "O recurso solicitado não foi encontrado.", "Verifique o identificador informado.", 404)),
            Map.entry(PlatformMessageKeys.INTERNAL_SERVER_ERROR,
                    new MessageDefinition("GLOBAL-0006", "Ocorreu um erro interno inesperado. ID do Erro: {0}.", "Informe o ID do erro ao suporte caso o problema persista.", 500)),
            Map.entry(PlatformMessageKeys.TYPE_MISMATCH,
                    new MessageDefinition("GLOBAL-0007", "O campo {0} deve ser do tipo {1}.", "Corrija o tipo do campo informado.", 400)),
            Map.entry(PlatformMessageKeys.INVALID_ENUM,
                    new MessageDefinition("GLOBAL-0008", "O valor informado para o campo {0} é inválido. Valores permitidos: {1}.", "Informe um dos valores permitidos.", 400)),
            Map.entry("validation.required",
                    new MessageDefinition("VALIDATION-0001", "O recurso informado é obrigatório.", "Informe os dados obrigatórios.", 400)),
            Map.entry("validation.id.required",
                    new MessageDefinition("VALIDATION-0002", "O identificador é obrigatório.", "Informe um identificador válido.", 400)),
            Map.entry("validation.id.must-be-absent",
                    new MessageDefinition("VALIDATION-0003", "O identificador não deve ser informado na criação.", "Remova o campo id do corpo da requisição.", 400))
    );

    private static final Map<String, MessageDefinition> EN_MESSAGES = Map.ofEntries(
            Map.entry(PlatformMessageKeys.VALIDATION_FAILED,
                    new MessageDefinition("GLOBAL-0001", "One or more fields are invalid.", "Correct the indicated fields and try again.", 400)),
            Map.entry(PlatformMessageKeys.REQUEST_NOT_READABLE,
                    new MessageDefinition("GLOBAL-0002", "The request body could not be read.", "Check the submitted JSON.", 400)),
            Map.entry(PlatformMessageKeys.REQUEST_FORMAT_INVALID,
                    new MessageDefinition("GLOBAL-0003", "One or more fields have an incompatible format.", "Review the submitted types and formats.", 400)),
            Map.entry(PlatformMessageKeys.DATA_INTEGRITY,
                    new MessageDefinition("GLOBAL-0004", "The operation could not be completed due to a data integrity violation.", "Review the related data.", 409)),
            Map.entry(PlatformMessageKeys.RESOURCE_NOT_FOUND,
                    new MessageDefinition("GLOBAL-0005", "The requested resource was not found.", "Check the provided identifier.", 404)),
            Map.entry(PlatformMessageKeys.INTERNAL_SERVER_ERROR,
                    new MessageDefinition("GLOBAL-0006", "An unexpected internal error occurred. Error ID: {0}.", "Provide the error ID to support if the issue persists.", 500)),
            Map.entry(PlatformMessageKeys.TYPE_MISMATCH,
                    new MessageDefinition("GLOBAL-0007", "Field {0} must be of type {1}.", "Correct the field type.", 400)),
            Map.entry(PlatformMessageKeys.INVALID_ENUM,
                    new MessageDefinition("GLOBAL-0008", "The value for field {0} is invalid. Allowed values: {1}.", "Provide one of the allowed values.", 400)),
            Map.entry("validation.required",
                    new MessageDefinition("VALIDATION-0001", "The resource is required.", "Provide the required data.", 400)),
            Map.entry("validation.id.required",
                    new MessageDefinition("VALIDATION-0002", "The identifier is required.", "Provide a valid identifier.", 400)),
            Map.entry("validation.id.must-be-absent",
                    new MessageDefinition("VALIDATION-0003", "The identifier must not be provided on create.", "Remove the id field from the request body.", 400))
    );

    @Override
    public Optional<ApiMessage> find(String key, Locale locale) {
        String languageTag = locale == null ? PT_BR : locale.toLanguageTag();
        boolean english = languageTag.equalsIgnoreCase(EN)
                || languageTag.toLowerCase(Locale.ROOT).startsWith("en-");

        MessageDefinition definition = (english ? EN_MESSAGES : PT_BR_MESSAGES).get(key);
        if (definition == null) {
            return Optional.empty();
        }

        return Optional.of(new ApiMessage(
                definition.code(),
                key,
                english ? EN : PT_BR,
                definition.message(),
                definition.solution(),
                definition.httpStatus()
        ));
    }

    private record MessageDefinition(
            String code,
            String message,
            String solution,
            int httpStatus
    ) {
    }
}
