package br.com.portalmanager.platform.library.testing.cloud.aws.internal.configuration;

import br.com.portalmanager.platform.library.testing.core.internal.context.AnnotationContextCustomizerFactory;
import br.com.portalmanager.platform.library.testing.cloud.aws.api.annotation.WithAwsLocalStack;

public final class AwsCloudTestContextCustomizerFactory extends AnnotationContextCustomizerFactory {
    public AwsCloudTestContextCustomizerFactory() {
        super(WithAwsLocalStack.class);
    }
}
