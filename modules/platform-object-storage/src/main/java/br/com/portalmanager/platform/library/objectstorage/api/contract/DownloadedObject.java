package br.com.portalmanager.platform.library.objectstorage.api.contract;

import br.com.portalmanager.platform.library.objectstorage.internal.validation.ObjectStorageValidation;
import java.io.InputStream;
import java.io.IOException;

/** Caller owns and must close the returned content stream. */
public record DownloadedObject(ObjectMetadata metadata, InputStream content) implements AutoCloseable {
    public DownloadedObject {
        if (metadata == null) throw ObjectStorageValidation.invalid("metadata");
        if (content == null) throw ObjectStorageValidation.invalid("content");
    }

    @Override
    public void close() throws IOException {
        content.close();
    }
}
