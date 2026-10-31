package br.com.portalmanager.platform.library.objectstorage.provider.azure;

import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.contract.DownloadedObject;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectHeaders;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectMetadata;
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
import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageFailureMapper;
import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;
import br.com.portalmanager.platform.library.objectstorage.internal.ObjectStorageKey;
import br.com.portalmanager.platform.library.objectstorage.internal.ObjectStorageUploadContent;
import com.azure.core.exception.AzureException;
import com.azure.core.exception.ClientAuthenticationException;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import java.net.URI;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.azure.core.util.Context;

public class AzureBlobObjectStorageClient implements ObjectStorageClient, SignedUrlClient {
    private final BlobServiceClient client;
    private final ObjectStorageProperties properties;

    public AzureBlobObjectStorageClient(BlobServiceClient client, ObjectStorageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public UploadResult upload(UploadRequest request, InputStream content) {
        if (request == null) throw ObjectStorageValidation.invalid("request");
        if (content == null) throw ObjectStorageValidation.invalid("content");
        Resolved resolved = resolve(request.destination(), request.key());
        BlobClient blob = container(resolved.container()).getBlobClient(resolved.key());
        ObjectStorageUploadContent uploadContent = ObjectStorageUploadContent.from(content);
        BlobParallelUploadOptions options = new BlobParallelUploadOptions(uploadContent.stream(), request.contentLength())
                .setMetadata(request.metadata());
        ObjectHeaders headers = request.headers();
        options.setHeaders(new com.azure.storage.blob.models.BlobHttpHeaders()
                .setContentType(headers.contentType())
                .setCacheControl(headers.cacheControl())
                .setContentDisposition(headers.contentDisposition())
                .setContentEncoding(headers.contentEncoding())
                .setContentLanguage(headers.contentLanguage()));
        if (request.writeMode() == WriteMode.CREATE_ONLY) {
            options.setRequestConditions(new BlobRequestConditions().setIfNoneMatch("*"));
        }
        try {
            BlockBlobItem result = blob.uploadWithResponse(options, Duration.ofMinutes(5), Context.NONE).getValue();
            return new UploadResult(new ObjectReference(request.destination(), request.key(), result.getVersionId()),
                    result.getETag(), request.contentLength(), uploadContent.sha256());
        } catch (BlobStorageException exception) { throw translate(exception, "upload object"); }
        catch (ClientAuthenticationException exception) { throw ObjectStorageFailureMapper.fromHttpStatus(401, "upload object", exception); }
        catch (AzureException exception) { throw ObjectStorageFailureMapper.fromProviderFailure("upload object", exception); }
    }

    @Override
    public DownloadedObject download(ObjectReference reference) {
        ObjectStorageValidation.requireReference(reference);
        Resolved resolved = resolve(reference.destination(), reference.key());
        BlobClient blob = blob(resolved, reference.versionId());
        try {
            com.azure.storage.blob.specialized.BlobInputStream stream = blob.openInputStream();
            BlobProperties result = stream.getProperties();
            return new DownloadedObject(metadata(reference, result), stream);
        } catch (BlobStorageException exception) { throw translate(exception, "download object"); }
        catch (ClientAuthenticationException exception) { throw ObjectStorageFailureMapper.fromHttpStatus(401, "download object", exception); }
        catch (AzureException exception) { throw ObjectStorageFailureMapper.fromProviderFailure("download object", exception); }
    }

    @Override
    public ObjectMetadata head(ObjectReference reference) {
        ObjectStorageValidation.requireReference(reference);
        Resolved resolved = resolve(reference.destination(), reference.key());
        try { return metadata(reference, blob(resolved, reference.versionId()).getProperties()); }
        catch (BlobStorageException exception) { throw translate(exception, "read object properties"); }
        catch (ClientAuthenticationException exception) { throw ObjectStorageFailureMapper.fromHttpStatus(401, "read object properties", exception); }
        catch (AzureException exception) { throw ObjectStorageFailureMapper.fromProviderFailure("read object properties", exception); }
    }

    @Override
    public boolean exists(ObjectReference reference) {
        ObjectStorageValidation.requireReference(reference);
        try { return blob(resolve(reference.destination(), reference.key()), reference.versionId()).exists(); }
        catch (BlobStorageException exception) {
            if (exception.getStatusCode() == 404) return false;
            throw translate(exception, "check object existence");
        }
        catch (ClientAuthenticationException exception) { throw ObjectStorageFailureMapper.fromHttpStatus(401, "check object existence", exception); }
        catch (AzureException exception) { throw ObjectStorageFailureMapper.fromProviderFailure("check object existence", exception); }
    }

    @Override
    public ObjectPage list(String destination, String prefix, String continuationToken, int pageSize) {
        ObjectStorageValidation.requireText(destination, "destination");
        requirePageSize(pageSize);
        ObjectStorageProperties.Destination target = destination(destination);
        String fullPrefix = ObjectStorageKey.resolvePrefix(target.getPrefix(), prefix);
        try {
            ListBlobsOptions options = new ListBlobsOptions().setPrefix(fullPrefix);
            PagedIterable<BlobItem> items = container(target.getContainer()).listBlobs(options, Duration.ofMinutes(2));
            Iterator<com.azure.core.http.rest.PagedResponse<BlobItem>> pages = items.iterableByPage(continuationToken, pageSize).iterator();
            if (!pages.hasNext()) return new ObjectPage(List.of(), null);
            com.azure.core.http.rest.PagedResponse<BlobItem> page = pages.next();
            List<ObjectSummary> result = new ArrayList<>();
            for (BlobItem item : page.getValue()) {
                String relative = ObjectStorageKey.logical(target.getPrefix(), target.getSuffix(), item.getName());
                if (relative == null) continue;
                com.azure.storage.blob.models.BlobItemProperties itemProperties = item.getProperties();
                result.add(new ObjectSummary(ObjectReference.current(destination, relative),
                        itemProperties.getContentLength() == null ? 0L : itemProperties.getContentLength(),
                        itemProperties.getETag(),
                        itemProperties.getLastModified() == null ? null : itemProperties.getLastModified().toInstant()));
            }
            return new ObjectPage(result, page.getContinuationToken());
        } catch (BlobStorageException exception) {
            throw translate(exception, "list objects");
        } catch (ClientAuthenticationException exception) {
            throw ObjectStorageFailureMapper.fromHttpStatus(401, "list objects", exception);
        } catch (AzureException exception) {
            throw ObjectStorageFailureMapper.fromProviderFailure("list objects", exception);
        }
    }

    @Override
    public VersionPage listVersions(String destination, String key, String continuationToken, int pageSize) {
        ObjectStorageValidation.requireText(destination, "destination");
        ObjectStorageValidation.requireText(key, "key");
        requirePageSize(pageSize);
        Resolved resolved = resolve(destination, key);
        try {
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
                        Boolean.TRUE.equals(item.isCurrentVersion()), Boolean.TRUE.equals(item.isDeleted())));
            }
            return new VersionPage(versions, page.getContinuationToken());
        } catch (BlobStorageException exception) {
            throw translate(exception, "list object versions");
        } catch (ClientAuthenticationException exception) {
            throw ObjectStorageFailureMapper.fromHttpStatus(401, "list object versions", exception);
        } catch (AzureException exception) {
            throw ObjectStorageFailureMapper.fromProviderFailure("list object versions", exception);
        }
    }

    @Override
    public Optional<ObjectVersion> findVersionByETag(String destination, String key, String eTag) {
        ObjectStorageValidation.requireText(eTag, "eTag");
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
        ObjectStorageValidation.requireReference(reference);
        Resolved resolved = resolve(reference.destination(), reference.key());
        try { blob(resolved, null).delete(); }
        catch (BlobStorageException exception) { throw translate(exception, "delete object"); }
        catch (ClientAuthenticationException exception) { throw ObjectStorageFailureMapper.fromHttpStatus(401, "delete object", exception); }
        catch (AzureException exception) { throw ObjectStorageFailureMapper.fromProviderFailure("delete object", exception); }
    }

    @Override
    public void deleteVersion(ObjectReference reference) {
        ObjectStorageValidation.requireReference(reference);
        if (reference.versionId() == null) throw ObjectStorageValidation.invalid("versionId");
        Resolved resolved = resolve(reference.destination(), reference.key());
        try { container(resolved.container()).getBlobVersionClient(resolved.key(), reference.versionId()).delete(); }
        catch (BlobStorageException exception) { throw translate(exception, "delete object version"); }
        catch (ClientAuthenticationException exception) { throw ObjectStorageFailureMapper.fromHttpStatus(401, "delete object version", exception); }
        catch (AzureException exception) { throw ObjectStorageFailureMapper.fromProviderFailure("delete object version", exception); }
    }

    @Override
    public SignedUrl createDownloadUrl(DownloadUrlRequest request) {
        if (request == null) throw ObjectStorageValidation.invalid("request");
        Resolved resolved = resolve(request.reference().destination(), request.reference().key());
        return createSignedUrl(resolved, request.reference().versionId(), request.validity(),
                new BlobSasPermission().setReadPermission(true), Map.of());
    }

    @Override
    public SignedUrl createUploadUrl(UploadUrlRequest request) {
        if (request == null) throw ObjectStorageValidation.invalid("request");
        Resolved resolved = resolve(request.reference().destination(), request.reference().key());
        return createSignedUrl(resolved, null, request.validity(),
                new BlobSasPermission().setCreatePermission(true).setWritePermission(true),
                request.headers().asHttpHeaders());
    }

    private SignedUrl createSignedUrl(Resolved resolved, String versionId, Duration validity,
            BlobSasPermission permissions, Map<String, String> requiredHeaders) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startsAt = now.minusMinutes(5);
        OffsetDateTime expiresAt = now.plus(validity);
        BlobClient target = blob(resolved, versionId);
        String blobUrl = target.getBlobUrl();
        SasProtocol protocol = blobUrl.startsWith("https://")
                ? SasProtocol.HTTPS_ONLY : SasProtocol.HTTPS_HTTP;
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiresAt, permissions)
                .setStartTime(startsAt)
                .setProtocol(protocol);
        if (!requiredHeaders.isEmpty()) values.setRequestHeaders(requiredHeaders);
        try {
            UserDelegationKey delegationKey = client.getUserDelegationKey(startsAt, expiresAt.plusMinutes(5));
            String sasToken = target.generateUserDelegationSas(values, delegationKey);
            String separator = blobUrl.contains("?") ? "&" : "?";
            URI url = URI.create(blobUrl + separator + sasToken);
            return new SignedUrl(url, expiresAt.toInstant(), requiredHeaders);
        } catch (BlobStorageException exception) {
            throw translate(exception, "create signed URL");
        } catch (ClientAuthenticationException exception) {
            throw ObjectStorageFailureMapper.fromHttpStatus(401, "create signed URL", exception);
        } catch (AzureException exception) {
            throw ObjectStorageFailureMapper.fromProviderFailure("create signed URL", exception);
        } catch (IllegalArgumentException exception) {
            throw ObjectStorageFailureMapper.fromClientFailure("create signed URL", exception);
        }
    }

    private ObjectMetadata metadata(ObjectReference requested, BlobProperties result) {
        ObjectReference actual = requested.versionId() == null && result.getVersionId() != null
                ? new ObjectReference(requested.destination(), requested.key(), result.getVersionId()) : requested;
        ObjectHeaders headers = new ObjectHeaders(result.getContentType(), result.getCacheControl(),
                result.getContentDisposition(), result.getContentEncoding(), result.getContentLanguage());
        return new ObjectMetadata(actual, result.getBlobSize(), headers, result.getETag(),
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
        return new Resolved(destination.getContainer(), ObjectStorageKey.resolve(destination.getPrefix(), destination.getSuffix(), key));
    }
    private ObjectStorageProperties.Destination destination(String logical) {
        ObjectStorageProperties.Destination target = properties.getDestinations().get(logical);
        if (target == null || target.getContainer() == null || target.getContainer().isBlank())
            throw ObjectStorageValidation.invalid("destination");
        return target;
    }
    private static void requirePageSize(int pageSize) {
        if (pageSize < 1 || pageSize > 5000) throw ObjectStorageValidation.invalid("pageSize");
    }
    private static boolean sameETag(String first, String second) {
        return first != null && second != null && first.replace("\"", "").equals(second.replace("\"", ""));
    }
    private static RuntimeException translate(BlobStorageException exception, String operation) {
        return ObjectStorageFailureMapper.fromHttpStatus(exception.getStatusCode(), operation, exception);
    }
    private record Resolved(String container, String key) { }
}
