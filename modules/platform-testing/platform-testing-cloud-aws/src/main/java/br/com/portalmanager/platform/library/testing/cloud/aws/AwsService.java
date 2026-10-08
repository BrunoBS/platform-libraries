package br.com.portalmanager.platform.library.testing.cloud.aws;

public enum AwsService {
    SQS("sqs"),
    S3("s3"),
    SECRETS_MANAGER("secretsmanager"),
    SNS("sns"),
    EVENT_BRIDGE("events");

    private final String localStackName;

    AwsService(String localStackName) {
        this.localStackName = localStackName;
    }

    public String localStackName() {
        return localStackName;
    }
}
