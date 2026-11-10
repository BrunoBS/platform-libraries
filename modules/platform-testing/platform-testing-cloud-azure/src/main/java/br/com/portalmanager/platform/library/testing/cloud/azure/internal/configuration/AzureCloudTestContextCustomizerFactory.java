package br.com.portalmanager.platform.library.testing.cloud.azure.internal.configuration;

import br.com.portalmanager.platform.library.testing.core.internal.context.AnnotationContextCustomizerFactory;
import br.com.portalmanager.platform.library.testing.cloud.azure.api.annotation.WithAzureEmulator;

public final class AzureCloudTestContextCustomizerFactory extends AnnotationContextCustomizerFactory {
    public AzureCloudTestContextCustomizerFactory() {
        super(WithAzureEmulator.class);
    }
}
