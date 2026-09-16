package com.empresa.platform.crud.validation;

import com.empresa.platform.messaging.exception.ValidationException;
import com.empresa.platform.messaging.message.PlatformMessageKeys;
import com.empresa.platform.messaging.model.ValidationDetail;

import java.util.List;

public class CrudValidationException extends ValidationException {

    public CrudValidationException(CrudValidationResult result) {
        super(
                PlatformMessageKeys.VALIDATION_FAILED,
                toValidationDetails(result)
        );
    }

    private static List<ValidationDetail> toValidationDetails(CrudValidationResult result) {
        if (result == null || result.getDetails().isEmpty()) {
            return List.of();
        }

        return result.getDetails()
                .stream()
                .map(CrudValidationException::toValidationDetail)
                .toList();
    }

    private static ValidationDetail toValidationDetail(CrudValidationDetail detail) {
        if (detail.messageKey() == null || detail.messageKey().isBlank()) {
            return ValidationDetail.literal(
                    detail.field(),
                    detail.defaultMessage()
            );
        }

        return new ValidationDetail(
                detail.field(),
                detail.messageKey(),
                detail.parameters()
        );
    }
}
