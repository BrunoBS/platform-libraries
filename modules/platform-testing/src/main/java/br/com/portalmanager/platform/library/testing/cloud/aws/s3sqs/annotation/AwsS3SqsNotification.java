package br.com.portalmanager.platform.library.testing.cloud.aws.s3sqs.annotation;

import br.com.portalmanager.platform.library.testing.cloud.aws.annotation.WithAwsLocalStack;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an S3 bucket notification that sends object-created events to an SQS queue.
 * This annotation is nested under {@link WithAwsLocalStack} and does not apply to a test class directly.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AwsS3SqsNotification {

    String bucket();

    String queue();
}
