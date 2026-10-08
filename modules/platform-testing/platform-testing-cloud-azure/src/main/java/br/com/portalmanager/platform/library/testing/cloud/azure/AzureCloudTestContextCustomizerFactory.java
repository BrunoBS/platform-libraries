package br.com.portalmanager.platform.library.testing.cloud.azure;

import br.com.portalmanager.platform.library.testing.context.AnnotationContextCustomizerFactory;
import br.com.portalmanager.platform.library.testing.cloud.azure.annotation.WithAzureEmulator;

public final class AzureCloudTestContextCustomizerFactory extends AnnotationContextCustomizerFactory {
    public AzureCloudTestContextCustomizerFactory() {
        super(WithAzureEmulator.class);
    }
}
