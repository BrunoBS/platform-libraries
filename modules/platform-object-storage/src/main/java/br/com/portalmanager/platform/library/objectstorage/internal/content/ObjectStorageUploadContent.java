package br.com.portalmanager.platform.library.objectstorage.internal.content;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.objectstorage.internal.message.ObjectStorageMessageKeys;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/** Computes a SHA-256 checksum while the provider consumes the upload stream. */
public final class ObjectStorageUploadContent {
    private final ChecksumInputStream stream;

    private ObjectStorageUploadContent(InputStream content) {
        stream = new ChecksumInputStream(content, newDigest());
    }

    public static ObjectStorageUploadContent from(InputStream content) {
        return new ObjectStorageUploadContent(content);
    }

    public InputStream stream() {
        return stream;
    }

    public String sha256() {
        return stream.sha256();
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new ApiException(ObjectStorageMessageKeys.OPERATION_FAILED,
                    Map.of("0", "initialize SHA-256 checksum"), exception);
        }
    }

    private static final class ChecksumInputStream extends DigestInputStream {
        private MessageDigest markedDigest;

        private ChecksumInputStream(InputStream content, MessageDigest digest) {
            super(content, digest);
        }

        /** Provider SDK cleanup is shielded because the caller owns and closes the original stream. */
        @Override
        public void close() {
            // Do not close the stream supplied by the caller.
        }

        @Override
        public synchronized void mark(int readLimit) {
            super.mark(readLimit);
            markedDigest = copy(digest);
        }

        @Override
        public synchronized void reset() throws IOException {
            super.reset();
            if (markedDigest != null) digest = copy(markedDigest);
        }

        private String sha256() {
            return Base64.getEncoder().encodeToString(digest.digest());
        }

        private MessageDigest copy(MessageDigest source) {
            try {
                return (MessageDigest) source.clone();
            } catch (CloneNotSupportedException exception) {
                throw new ApiException(ObjectStorageMessageKeys.OPERATION_FAILED,
                        Map.of("0", "preserve SHA-256 checksum while retrying upload"), exception);
            }
        }
    }
}
