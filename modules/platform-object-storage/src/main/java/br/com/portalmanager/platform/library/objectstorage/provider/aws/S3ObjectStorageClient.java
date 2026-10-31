package br.com.portalmanager.platform.library.objectstorage.provider.aws;

import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.contract.DownloadedObject;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectMetadata;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectHeaders;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectPage;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectReference;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectSummary;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectVersion;
import br.com.portalmanager.platform.library.objectstorage.contract.DownloadUrlRequest;
import br.com.portalmanager.platform.library.objectstorage.contract.SignedUrl;
import br.com.portalmanager.platform.library.objectstorage.contract.SignedUrlClient;
import br.com.portalmanager.platform.library.objectstorage.contract.UploadUrlRequest;
import br.com.portalmanager.platform.library.objectstorage.contract.UploadRequest;
import br.com.portalmanager.platform.library.objectstorage.contract.UploadResult;
import br.com.portalmanager.platform.library.objectstorage.contract.VersionPage;
import br.com.portalmanager.platform.library.objectstorage.contract.WriteMode;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageFailureMapper;
import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;
import br.com.portalmanager.platform.library.objectstorage.internal.ObjectStorageKey;
import br.com.portalmanager.platform.library.objectstorage.internal.ObjectStorageUploadContent;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.*;

public class S3ObjectStorageClient implements ObjectStorageClient, SignedUrlClient {
    private final S3Client client;
    private final S3Presigner presigner;
    private final ObjectStorageProperties properties;

    public S3ObjectStorageClient(S3Client client, S3Presigner presigner, ObjectStorageProperties properties) {
        this.client = client;
        this.presigner = presigner;
        this.properties = properties;
    }

    @Override
    public UploadResult upload(UploadRequest request, InputStream content) {
        if (request == null) throw ObjectStorageValidation.invalid("request");
        if (content == null) throw ObjectStorageValidation.invalid("content");
        Resolved resolved = resolve(request.destination(), request.key());
        ObjectStorageUploadContent uploadContent = ObjectStorageUploadContent.from(content);
        try {
            ObjectHeaders headers = request.headers();
            PutObjectRequest.Builder builder = PutObjectRequest.builder().bucket(resolved.bucket()).key(resolved.key())
                    .contentLength(request.contentLength()).metadata(request.metadata())
                    .contentType(headers.contentType()).cacheControl(headers.cacheControl())
                    .contentDisposition(headers.contentDisposition()).contentEncoding(headers.contentEncoding())
                    .contentLanguage(headers.contentLanguage());
            if (request.writeMode() == WriteMode.CREATE_ONLY) builder.ifNoneMatch("*");
            PutObjectResponse response = client.putObject(builder.build(),
                    RequestBody.fromInputStream(uploadContent.stream(), request.contentLength()));
            String version = response.versionId();
            return new UploadResult(new ObjectReference(request.destination(), request.key(), version), response.eTag(),
                    request.contentLength(), uploadContent.sha256());
        } catch (S3Exception exception) {
            throw translate(exception, "upload object");
        } catch (SdkClientException exception) {
            throw ObjectStorageFailureMapper.fromClientFailure("upload object", exception);
        } catch (SdkException exception) {
            throw ObjectStorageFailureMapper.fromProviderFailure("upload object", exception);
        }
    }

    @Override
    public DownloadedObject download(ObjectReference reference) {
        ObjectStorageValidation.requireReference(reference);
        Resolved resolved = resolve(reference.destination(), reference.key());
        try {
            GetObjectRequest.Builder builder = GetObjectRequest.builder().bucket(resolved.bucket()).key(resolved.key());
            if (reference.versionId() != null) builder.versionId(reference.versionId());
            software.amazon.awssdk.core.ResponseInputStream<GetObjectResponse> response = client.getObject(builder.build());
            GetObjectResponse result = response.response();
            ObjectReference actual = reference.versionId() == null && result.versionId() != null
                    ? new ObjectReference(reference.destination(), reference.key(), result.versionId()) : reference;
            ObjectMetadata metadata = new ObjectMetadata(actual, result.contentLength(), headers(result.contentType(),
                    result.cacheControl(), result.contentDisposition(), result.contentEncoding(), result.contentLanguage()),
                    result.eTag(), result.lastModified(), result.metadata());
            return new DownloadedObject(metadata, response);
        } catch (S3Exception exception) {
            throw translate(exception, "download object");
        } catch (SdkClientException exception) {
            throw ObjectStorageFailureMapper.fromClientFailure("download object", exception);
        } catch (SdkException exception) {
            throw ObjectStorageFailureMapper.fromProviderFailure("download object", exception);
        }
    }

