package br.com.portalmanager.platform.library.objectstorage.api.contract;

import br.com.portalmanager.platform.library.objectstorage.api.contract.request.DownloadUrlRequest;
import br.com.portalmanager.platform.library.objectstorage.api.contract.request.UploadUrlRequest;
import br.com.portalmanager.platform.library.objectstorage.api.contract.response.SignedUrl;

/** Creates temporary, provider-neutral URLs for direct object upload and download. */
public interface SignedUrlClient {
    SignedUrl createUploadUrl(UploadUrlRequest request);

    SignedUrl createDownloadUrl(DownloadUrlRequest request);
}
