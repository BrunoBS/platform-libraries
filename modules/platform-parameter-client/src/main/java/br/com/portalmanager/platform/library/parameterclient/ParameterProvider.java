package br.com.portalmanager.platform.library.parameterclient;

import tools.jackson.core.type.TypeReference;

/** Reads the value of a published parameter by its complete logical key, without fallback lookup. */
public interface ParameterProvider {
    /** Infer only scalar wrapper/String/enum types; null, POJOs and collections need an explicit type. */
    <T> T get(String key, T defaultValue);

    /** A null default is allowed. Only an absent Redis key returns the supplied default. */
    <T> T get(String key, Class<T> type, T defaultValue);

    /** Jackson 3 type token for concrete generic values. */
    <T> T get(String key, TypeReference<T> type, T defaultValue);
}
