package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.testing.context.AnnotationContextCustomizerFactory;
import br.com.portalmanager.platform.library.testing.cloud.aws.annotation.WithAwsLocalStack;

public final class AwsCloudTestContextCustomizerFactory extends AnnotationContextCustomizerFactory {
    public AwsCloudTestContextCustomizerFactory() {
        super(WithAwsLocalStack.class);
    }
}
