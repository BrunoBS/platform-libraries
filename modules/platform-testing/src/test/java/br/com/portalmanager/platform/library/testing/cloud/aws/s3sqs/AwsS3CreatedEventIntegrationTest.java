package br.com.portalmanager.platform.library.testing.cloud.aws.s3sqs;

import br.com.portalmanager.platform.library.testing.cloud.aws.AwsLocalStackConnection;
import br.com.portalmanager.platform.library.testing.cloud.aws.s3.annotation.AwsS3;
import br.com.portalmanager.platform.library.testing.cloud.aws.s3sqs.annotation.AwsS3SqsNotification;
import br.com.portalmanager.platform.library.testing.cloud.aws.sqs.annotation.AwsSqs;
import br.com.portalmanager.platform.library.testing.cloud.aws.annotation.WithAwsLocalStack;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(AwsS3CreatedEventIntegrationTest.AwsLocalStackConfiguration.class)
class AwsS3CreatedEventIntegrationTest {

    private static final String QUEUE = "s3-created-events";
    private static final String BUCKET = "platform-testing-s3-events";
    private static final String CONTENT = "audit-document-123";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private S3Client s3Client;

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private AwsLocalStackConnection connection;

    @Test
    void shouldPublishS3CreatedEventAndAllowReadingTheUploadedObject() throws Exception {
        String queueUrl = sqsClient.getQueueUrl(request -> request.queueName(QUEUE)).queueUrl();
        String objectKey = "audit-123.json";

        s3Client.putObject(
                PutObjectRequest.builder().bucket(BUCKET).key(objectKey).build(),
                RequestBody.fromString(CONTENT));

        JsonNode record = receiveCreatedRecord(queueUrl);
        assertNotNull(record, "Expected an S3 ObjectCreated event in SQS");
        assertTrue(record.path("eventName").asText().startsWith("ObjectCreated:"));
        assertEquals(BUCKET, record.path("s3").path("bucket").path("name").asText());

        String eventKey = URLDecoder.decode(
                record.path("s3").path("object").path("key").asText().replace("+", "%2B"),
                StandardCharsets.UTF_8);
        assertEquals(objectKey, eventKey);

        String uploadedContent = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(record.path("s3").path("bucket").path("name").asText())
                        .key(eventKey)
                        .build())
                .asUtf8String();
        assertEquals(CONTENT, uploadedContent);
        assertNotNull(connection.endpoint());
    }

    private JsonNode receiveCreatedRecord(String queueUrl) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (System.nanoTime() < deadline) {
            var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .waitTimeSeconds(5)
                    .maxNumberOfMessages(10)
                    .build());
            for (var message : response.messages()) {
                JsonNode records = JSON.readTree(message.body()).path("Records");
                if (records.isArray() && records.size() > 0) {
                    return records.get(0);
                }
                // S3 sends a one-time test event when the bucket notification is configured.
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        }
        return null;
    }

    @Configuration(proxyBeanMethods = false)
    @WithAwsLocalStack(
            sqs = @AwsSqs(queues = @AwsSqs.Queue(name = QUEUE)),
            s3 = @AwsS3(buckets = BUCKET),
            s3SqsNotifications = @AwsS3SqsNotification(bucket = BUCKET, queue = QUEUE)
    )
    static class AwsLocalStackConfiguration {
    }
}
