package br.com.portalmanager.platform.library.schemavalidation.exception;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

/** Indicates that neither a resource schema nor its configured fallback is available. */
public final class ResourceSchemaNotFoundException extends PlatformConfigurationException {

    public ResourceSchemaNotFoundException(PlatformErrorDefinition definition) {
        super(definition);
    }
}
