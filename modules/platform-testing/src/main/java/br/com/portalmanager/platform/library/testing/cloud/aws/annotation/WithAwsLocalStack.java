package br.com.portalmanager.platform.library.testing.cloud.aws.annotation;

import br.com.portalmanager.platform.library.testing.cloud.aws.s3.annotation.AwsS3;
import br.com.portalmanager.platform.library.testing.cloud.aws.s3notificationsqs.annotation.AwsS3SqsNotification;
import br.com.portalmanager.platform.library.testing.cloud.aws.sqs.annotation.AwsSqs;
import br.com.portalmanager.platform.library.testing.cloud.aws.AwsLocalStackImportRegistrar;
import br.com.portalmanager.platform.library.testing.container.PlatformTestingContainerImages;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(AwsLocalStackImportRegistrar.class)
public @interface WithAwsLocalStack {

    String image() default PlatformTestingContainerImages.AWS_LOCALSTACK;

    AwsSqs[] sqs() default {};

    AwsS3[] s3() default {};

    AwsS3SqsNotification[] s3SqsNotifications() default {};
}