    @Override
    public ObjectMetadata head(ObjectReference reference) {
        ObjectStorageValidation.requireReference(reference);
        Resolved resolved = resolve(reference.destination(), reference.key());
        try {
            HeadObjectRequest.Builder builder = HeadObjectRequest.builder().bucket(resolved.bucket()).key(resolved.key());
            if (reference.versionId() != null) builder.versionId(reference.versionId());
            HeadObjectResponse response = client.headObject(builder.build());
            ObjectReference actual = reference.versionId() == null && response.versionId() != null
                    ? new ObjectReference(reference.destination(), reference.key(), response.versionId()) : reference;
            return new ObjectMetadata(actual, response.contentLength(), headers(response.contentType(), response.cacheControl(),
                    response.contentDisposition(), response.contentEncoding(), response.contentLanguage()), response.eTag(),
                    response.lastModified(), response.metadata());
        } catch (S3Exception exception) {
            throw translate(exception, "read object properties");
        } catch (SdkClientException exception) {
            throw ObjectStorageFailureMapper.fromClientFailure("read object properties", exception);
        } catch (SdkException exception) {
            throw ObjectStorageFailureMapper.fromProviderFailure("read object properties", exception);
        }
    }

    @Override
    public boolean exists(ObjectReference reference) {
        ObjectStorageValidation.requireReference(reference);
        try { head(reference); return true; }
        catch (NotFoundException exception) { return false; }
    }

    @Override
    public ObjectPage list(String destination, String prefix, String continuationToken, int pageSize) {
        ObjectStorageValidation.requireText(destination, "destination");
        requirePageSize(pageSize);
        ObjectStorageProperties.Destination target = destination(destination);
        String fullPrefix = ObjectStorageKey.resolvePrefix(target.getPrefix(), prefix);
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder().bucket(target.getBucket())
                .prefix(fullPrefix).maxKeys(pageSize);
        if (continuationToken != null) builder.continuationToken(continuationToken);
        try {
            ListObjectsV2Response response = client.listObjectsV2(builder.build());
            List<ObjectSummary> objects = new ArrayList<>();
            for (S3Object item : response.contents()) {
                String relative = ObjectStorageKey.logical(target.getPrefix(), target.getSuffix(), item.key());
                if (relative == null) continue;
                ObjectReference reference = ObjectReference.current(destination, relative);
                objects.add(new ObjectSummary(reference, item.size(), item.eTag(), item.lastModified()));
            }
            return new ObjectPage(objects, response.nextContinuationToken());
        } catch (S3Exception exception) { throw translate(exception, "list objects"); }
        catch (SdkClientException exception) { throw ObjectStorageFailureMapper.fromClientFailure("list objects", exception); }
        catch (SdkException exception) { throw ObjectStorageFailureMapper.fromProviderFailure("list objects", exception); }
    }

