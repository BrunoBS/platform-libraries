package br.com.portalmanager.platform.library.objectstorage.provider.aws;

import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.contract.*;
import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageException;
import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageException.Reason;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

public class S3ObjectStorageClient implements ObjectStorageClient {
    private final S3Client client;
    private final ObjectStorageProperties properties;

    public S3ObjectStorageClient(S3Client client, ObjectStorageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public UploadResult upload(UploadRequest request, InputStream content) {
        Resolved resolved = resolve(request.destination(), request.key());
        try {
            PutObjectRequest.Builder builder = PutObjectRequest.builder().bucket(resolved.bucket()).key(resolved.key())
                    .contentLength(request.contentLength()).metadata(request.metadata());
            if (request.contentType() != null) builder.contentType(request.contentType());
            if (request.writeMode() == WriteMode.CREATE_ONLY) builder.ifNoneMatch("*");
            PutObjectResponse response = client.putObject(builder.build(), RequestBody.fromInputStream(content, request.contentLength()));
            String version = response.versionId();
            return new UploadResult(new ObjectReference(request.destination(), request.key(), version), response.eTag(), request.contentLength());
        } catch (S3Exception exception) {
            throw translate(exception, "upload object");
        }
    }

    @Override
    public DownloadedObject download(ObjectReference reference) {
        Resolved resolved = resolve(reference.destination(), reference.key());
        try {
            GetObjectRequest.Builder builder = GetObjectRequest.builder().bucket(resolved.bucket()).key(resolved.key());
            if (reference.versionId() != null) builder.versionId(reference.versionId());
            software.amazon.awssdk.core.ResponseInputStream<GetObjectResponse> response = client.getObject(builder.build());
            ObjectMetadata metadata = new ObjectMetadata(reference, response.response().contentLength(), response.response().contentType(),
                    response.response().eTag(), response.response().lastModified(), response.response().metadata());
            return new DownloadedObject(metadata, response);
        } catch (S3Exception exception) {
            throw translate(exception, "download object");
        }
    }

    @Override
    public ObjectMetadata head(ObjectReference reference) {
        Resolved resolved = resolve(reference.destination(), reference.key());
        try {
            HeadObjectRequest.Builder builder = HeadObjectRequest.builder().bucket(resolved.bucket()).key(resolved.key());
            if (reference.versionId() != null) builder.versionId(reference.versionId());
            HeadObjectResponse response = client.headObject(builder.build());
            ObjectReference actual = reference.versionId() == null && response.versionId() != null
                    ? new ObjectReference(reference.destination(), reference.key(), response.versionId()) : reference;
            return new ObjectMetadata(actual, response.contentLength(), response.contentType(), response.eTag(), response.lastModified(), response.metadata());
        } catch (S3Exception exception) {
            throw translate(exception, "read object properties");
        }
    }

    @Override
    public boolean exists(ObjectReference reference) {
        try { head(reference); return true; }
        catch (ObjectStorageException exception) {
            if (exception.getReason() == Reason.NOT_FOUND) return false;
            throw exception;
        }
    }

    @Override
    public ObjectPage list(String destination, String prefix, String continuationToken, int pageSize) {
        requirePageSize(pageSize);
        ObjectStorageProperties.Destination target = destination(destination);
        String fullPrefix = join(target.getPrefix(), prefix == null ? "" : prefix);
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder().bucket(target.getBucket())
                .prefix(fullPrefix).maxKeys(pageSize);
        if (continuationToken != null) builder.continuationToken(continuationToken);
        try {
            ListObjectsV2Response response = client.listObjectsV2(builder.build());
            List<ObjectSummary> objects = new ArrayList<>();
            for (S3Object item : response.contents()) {
                String relative = stripPrefix(target.getPrefix(), item.key());
                ObjectReference reference = ObjectReference.current(destination, relative);
                objects.add(new ObjectSummary(reference, item.size(), item.eTag(), item.lastModified()));
            }
            return new ObjectPage(objects, response.nextContinuationToken());
        } catch (S3Exception exception) { throw translate(exception, "list objects"); }
    }

    @Override
    public VersionPage listVersions(String destination, String key, String continuationToken, int pageSize) {
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
    }

    @Override
    public Optional<ObjectVersion> findVersionByETag(String destination, String key, String eTag) {
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
        Resolved resolved = resolve(reference.destination(), reference.key());
        try { client.deleteObject(DeleteObjectRequest.builder().bucket(resolved.bucket()).key(resolved.key()).build()); }
        catch (S3Exception exception) { throw translate(exception, "delete object"); }
    }

    @Override
    public void deleteVersion(ObjectReference reference) {
        if (reference.versionId() == null) throw new IllegalArgumentException("versionId is required to delete a specific version");
        Resolved resolved = resolve(reference.destination(), reference.key());
        try { client.deleteObject(DeleteObjectRequest.builder().bucket(resolved.bucket()).key(resolved.key()).versionId(reference.versionId()).build()); }
        catch (S3Exception exception) { throw translate(exception, "delete object version"); }
    }

    private Resolved resolve(String logical, String key) {
        ObjectStorageProperties.Destination target = destination(logical);
        return new Resolved(target.getBucket(), join(target.getPrefix(), key));
    }

    private ObjectStorageProperties.Destination destination(String logical) {
        ObjectStorageProperties.Destination target = properties.getDestinations().get(logical);
        if (target == null || target.getBucket() == null || target.getBucket().isBlank())
            throw new IllegalArgumentException("Unknown or unconfigured object storage destination: " + logical);
        return target;
    }

    private static String join(String prefix, String key) {
        if (prefix == null || prefix.isBlank()) return key;
        return key == null || key.isBlank() ? prefix : prefix.replaceAll("/$", "") + "/" + key.replaceAll("^/", "");
    }
    private static String stripPrefix(String prefix, String key) {
        if (prefix == null || prefix.isBlank()) return key;
        String normalized = prefix.replaceAll("/$", "") + "/";
        return key.startsWith(normalized) ? key.substring(normalized.length()) : key;
    }
    private static void requirePageSize(int pageSize) {
        if (pageSize < 1 || pageSize > 1000) throw new IllegalArgumentException("pageSize must be between 1 and 1000");
    }
    private static String encodeToken(String key, String version) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((key + "\n" + version).getBytes(StandardCharsets.UTF_8));
    }
    private static String[] decodeToken(String token) {
        String[] parts = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8).split("\n", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid version continuation token");
        return parts;
    }
    private static boolean sameETag(String first, String second) {
        return first != null && second != null && first.replace("\"", "").equals(second.replace("\"", ""));
    }
    private static ObjectStorageException translate(S3Exception exception, String operation) {
        Reason reason = switch (exception.statusCode()) {
            case 404 -> Reason.NOT_FOUND;
            case 403 -> Reason.ACCESS_DENIED;
            case 409, 412 -> Reason.ALREADY_EXISTS;
            case 429, 500, 502, 503, 504 -> Reason.TRANSIENT;
            default -> Reason.PROVIDER_FAILURE;
        };
        return new ObjectStorageException(reason, "Unable to " + operation + " in S3", exception);
    }
    private record Resolved(String bucket, String key) { }
}
