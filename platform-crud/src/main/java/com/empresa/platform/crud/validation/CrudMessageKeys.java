package com.empresa.platform.crud.validation;

public final class CrudMessageKeys {

    private static final String PREFIX = "crud.";

    public static final String REQUIRED = PREFIX + "validation.required";
    public static final String ID_REQUIRED = PREFIX + "validation.id.required";
    public static final String ID_MUST_BE_ABSENT = PREFIX + "validation.id.must-be-absent";

    private CrudMessageKeys() {
    }
}