    @Override
    public VersionPage listVersions(String destination, String key, String continuationToken, int pageSize) {
        ObjectStorageValidation.requireText(destination, "destination");
        ObjectStorageValidation.requireText(key, "key");
        requirePageSize(pageSize);
        Resolved resolved = resolve(destination, key);
        ListObjectVersionsRequest.Builder builder = ListObjectVersionsRequest.builder().bucket(resolved.bucket())
                .prefix(resolved.key()).maxKeys(pageSize);
        if (continuationToken != null) {
            String[] markers = decodeToken(continuationToken);
            builder.keyMarker(markers[0]).versionIdMarker(markers[1]);
        }
        try {
            ListObjectVersionsResponse response = client.listObjectVersions(builder.build());
            List<ObjectVersion> versions = new ArrayList<>();
            response.versions().stream().filter(item -> item.key().equals(resolved.key())).forEach(item ->
                    versions.add(new ObjectVersion(new ObjectReference(destination, key, item.versionId()), item.eTag(), item.size(),
                            item.lastModified(), item.isLatest(), false)));
            response.deleteMarkers().stream().filter(item -> item.key().equals(resolved.key())).forEach(item ->
                    versions.add(new ObjectVersion(new ObjectReference(destination, key, item.versionId()), null, 0,
                            item.lastModified(), item.isLatest(), true)));
            String next = response.isTruncated() ? encodeToken(response.nextKeyMarker(), response.nextVersionIdMarker()) : null;
            return new VersionPage(versions, next);
        } catch (S3Exception exception) { throw translate(exception, "list object versions"); }
        catch (SdkClientException exception) { throw ObjectStorageFailureMapper.fromClientFailure("list object versions", exception); }
        catch (SdkException exception) { throw ObjectStorageFailureMapper.fromProviderFailure("list object versions", exception); }
    }

    @Override
    public Optional<ObjectVersion> findVersionByETag(String destination, String key, String eTag) {
        ObjectStorageValidation.requireText(eTag, "eTag");
        String token = null;
        do {
            VersionPage page = listVersions(destination, key, token, 1000);
            Optional<ObjectVersion> found = page.versions().stream().filter(version -> sameETag(version.eTag(), eTag)).findFirst();
            if (found.isPresent()) return found;
            token = page.continuationToken();
        } while (token != null);
        return Optional.empty();
    }

    @Override
    public void delete(ObjectReference reference) {
        ObjectStorageValidation.requireReference(reference);
        Resolved resolved = resolve(reference.destination(), reference.key());
        try { client.deleteObject(DeleteObjectRequest.builder().bucket(resolved.bucket()).key(resolved.key()).build()); }
        catch (S3Exception exception) { throw translate(exception, "delete object"); }
        catch (SdkClientException exception) { throw ObjectStorageFailureMapper.fromClientFailure("delete object", exception); }
        catch (SdkException exception) { throw ObjectStorageFailureMapper.fromProviderFailure("delete object", exception); }
    }

    @Override
    public void deleteVersion(ObjectReference reference) {
        ObjectStorageValidation.requireReference(reference);
        if (reference.versionId() == null) throw ObjectStorageValidation.invalid("versionId");
        Resolved resolved = resolve(reference.destination(), reference.key());
        try { client.deleteObject(DeleteObjectRequest.builder().bucket(resolved.bucket()).key(resolved.key()).versionId(reference.versionId()).build()); }
        catch (S3Exception exception) { throw translate(exception, "delete object version"); }
        catch (SdkClientException exception) { throw ObjectStorageFailureMapper.fromClientFailure("delete object version", exception); }
        catch (SdkException exception) { throw ObjectStorageFailureMapper.fromProviderFailure("delete object version", exception); }
    }

    @Override
    public SignedUrl createDownloadUrl(DownloadUrlRequest request) {
        if (request == null) throw ObjectStorageValidation.invalid("request");
        Resolved resolved = resolve(request.reference().destination(), request.reference().key());
        try {
            GetObjectRequest.Builder objectRequest = GetObjectRequest.builder()
                    .bucket(resolved.bucket()).key(resolved.key());
            if (request.reference().versionId() != null) {
                objectRequest.versionId(request.reference().versionId());
            }
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(request.validity()).getObjectRequest(objectRequest.build()).build();
            PresignedGetObjectRequest signedRequest = presigner.presignGetObject(presignRequest);
            return new SignedUrl(signedRequest.url().toURI(), signedRequest.expiration(), Map.of());
        } catch (S3Exception exception) {
            throw translate(exception, "create signed download URL");
        } catch (SdkClientException exception) {
            throw ObjectStorageFailureMapper.fromClientFailure("create signed download URL", exception);
        } catch (SdkException exception) {
            throw ObjectStorageFailureMapper.fromProviderFailure("create signed download URL", exception);
        } catch (java.net.URISyntaxException exception) {
            throw ObjectStorageFailureMapper.fromProviderFailure("create signed download URL", exception);
        } catch (IllegalArgumentException exception) {
            throw ObjectStorageFailureMapper.fromClientFailure("create signed download URL", exception);
        }
    }

