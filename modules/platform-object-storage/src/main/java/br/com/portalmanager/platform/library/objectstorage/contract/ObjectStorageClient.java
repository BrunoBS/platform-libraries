package br.com.portalmanager.platform.library.objectstorage.contract;

import java.io.InputStream;
import java.util.Optional;

/** Provider-neutral data-plane API. Destination names are logical application names. */
public interface ObjectStorageClient {
    UploadResult upload(UploadRequest request, InputStream content);

    DownloadedObject download(ObjectReference reference);

    ObjectMetadata head(ObjectReference reference);

    boolean exists(ObjectReference reference);

    ObjectPage list(String destination, String prefix, String continuationToken, int pageSize);

    VersionPage listVersions(String destination, String key, String continuationToken, int pageSize);

    Optional<ObjectVersion> findVersionByETag(String destination, String key, String eTag);

    void delete(ObjectReference reference);

    void deleteVersion(ObjectReference reference);

    SignedUrlResult createSignedUrl(SignedUrlRequest request);
}
