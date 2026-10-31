package br.com.portalmanager.platform.library.objectstorage.contract;

import java.util.LinkedHashMap;
import java.util.Map;

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

    public Map<String, String> asHttpHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        add(headers, "Content-Type", contentType);
        add(headers, "Cache-Control", cacheControl);
        add(headers, "Content-Disposition", contentDisposition);
        add(headers, "Content-Encoding", contentEncoding);
        add(headers, "Content-Language", contentLanguage);
        return Map.copyOf(headers);
    }

    private static void add(Map<String, String> headers, String name, String value) {
        if (value != null && !value.isBlank()) headers.put(name, value);
    }
}
