package br.com.portalmanager.platform.library.objectstorage.contract;

import java.io.InputStream;
import java.io.IOException;
import java.util.Objects;

/** Caller owns and must close the returned content stream. */
public record DownloadedObject(ObjectMetadata metadata, InputStream content) implements AutoCloseable {
    public DownloadedObject {
        Objects.requireNonNull(metadata, "metadata is required");
        Objects.requireNonNull(content, "content is required");
    }

    @Override
    public void close() throws IOException {
        content.close();
    }
}
