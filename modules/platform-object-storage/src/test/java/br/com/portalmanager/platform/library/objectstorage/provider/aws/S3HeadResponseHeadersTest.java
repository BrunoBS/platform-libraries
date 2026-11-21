package br.com.portalmanager.platform.library.objectstorage.provider.aws;

import br.com.portalmanager.platform.library.objectstorage.api.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectReference;
import br.com.portalmanager.platform.library.objectstorage.api.contract.response.ObjectMetadata;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3HeadResponseHeadersTest {
    @Test void headExposesRawResponseHeadersWithoutDownloadingObject() {
        S3Client sdk = mock(S3Client.class);
        ObjectStorageProperties properties = new ObjectStorageProperties();
        ObjectStorageProperties.Destination destination = new ObjectStorageProperties.Destination();
        destination.setBucket("test-bucket");
        properties.getDestinations().put("audit", destination);
        HeadObjectResponse.Builder response = HeadObjectResponse.builder().contentLength(15L).versionId("v1");
        response.sdkHttpResponse(SdkHttpResponse.builder().statusCode(200)
                .putHeader("x-amz-version-id", "v1").putHeader("custom", java.util.List.of("first", "second")).build());
        when(sdk.headObject(any(HeadObjectRequest.class))).thenReturn(response.build());
        S3ObjectStorageClient client = new S3ObjectStorageClient(sdk, mock(S3Presigner.class), properties);
        ObjectMetadata metadata = client.head(new ObjectReference("audit", "snapshot.json", "v1"));
        assertThat(metadata.responseHeaders().get("custom")).containsExactly("first", "second");
        assertThatThrownBy(() -> metadata.responseHeaders().put("other", java.util.List.of("value")))
                .isInstanceOf(UnsupportedOperationException.class);
        verify(sdk).headObject(HeadObjectRequest.builder().bucket("test-bucket").key("snapshot.json").versionId("v1").build());
        verifyNoMoreInteractions(sdk);
    }
}
