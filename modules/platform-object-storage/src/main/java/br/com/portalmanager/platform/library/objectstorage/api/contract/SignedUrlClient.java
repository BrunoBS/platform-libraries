package br.com.portalmanager.platform.library.objectstorage.api.contract;

/** Creates temporary, provider-neutral URLs for direct object upload and download. */
public interface SignedUrlClient {
    SignedUrl createUploadUrl(UploadUrlRequest request);

    SignedUrl createDownloadUrl(DownloadUrlRequest request);
}