    @Override
    public SignedUrl createUploadUrl(UploadUrlRequest request) {
        if (request == null) throw ObjectStorageValidation.invalid("request");
        Resolved resolved = resolve(request.reference().destination(), request.reference().key());
        try {
            ObjectHeaders headers = request.headers();
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(resolved.bucket()).key(resolved.key())
                    .contentType(headers.contentType()).cacheControl(headers.cacheControl())
                    .contentDisposition(headers.contentDisposition()).contentEncoding(headers.contentEncoding())
                    .contentLanguage(headers.contentLanguage()).build();
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(request.validity()).putObjectRequest(objectRequest).build();
            PresignedPutObjectRequest signedRequest = presigner.presignPutObject(presignRequest);
            return new SignedUrl(signedRequest.url().toURI(), signedRequest.expiration(), headers.asHttpHeaders());
        } catch (S3Exception exception) {
            throw translate(exception, "create signed upload URL");
        } catch (SdkClientException exception) {
            throw ObjectStorageFailureMapper.fromClientFailure("create signed upload URL", exception);
        } catch (SdkException exception) {
            throw ObjectStorageFailureMapper.fromProviderFailure("create signed upload URL", exception);
        } catch (java.net.URISyntaxException exception) {
            throw ObjectStorageFailureMapper.fromProviderFailure("create signed upload URL", exception);
        } catch (IllegalArgumentException exception) {
            throw ObjectStorageFailureMapper.fromClientFailure("create signed upload URL", exception);
        }
    }

    private Resolved resolve(String logical, String key) {
        ObjectStorageProperties.Destination target = destination(logical);
        return new Resolved(target.getBucket(), ObjectStorageKey.resolve(target.getPrefix(), target.getSuffix(), key));
    }

    private ObjectStorageProperties.Destination destination(String logical) {
        ObjectStorageProperties.Destination target = properties.getDestinations().get(logical);
        if (target == null || target.getBucket() == null || target.getBucket().isBlank())
            throw ObjectStorageValidation.invalid("destination");
        return target;
    }

    private static ObjectHeaders headers(String contentType, String cacheControl, String contentDisposition,
            String contentEncoding, String contentLanguage) {
        return new ObjectHeaders(contentType, cacheControl, contentDisposition, contentEncoding, contentLanguage);
    }
    private static void requirePageSize(int pageSize) {
        if (pageSize < 1 || pageSize > 1000) throw ObjectStorageValidation.invalid("pageSize");
    }
    private static String encodeToken(String key, String version) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((key + "\n" + version).getBytes(StandardCharsets.UTF_8));
    }
    private static String[] decodeToken(String token) {
        byte[] decoded;
        try {
            decoded = Base64.getUrlDecoder().decode(token);
        } catch (IllegalArgumentException exception) {
            throw ObjectStorageValidation.invalid("continuationToken");
        }
        String[] parts = new String(decoded, StandardCharsets.UTF_8).split("\n", 2);
        if (parts.length != 2) throw ObjectStorageValidation.invalid("continuationToken");
        return parts;
    }
    private static boolean sameETag(String first, String second) {
        return first != null && second != null && first.replace("\"", "").equals(second.replace("\"", ""));
    }
    private static RuntimeException translate(S3Exception exception, String operation) {
        return ObjectStorageFailureMapper.fromHttpStatus(exception.statusCode(), operation, exception);
    }
    private record Resolved(String bucket, String key) { }
}
