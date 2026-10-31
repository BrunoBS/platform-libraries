package br.com.portalmanager.platform.library.objectstorage.contract;

/** Operation granted by a temporary object-storage URL. */
public enum SignedUrlOperation {
    DOWNLOAD("GET"),
    UPLOAD("PUT");

    private final String httpMethod;

    SignedUrlOperation(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String httpMethod() {
        return httpMethod;
    }
}
