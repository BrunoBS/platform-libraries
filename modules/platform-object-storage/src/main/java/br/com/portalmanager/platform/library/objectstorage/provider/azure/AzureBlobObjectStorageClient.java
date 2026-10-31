package br.com.portalmanager.platform.library.objectstorage.provider.azure;

import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.contract.*;
import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageException;
import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageException.Reason;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.models.BlobParallelUploadOptions;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobStorageException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.azure.core.util.Context;

public class AzureBlobObjectStorageClient implements ObjectStorageClient {
    private final BlobServiceClient client;
    private final ObjectStorageProperties properties;

    public AzureBlobObjectStorageClient(BlobServiceClient client, ObjectStorageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public UploadResult upload(UploadRequest request, InputStream content) {
        Resolved resolved = resolve(request.destination(), request.key());
        BlobClient blob = container(resolved.container()).getBlobClient(resolved.key());
        BlobParallelUploadOptions options = new BlobParallelUploadOptions(content, request.contentLength())
                .setMetadata(request.metadata());
        if (request.contentType() != null) {
            options.setHeaders(new com.azure.storage.blob.models.BlobHttpHeaders().setContentType(request.contentType()));
        }
        if (request.writeMode() == WriteMode.CREATE_ONLY) {
            options.setRequestConditions(new BlobRequestConditions().setIfNoneMatch("*"));
        }
        try {
            blob.uploadWithResponse(options, Duration.ofMinutes(5), Context.NONE);
            BlobProperties result = blob.getProperties();
            return new UploadResult(new ObjectReference(request.destination(), request.key(), result.getVersionId()),
                    result.getETag(), result.getBlobSize());
        } catch (BlobStorageException exception) { throw translate(exception, "upload object"); }
    }

    @Override
    public DownloadedObject download(ObjectReference reference) {
        Resolved resolved = resolve(reference.destination(), reference.key());
        BlobClient blob = blob(resolved, reference.versionId());
        try {
            com.azure.storage.blob.specialized.BlobInputStream stream = blob.openInputStream();
            BlobProperties result = stream.getProperties();
            return new DownloadedObject(metadata(reference, result), stream);
        } catch (BlobStorageException exception) { throw translate(exception, "download object"); }
    }

    @Override
    public ObjectMetadata head(ObjectReference reference) {
        Resolved resolved = resolve(reference.destination(), reference.key());
        try { return metadata(reference, blob(resolved, reference.versionId()).getProperties()); }
        catch (BlobStorageException exception) { throw translate(exception, "read object properties"); }
    }

    @Override
    public boolean exists(ObjectReference reference) {
        try { return blob(resolve(reference.destination(), reference.key()), reference.versionId()).exists(); }
        catch (BlobStorageException exception) { throw translate(exception, "check object existence"); }
    }

    @Override
    public ObjectPage list(String destination, String prefix, String continuationToken, int pageSize) {
        requirePageSize(pageSize);
        ObjectStorageProperties.Destination target = destination(destination);
        String fullPrefix = join(target.getPrefix(), prefix == null ? "" : prefix);
        ListBlobsOptions options = new ListBlobsOptions().setPrefix(fullPrefix);
        PagedIterable<BlobItem> items = container(target.getContainer()).listBlobs(options, Duration.ofMinutes(2));
        Iterator<com.azure.core.http.rest.PagedResponse<BlobItem>> pages = items.iterableByPage(continuationToken, pageSize).iterator();
        if (!pages.hasNext()) return new ObjectPage(List.of(), null);
        com.azure.core.http.rest.PagedResponse<BlobItem> page = pages.next();
        List<ObjectSummary> result = new ArrayList<>();
        for (BlobItem item : page.getValue()) {
            String relative = stripPrefix(target.getPrefix(), item.getName());
            com.azure.storage.blob.models.BlobItemProperties itemProperties = item.getProperties();
            result.add(new ObjectSummary(ObjectReference.current(destination, relative),
                    itemProperties.getContentLength() == null ? 0L : itemProperties.getContentLength(),
                    itemProperties.getETag(),
                    itemProperties.getLastModified() == null ? null : itemProperties.getLastModified().toInstant()));
        }
        return new ObjectPage(result, page.getContinuationToken());
    }

    @Override
    public VersionPage listVersions(String destination, String key, String continuationToken, int pageSize) {
        requirePageSize(pageSize);
        Resolved resolved = resolve(destination, key);
        BlobListDetails details = new BlobListDetails().setRetrieveVersions(true);
        ListBlobsOptions options = new ListBlobsOptions().setPrefix(resolved.key()).setDetails(details);
        PagedIterable<BlobItem> items = container(resolved.container()).listBlobs(options, Duration.ofMinutes(2));
        Iterator<com.azure.core.http.rest.PagedResponse<BlobItem>> pages = items.iterableByPage(continuationToken, pageSize).iterator();
        if (!pages.hasNext()) return new VersionPage(List.of(), null);
        com.azure.core.http.rest.PagedResponse<BlobItem> page = pages.next();
        List<ObjectVersion> versions = new ArrayList<>();
        for (BlobItem item : page.getValue()) {
            if (!item.getName().equals(resolved.key())) continue;
            com.azure.storage.blob.models.BlobItemProperties itemProperties = item.getProperties();
            versions.add(new ObjectVersion(new ObjectReference(destination, key, item.getVersionId()), itemProperties.getETag(),
                    itemProperties.getContentLength() == null ? 0L : itemProperties.getContentLength(),
                    itemProperties.getLastModified() == null ? null : itemProperties.getLastModified().toInstant(),
                    Boolean.TRUE.equals(item.isLatestVersion()), Boolean.TRUE.equals(item.isDeleted())));
        }
        return new VersionPage(versions, page.getContinuationToken());
    }

    @Override
    public Optional<ObjectVersion> findVersionByETag(String destination, String key, String eTag) {
        String token = null;
        do {
            VersionPage page = listVersions(destination, key, token, 5000);
            Optional<ObjectVersion> found = page.versions().stream().filter(version -> sameETag(version.eTag(), eTag)).findFirst();
            if (found.isPresent()) return found;
            token = page.continuationToken();
        } while (token != null);
        return Optional.empty();
    }

    @Override
    public void delete(ObjectReference reference) {
        Resolved resolved = resolve(reference.destination(), reference.key());
        try { blob(resolved, null).delete(); }
        catch (BlobStorageException exception) { throw translate(exception, "delete object"); }
    }

    @Override
    public void deleteVersion(ObjectReference reference) {
        if (reference.versionId() == null) throw new IllegalArgumentException("versionId is required to delete a specific version");
        Resolved resolved = resolve(reference.destination(), reference.key());
        try { container(resolved.container()).getBlobVersionClient(resolved.key(), reference.versionId()).delete(); }
        catch (BlobStorageException exception) { throw translate(exception, "delete object version"); }
    }

    private ObjectMetadata metadata(ObjectReference requested, BlobProperties result) {
        ObjectReference actual = requested.versionId() == null && result.getVersionId() != null
                ? new ObjectReference(requested.destination(), requested.key(), result.getVersionId()) : requested;
        return new ObjectMetadata(actual, result.getBlobSize(), result.getContentType(), result.getETag(),
                result.getLastModified() == null ? null : result.getLastModified().toInstant(),
                result.getMetadata() == null ? Map.of() : result.getMetadata());
    }
    private BlobClient blob(Resolved resolved, String version) {
        BlobContainerClient container = container(resolved.container());
        return version == null ? container.getBlobClient(resolved.key()) : container.getBlobVersionClient(resolved.key(), version);
    }
    private BlobContainerClient container(String container) { return client.getBlobContainerClient(container); }
    private Resolved resolve(String logical, String key) {
        ObjectStorageProperties.Destination destination = destination(logical);
        return new Resolved(destination.getContainer(), join(destination.getPrefix(), key));
    }
    private ObjectStorageProperties.Destination destination(String logical) {
        ObjectStorageProperties.Destination target = properties.getDestinations().get(logical);
        if (target == null || target.getContainer() == null || target.getContainer().isBlank())
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
        if (pageSize < 1 || pageSize > 5000) throw new IllegalArgumentException("pageSize must be between 1 and 5000");
    }
    private static boolean sameETag(String first, String second) {
        return first != null && second != null && first.replace("\"", "").equals(second.replace("\"", ""));
    }
    private static ObjectStorageException translate(BlobStorageException exception, String operation) {
        Reason reason = switch (exception.getStatusCode()) {
            case 404 -> Reason.NOT_FOUND;
            case 403 -> Reason.ACCESS_DENIED;
            case 409, 412 -> Reason.ALREADY_EXISTS;
            case 408, 429, 500, 502, 503, 504 -> Reason.TRANSIENT;
            default -> Reason.PROVIDER_FAILURE;
        };
        return new ObjectStorageException(reason, "Unable to " + operation + " in Azure Blob Storage", exception);
    }
    private record Resolved(String container, String key) { }
}
