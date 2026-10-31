package br.com.portalmanager.platform.library.objectstorage.contract;

/** Provider-neutral HTTP headers stored with an object. */
public record ObjectHeaders(
        String contentType,
        String cacheControl,
        String contentDisposition,
        String contentEncoding,
        String contentLanguage) {

    public static ObjectHeaders empty() {
        return new ObjectHeaders(null, null, null, null, null);
    }
}
