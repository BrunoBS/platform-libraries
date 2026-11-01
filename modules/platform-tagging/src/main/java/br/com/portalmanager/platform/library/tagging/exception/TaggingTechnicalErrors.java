package br.com.portalmanager.platform.library.tagging.exception;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class TaggingTechnicalErrors {

    public static final PlatformErrorDefinition MANAGER_DEPENDENCIES_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-TAG-001",
                    "TagManager requires a repository and a tag factory.",
                    "Register TagManager with non-null repository and factory beans.",
                    500
            );

    public static final String SYSTEM_TAGS_REQUIRED = "tagging.error.system-tags.required";
    public static final String FACTORY_RESULT_INVALID = "tagging.error.factory-result.invalid";
    public static final String PERSISTED_STATE_INVALID = "tagging.error.persisted-state.invalid";
    public static final String PERSISTED_NAME_INVALID = "tagging.error.persisted-name.invalid";

    private TaggingTechnicalErrors() {
    }
}
