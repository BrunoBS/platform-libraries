package br.com.portalmanager.platform.tagging.validation;

import br.com.portalmanager.platform.messaging.exception.ValidationException;
import br.com.portalmanager.platform.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.messaging.model.ValidationDetail;
import br.com.portalmanager.platform.tagging.message.TaggingMessageKeys;
import br.com.portalmanager.platform.tagging.model.TagOriginType;
import br.com.portalmanager.platform.tagging.model.TagOwnerType;

import java.util.List;
import java.util.Locale;

public final class TagValidation {

    private TagValidation() {
    }

    public static String requireId(String id) {
        return requireText(id, "id", TaggingMessageKeys.ID_REQUIRED);
    }

    public static String requireOwnerType(TagOwnerType ownerType) {
        if (ownerType == null) {
            throw required("ownerType", TaggingMessageKeys.OWNER_TYPE_REQUIRED);
        }
        return requireOwnerType(ownerType.value());
    }

    public static String requireOwnerType(String ownerType) {
        return requireText(ownerType, "ownerType", TaggingMessageKeys.OWNER_TYPE_REQUIRED)
                .toUpperCase(Locale.ROOT);
    }

    public static String requireOwnerId(Object ownerId) {
        if (ownerId == null) {
            throw required("ownerId", TaggingMessageKeys.OWNER_ID_REQUIRED);
        }
        return requireText(
                String.valueOf(ownerId),
                "ownerId",
                TaggingMessageKeys.OWNER_ID_REQUIRED
        );
    }

    public static String requireName(String name) {
        return requireText(name, "name", TaggingMessageKeys.NAME_REQUIRED);
    }

    public static TagOriginType requireOrigin(TagOriginType originType) {
        if (originType == null) {
            throw required("originType", TaggingMessageKeys.ORIGIN_REQUIRED);
        }
        return originType;
    }

    private static String requireText(String value, String field, String messageKey) {
        if (value == null || value.isBlank()) {
            throw required(field, messageKey);
        }
        return value.trim();
    }

    private static ValidationException required(String field, String messageKey) {
        return new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                List.of(new ValidationDetail(field, messageKey))
        );
    }
}
