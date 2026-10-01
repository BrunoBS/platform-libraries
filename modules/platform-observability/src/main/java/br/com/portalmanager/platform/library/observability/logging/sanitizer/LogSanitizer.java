package br.com.portalmanager.platform.library.observability.logging.sanitizer;

@FunctionalInterface
public interface LogSanitizer {

    String sanitize(String value);
}
