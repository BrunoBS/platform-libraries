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

    public static final PlatformErrorDefinition SYSTEM_TAGS_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-TAG-002",
                    "TagOwner.getSystemTags() must return a non-null collection.",
                    "Return an empty collection when no system tags apply.",
                    500
            );

    public static final PlatformErrorDefinition FACTORY_RESULT_INVALID =
            new PlatformErrorDefinition(
                    "PLT-TAG-003",
                    "TagFactory returned a null tag.",
                    "Return a tag entity for every requested tag name.",
                    500
            );

    public static final PlatformErrorDefinition PERSISTED_STATE_INVALID =
            new PlatformErrorDefinition(
                    "PLT-TAG-004",
                    "Persisted tagging state is invalid.",
                    "Inspect the owner's tag rows and correct missing required values.",
                    500
            );

    public static final PlatformErrorDefinition PERSISTED_NAME_INVALID =
            new PlatformErrorDefinition(
                    "PLT-TAG-005",
                    "A persisted tag name is invalid after normalization.",
                    "Correct the stored tag name so it normalizes to a non-empty value within the supported length.",
                    500
            );

    private TaggingTechnicalErrors() {
    }
}
