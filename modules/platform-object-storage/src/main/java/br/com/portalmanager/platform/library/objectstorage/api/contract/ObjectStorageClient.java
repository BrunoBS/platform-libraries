package br.com.portalmanager.platform.library.objectstorage.api.contract;

import java.io.InputStream;
import java.util.Optional;

/** Provider-neutral data-plane API. Destination names are logical application names. */
public interface ObjectStorageClient {
    /** Maximum number of objects or versions returned by one listing call. */
    int MAX_PAGE_SIZE = 1000;

    /**
     * Uploads the supplied content synchronously. The caller retains ownership of {@code content} and must close it,
     * including when this method fails. The stream is consumed during this call but is not closed by this method.
     * The request's content length must exactly match the number of bytes available from the stream.
     */
    UploadResult upload(UploadRequest request, InputStream content);

    /** Opens a download and returns its live stream; the caller must close the returned object. */
    DownloadedObject download(ObjectReference reference);

    ObjectMetadata head(ObjectReference reference);

    /**
     * Returns whether the referenced object or exact version exists. A provider-identified missing object or version
     * is {@code false}; access, destination, and unclassified provider failures are reported as exceptions.
     */
    boolean exists(ObjectReference reference);

    /** Returns one page. Reuse its continuation token with the same destination and prefix. */
    ObjectPage list(String destination, String prefix, String continuationToken, int pageSize);

    default ObjectPage list(String destination, String prefix, String continuationToken) {
        return list(destination, prefix, continuationToken, MAX_PAGE_SIZE);
    }

    default ObjectPage list(String destination, String prefix) {
        return list(destination, prefix, null, MAX_PAGE_SIZE);
    }

    /** Returns one page of versions. Reuse its continuation token with the same destination and key. */
    VersionPage listVersions(String destination, String key, String continuationToken, int pageSize);

    default VersionPage listVersions(String destination, String key, String continuationToken) {
        return listVersions(destination, key, continuationToken, MAX_PAGE_SIZE);
    }

    default VersionPage listVersions(String destination, String key) {
        return listVersions(destination, key, null, MAX_PAGE_SIZE);
    }

    /**
     * Returns the first matching version in provider listing order. This may read every version page,
     * and an ETag can be shared by multiple versions; use a versionId when an exact version is known.
     */
    Optional<ObjectVersion> findVersionByETag(String destination, String key, String eTag);

    /**
     * Deletes the resource identified by the reference. Without a versionId, this removes the current object
     * from normal reads and listings; with a versionId, it deletes that exact version. Provider version
     * history and retention behavior remain provider-specific.
     */
    void delete(ObjectReference reference);

    /** Requests deletion of the exact version identified by the reference's versionId. */
    void deleteVersion(ObjectReference reference);
}
